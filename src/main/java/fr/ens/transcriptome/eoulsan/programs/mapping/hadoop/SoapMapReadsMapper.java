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

package fr.ens.transcriptome.eoulsan.programs.mapping.hadoop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.AlignResult;
import fr.ens.transcriptome.eoulsan.core.ReadSequence;
import fr.ens.transcriptome.eoulsan.core.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.util.ExecLock;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.UnSynchronizedBufferedWriter;

@SuppressWarnings("deprecation")
public class SoapMapReadsMapper implements
    Mapper<LongWritable, Text, Text, Text> {

  public static final String COUNTER_GROUP = "Map reads with SOAP";

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
    reporter.incrCounter(this.counterGroup, "soap input reads", 1);

    if (this.reporter == null && reporter != null)
      this.reporter = reporter;

    if (this.collector == null && collector != null)
      this.collector = collector;
  }

  @Override
  public void configure(final JobConf conf) {

    this.conf = conf;

    try {
      // Get SOAP arguments
      this.soapArgs =
          conf.get(Globals.PARAMETER_PREFIX + ".soap.args", ""
              + Common.SOAP_ARGS_DEFAULT);

      // Get SOAP index zip file path
      this.soapIndexZipPath =
          conf.get(Globals.PARAMETER_PREFIX + ".soap.indexzipfilepath");

      if (this.soapIndexZipPath == null) {

        throw new IOException("The SOAP index zip file path is not set");
      }

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

      this.unmapFilesDirPath = new Path(unmapChunkFilesDir);

      PathUtils.mkdirs(this.unmapFilesDirPath, conf);

      this.dataFile =
          FileUtils.createTempFile(Globals.APP_NAME_LOWER_CASE + "-soap-data-",
              ".fq");
      this.writer =
          new UnSynchronizedBufferedWriter(new FileWriter(this.dataFile));

    } catch (IOException e) {

      this.configureException = e;
    }

  }

  private String getSoapIndexLocalName(final Path soapIndexPath)
      throws IOException {

    FileSystem fs = PathUtils.getFileSystem(soapIndexPath, this.conf);
    FileStatus fStatus = fs.getFileStatus(soapIndexPath);

    return "soap-index-"
        + fStatus.getLen() + "-" + fStatus.getModificationTime();
  }

  private File installSoapIndex(final Path soapIndexPath) {

    try {

      final String dirname = getSoapIndexLocalName(soapIndexPath);
      final File dir = new File("/tmp", dirname);

      if (dir.exists())
        return dir;

      if (!dir.mkdirs())
        return null;

      final FileSystem fs = FileSystem.get(soapIndexPath.toUri(), this.conf);
      final InputStream is = fs.open(soapIndexPath);
      FileUtils.unzip(is, dir);

      return dir;

    } catch (IOException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();

      return null;
    }

  }

  @Override
  public final void close() throws IOException {

    // Close the data file
    this.writer.close();

    if (this.collector == null || this.reporter == null)
      return;

    // Download genome reference
    if (this.soapIndexZipDir == null) {
      lock.lock();
      this.soapIndexZipDir = installSoapIndex(new Path(this.soapIndexZipPath));
      lock.unlock();
    }

    if (this.soapIndexZipPath == null) {
      this.reporter.incrCounter(this.counterGroup,
          "ERROR CAN'T INSTALL SOAP INDEX", 1);
      return;
    }

    final File outputFile =
        FileUtils.createTempFile(Globals.TEMP_PREFIX + "soap-output-", ".aln");

    final File unmapFile =
        FileUtils.createTempFile(this.unmapChunkPrefix, ".fasta");

    ProcessUtils.waitRandom(5000);
    lock.lock();
    ProcessUtils.waitUntilExecutableRunning("soap");
    SOAPWrapper.map(this.dataFile, this.soapIndexZipDir, outputFile, unmapFile,
        this.soapArgs, this.nbSoapThreads);
    lock.unlock();

    parseSOAPResults(outputFile, unmapFile, this.collector, this.reporter);

    // Remove temporary files
    outputFile.delete();
    // unmapFile.delete();
    this.dataFile.delete();

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
    
    while ((line = readerResults.readLine()) != null) {

      final String trimmedLine = line.trim();
      if ("".equals(trimmedLine))
        continue;

      aln.parseResultLine(trimmedLine);
      reporter.incrCounter(this.counterGroup, "soap alignments", 1);
      
      final String currentSequenceId = aln.getSequenceId();

      if (aln.getNumberOfHits() == 1) {
        outKey.set(aln.getSequenceId());
        outValue.set(StringUtils.subStringAfterFirstTab(line));
        collector.collect(outKey, outValue);
        reporter.incrCounter(this.counterGroup,
            "soap alignment with only one hit", 1);
      } else if (currentSequenceId != null
          && (!currentSequenceId.equals(lastSequenceId)))
        reporter.incrCounter(this.counterGroup,
            "soap alignment with more one hit", 1);

      lastSequenceId = currentSequenceId;
    }

    readerResults.close();

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

    // Move unmap file to HDFS
    final Path unmapPath =
        new Path(this.unmapFilesDirPath, unmapFile.getName());
    PathUtils.copyLocalFileToPath(unmapFile, unmapPath, true, this.conf);
  }

}
