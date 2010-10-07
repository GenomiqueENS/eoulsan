/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.steps.mapping.hadoop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.logging.Logger;

import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.AlignResult;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.core.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.util.ExecLock;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.UnSynchronizedBufferedWriter;

/**
 * This class is mapper to map reads using SOAP.
 * @author Laurent Jourdren
 */
@SuppressWarnings("deprecation")
public class SoapMapReadsMapper implements
    Mapper<LongWritable, Text, Text, Text> {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  public static final String COUNTER_GROUP = "Map reads with SOAP";

  private static final String END_DECOMPRESSION_FILE = "unzip.end";

  private final String counterGroup = getCounterGroup();

  private IOException configureException;

  private String soapArgs;
  private String soapIndexZipPath;
  private Path unmapFilesDirPath;
  private String unmapChunkPrefix;
  private File soapIndexZipDir;
  private Writer writer;
  private File dataFile;
  private int nbSoapThreads = 1;
  private final ReadSequence readSequence = new ReadSequence();
  private OutputCollector<Text, Text> collector;
  private Reporter reporter;
  private JobConf conf;
  private int fastqEntries;
  private static final ExecLock lock = new ExecLock("soap");

  protected String getCounterGroup() {

    return COUNTER_GROUP;
  }

  @Override
  public void map(final LongWritable key, final Text value,
      final OutputCollector<Text, Text> collector, final Reporter reporter)
      throws IOException {

    if (configureException != null)
      throw configureException;

    reporter.incrCounter(this.counterGroup, "map input reads", 1);

    this.readSequence.parse(value.toString());
    writeRead(this.readSequence, collector, reporter);
  }

  protected final void writeRead(final ReadSequence readSequence,
      final OutputCollector<Text, Text> collector, final Reporter reporter)
      throws IOException {

    this.writer.write(readSequence.toFastQ());
    reporter.incrCounter(this.counterGroup, Common.SOAP_INPUT_READS_COUNTER, 1);
    this.fastqEntries++;

    if (this.reporter == null && reporter != null)
      this.reporter = reporter;

    if (this.collector == null && collector != null)
      this.collector = collector;
  }

  @Override
  public void configure(final JobConf conf) {

    logger.info("Start of configure()");

    this.conf = conf;

    try {
      // Get SOAP arguments
      this.soapArgs =
          conf.get(Globals.PARAMETER_PREFIX + ".soap.args", ""
              + Common.SOAP_ARGS_DEFAULT);

      // Get SOAP index zip file path
      this.soapIndexZipPath =
          conf.get(Globals.PARAMETER_PREFIX + ".soap.indexzipfilepath");

      final String unmapChunkFilesDir =
          conf.get(Globals.PARAMETER_PREFIX + ".soap.unmap.chunk.prefix.dir");

      // Get unmap Files directory
      if (unmapChunkFilesDir == null)
        throw new IOException(
            "The temporary directory path for unmap file is not set");

      this.unmapChunkPrefix =
          conf.get(Globals.PARAMETER_PREFIX + ".soap.unmap.chunk.prefix");

      // Get unmap Files prefix directory
      if (unmapChunkPrefix == null)
        throw new IOException("The prefix of unmap chunk  is not set");

      // Get the number of threads to use
      this.nbSoapThreads =
          Integer.parseInt(conf.get(Globals.PARAMETER_PREFIX
              + ".soap.nb.threads", ""
              + Runtime.getRuntime().availableProcessors()));

      if (this.nbSoapThreads > Runtime.getRuntime().availableProcessors())
        this.nbSoapThreads = Runtime.getRuntime().availableProcessors();
      logger.info("Use SOAP with " + this.nbSoapThreads + " threads option");

      this.unmapFilesDirPath = new Path(unmapChunkFilesDir);

      PathUtils.mkdirs(this.unmapFilesDirPath, conf);

      this.dataFile =
          FileUtils.createTempFile(Globals.APP_NAME_LOWER_CASE + "-soap-data-",
              ".fq");
      this.writer =
          new UnSynchronizedBufferedWriter(new FileWriter(this.dataFile));

      // Download genome reference
      if (this.soapIndexZipDir == null) {

        final Path[] localCacheFiles =
            DistributedCache.getLocalCacheFiles(conf);

        if (localCacheFiles == null || localCacheFiles.length == 0)
          throw new IOException("Unable to retrieve genome index");

        if (localCacheFiles.length > 1)
          throw new IOException(
              "Retrieve more than one file in distributed cache");

        // Get the local genome index zip file
        final File soapLocalIndexZipFile =
            new File(localCacheFiles[0].toString());

        logger.info("Genome index compressed file (from distributed cache): "
            + soapLocalIndexZipFile);

        // Test if the decompression of the index is ok

        final File endUnzipFile =
            new File("/tmp/" + getSoapIndexLocalName(soapLocalIndexZipFile),
                END_DECOMPRESSION_FILE);

        if (!endUnzipFile.exists()) {

          lock.lock();

          logger.info("Start decompressing genome index");

          this.soapIndexZipDir = installSoapIndex(soapLocalIndexZipFile);

          final boolean resultCreationEndUnzipFile =
              endUnzipFile.createNewFile();

          lock.unlock();

          // Error can't unzip genome index
          if (this.soapIndexZipDir == null) {
            logger.info("soapIndexZipDir is null");
            throw new IOException("The SOAP index zip file path is not set");
          }

          logger.info("soapIndexZipDir: " + this.soapIndexZipDir);
          logger.info("soapIndexZipDir content: "
              + Arrays.toString(this.soapIndexZipDir.list()));

          if (!resultCreationEndUnzipFile)
            throw new IOException("Unable to create end decompression file");

          logger.info("End of the decompression of the genome index");

        } else {
          logger.info("The genome index has been already unzipped");
          this.soapIndexZipDir = endUnzipFile.getParentFile();
        }
      }

    } catch (IOException e) {

      logger.severe("Error: " + e.getMessage());
      this.configureException = e;
    }

    logger.info("End of configure()");
  }

  private String getSoapIndexLocalName(final File soapIndexPath)
      throws IOException {

    return "soap-index-"
        + soapIndexPath.length() + "-" + soapIndexPath.lastModified();
  }

  private File installSoapIndex(final File soapIndexFile) throws IOException {

    final String dirname = getSoapIndexLocalName(soapIndexFile);
    final File dir = new File("/tmp", dirname);

    if (dir.exists()) {
      logger.info("The genome index has been already decompressed");
      return dir;
    }

    if (!dir.mkdirs())
      return null;

    FileUtils.unzip(soapIndexFile, dir);

    return dir;
  }

  @Override
  public final void close() throws IOException {

    logger.info("Start of close() of the mapper.");
    // Close the data file
    this.writer.close();

    logger.info(this.fastqEntries
        + " fastq entries wrote in input file for SOAP.");

    if (this.collector == null || this.reporter == null)
      return;

    if (this.soapIndexZipPath == null) {

      throw new IOException("SOAP index was not installed");

      // this.reporter.incrCounter(this.counterGroup,
      // "ERROR CAN'T INSTALL SOAP INDEX", 1);
      // return;
    }

    final File outputFile =
        FileUtils.createTempFile(Globals.TEMP_PREFIX + "soap-output-", ".aln");

    final File unmapFile =
        FileUtils.createTempFile(this.unmapChunkPrefix, ".fasta");

    this.reporter.setStatus("Wait free JVM for running SOAP");
    final long waitStartTime = System.currentTimeMillis();

    ProcessUtils.waitRandom(5000);
    lock.lock();
    ProcessUtils.waitUntilExecutableRunning("soap");

    logger.info("Wait "
        + StringUtils.toTimeHumanReadable(System.currentTimeMillis()
            - waitStartTime) + " before running SOAP");

    this.reporter.setStatus("Run SOAP");

    SOAPWrapper.map(this.dataFile, this.soapIndexZipDir, outputFile, unmapFile,
        this.soapArgs, this.nbSoapThreads);
    lock.unlock();

    this.reporter.setStatus("Parse SOAP results");
    parseSOAPResults(outputFile, unmapFile, this.collector, this.reporter);

    // Remove temporary files
    outputFile.delete();
    // unmapFile.delete();
    this.dataFile.delete();

    logger.info("End of close() of the mapper.");
  }

  private final void parseSOAPResults(final File resultFile,
      final File unmapFile, final OutputCollector<Text, Text> collector,
      final Reporter reporter) throws IOException {

    String line;

    final Text outKey = new Text();
    final Text outValue = new Text();

    // Parse SOAP main result file
    final BufferedReader readerResults =
        FileUtils.createBufferedReader(resultFile);
    final AlignResult aln = new AlignResult();
    // int parseCount = 0;
    // int moreOneLocus = 0;

    String lastSequenceId = null;

    int entriesParsed = 0;

    while ((line = readerResults.readLine()) != null) {

      final String trimmedLine = line.trim();
      if ("".equals(trimmedLine))
        continue;

      entriesParsed++;

      aln.parseResultLine(trimmedLine);
      reporter.incrCounter(this.counterGroup, "soap alignments", 1);

      final String currentSequenceId = aln.getSequenceId();

      if (aln.getNumberOfHits() == 1) {
        outKey.set(aln.getSequenceId());
        outValue.set(StringUtils.subStringAfterFirstTab(line));
        collector.collect(outKey, outValue);
        reporter.incrCounter(this.counterGroup,
            Common.SOAP_ALIGNEMENT_WITH_ONLY_ONE_HIT_COUNTER, 1);
      } else if (currentSequenceId != null
          && (!currentSequenceId.equals(lastSequenceId)))
        reporter.incrCounter(this.counterGroup,
            "soap alignment with more one hit", 1);

      lastSequenceId = currentSequenceId;
    }

    readerResults.close();

    logger.info(entriesParsed + " entries parsed in SOAP output file");

    // Parse unmap
    final BufferedReader readerUnmap =
        FileUtils.createBufferedReader(unmapFile);
    int countUnMap = 0;

    while ((line = readerUnmap.readLine()) != null)
      if (line.trim().startsWith(">"))
        countUnMap++;

    readerUnmap.close();

    // Return unmaps reads
    reporter.incrCounter(this.counterGroup, "soap unmap reads", countUnMap);
    logger.info(countUnMap + " entries parsed in SOAP Unmap output file");

    // Move unmap file to HDFS
    final Path unmapPath =
        new Path(this.unmapFilesDirPath, unmapFile.getName());
    PathUtils.copyLocalFileToPath(unmapFile, unmapPath, true, this.conf);
  }

}
