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
import java.io.Writer;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.parsers.AlignResult;
import fr.ens.transcriptome.eoulsan.parsers.ReadSequence;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.UnSynchronizedBufferedWriter;

@SuppressWarnings("deprecation")
public class SoapMapReadsMapper implements
    Mapper<LongWritable, Text, Text, Text> {

  public static final String COUNTER_GROUP = "Map reads with SOAP";
  
  private static final String SOAP_ARGS_DEFAULT = "-r 1 -l 28";

  
  
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

  protected String getCounterGroup() {

    return COUNTER_GROUP;
  }

  @Override
  public void map(final LongWritable key, final Text value,
      final OutputCollector<Text, Text> collector, final Reporter reporter)
      throws IOException {

    if (configureException != null)
      throw configureException;

    reporter.incrCounter(this.counterGroup, "input reads", 1);

    this.readSequence.parse(value.toString());
    writeRead(this.readSequence, collector, reporter);
  }

  protected final void writeRead(final ReadSequence readSequence,
      final OutputCollector<Text, Text> collector, final Reporter reporter)
      throws IOException {

    this.writer.write(readSequence.toFastQ());

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
              + SOAP_ARGS_DEFAULT);

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

  @Override
  public final void close() throws IOException {

    // Close the data file
    this.writer.close();

    if (this.collector == null || this.reporter == null)
      return;

    // Download genome reference
    if (this.soapIndexZipDir == null) {

      // final Configuration conf = context.getConfiguration();
      final File outputDirectory = FileUtils.createTempDir("soap-index-");

      PathUtils.unZipPathToLocalFile(new Path(this.soapIndexZipPath),
          outputDirectory, this.conf);

      this.soapIndexZipDir = outputDirectory;
    }

    // Create the temporary output files

    final File outputFile =
        FileUtils.createTempFile(Globals.TEMP_PREFIX + "soap-output-", ".aln");

    final File unmapFile =
        FileUtils.createTempFile(this.unmapChunkPrefix, ".fasta");

    SOAPWrapper.map(this.dataFile, this.soapIndexZipDir, outputFile, unmapFile,
        this.soapArgs, this.nbSoapThreads);

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
    int parseCount = 0;
    int moreOneLocus = 0;

    while ((line = readerResults.readLine()) != null) {

      final String trimmedLine = line.trim();
      if ("".equals(trimmedLine))
        continue;

      aln.parseResultLine(trimmedLine);
      parseCount++;

      if (aln.getNumberOfHits() == 1) {
        outKey.set(aln.getSequenceId());
        outValue.set(StringUtils.subStringAfterFirstTab(line));
        collector.collect(outKey, outValue);
      } else
        moreOneLocus++;

    }

    readerResults.close();

    // Return reads count with more than one hit
    // outKey.set("__COUNT_MORE_ONE_LOCUS__");
    // outValue.set("" + moreOneLocus);
    // context.write(outKey, outValue);
    reporter.incrCounter(this.counterGroup, "more one locus", moreOneLocus);
    reporter.incrCounter(this.counterGroup, "results parsed", parseCount);

    // Parse unmap
    final BufferedReader readerUnmap =
        FileUtils.createBufferedReader(unmapFile);
    int countUnMap = 0;

    while ((line = readerUnmap.readLine()) != null)
      if (line.trim().startsWith(">"))
        countUnMap++;

    readerUnmap.close();

    // Return unmaps reads
    // outKey.set("__COUNT_UNMAP_READS__");
    // outValue.set("" + countUnMap);
    // context.write(outKey, outValue);
    reporter.incrCounter(this.counterGroup, "unmap reads", countUnMap);

    // Move unmap file to HDFS
    final Path unmapPath =
        new Path(this.unmapFilesDirPath, unmapFile.getName());
    PathUtils.copyLocalFileToPath(unmapFile, unmapPath, true, this.conf);

    // Set the unmap file
    // outKey.set("__UNMAP_FILE__");
    // outValue.set(unmapPath.toString());
    // collector.collect(outKey, outValue);
  }

}
