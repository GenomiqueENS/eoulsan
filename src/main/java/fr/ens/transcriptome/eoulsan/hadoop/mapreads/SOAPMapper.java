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

package fr.ens.transcriptome.eoulsan.hadoop.mapreads;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import fr.ens.transcriptome.eoulsan.AlignResult;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.ReadSequence;
import fr.ens.transcriptome.eoulsan.core.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.UnSynchronizedBufferedWriter;

public class SOAPMapper extends Mapper<LongWritable, Text, Text, Text> {

  private static final String SOAP_ARGS_DEFAULT = "-r 1 -l 28";

  private String soapArgs;
  private String soapIndexZipPath;
  private Path unmapFilesDirPath;
  private String unmapChunkPrefix;
  private File soapIndexZipDir;
  private Writer writer;
  private File dataFile;
  private int nbSoapThreads = 1;
  private final ReadSequence readSequence = new ReadSequence();

  @Override
  protected void setup(final Context context) throws IOException,
      InterruptedException {

    // Get configuration
    final Configuration conf = context.getConfiguration();

    // Get SOAP arguments
    this.soapArgs =
        conf.get(Globals.PARAMETER_PREFIX + ".soap.args", ""
            + SOAP_ARGS_DEFAULT);

    // Get SOAP index zip file path
    this.soapIndexZipPath =
        conf.get(Globals.PARAMETER_PREFIX + ".soap.indexzipfilepath");

    if (this.soapIndexZipPath == null)
      throw new IOException("The SOAP index zip file path is not set");

    final String unmapChunkFilesDir =
        conf.get(Globals.PARAMETER_PREFIX + ".soap.unmap.chunk.temp.dir");

    // Get unmap Files directory
    if (unmapChunkFilesDir == null)
      throw new IOException(
          "The temporary directory path for unmap file is not set");

    final String unmapChunkPrefix =
        conf.get(Globals.PARAMETER_PREFIX + ".soap.unmap.chunk.prefix");

    // Get unmap Files prefix directory
    if (unmapChunkPrefix == null)
      throw new IOException("The prefix of unmap chunk  is not set");

    // Get the number of threads to use
    this.nbSoapThreads =
        Integer.parseInt(conf.get(
            Globals.PARAMETER_PREFIX + ".soap.nb.threads", ""
                + Runtime.getRuntime().availableProcessors()));

    this.unmapFilesDirPath = new Path(unmapChunkFilesDir);

    PathUtils.mkdirs(this.unmapFilesDirPath, conf);

    this.dataFile =
        FileUtils.createTempFile(Globals.APP_NAME_LOWER_CASE + "-soap-data-",
            ".fq");
    this.writer =
        new UnSynchronizedBufferedWriter(new FileWriter(this.dataFile));

    super.setup(context);

  }

  @Override
  protected void map(final LongWritable key, final Text value,
      final Context context) throws IOException, InterruptedException {

    // this.readSequence.parseKeyValue(key.toString(), value.toString());
    this.readSequence.parse(value.toString());
    this.writer.write(this.readSequence.toFastQ());
  }

  @Override
  protected void cleanup(final Context context) throws IOException,
      InterruptedException {

    // Close the data file
    this.writer.close();

    // Download genome reference
    if (this.soapIndexZipDir == null) {

      final Configuration conf = context.getConfiguration();
      final File outputDirectory = FileUtils.createTempDir("soap-index-");

      PathUtils.unZipPathToLocalFile(new Path(this.soapIndexZipPath),
          outputDirectory, conf);

      this.soapIndexZipDir = outputDirectory;
    }

    // Create the temporary output files

    final File outputFile =
        FileUtils.createTempFile(Globals.TEMP_PREFIX + "soap-output-", ".aln");

    final File unmapFile =
        FileUtils.createTempFile(this.unmapChunkPrefix, ".fasta");

    SOAPWrapper.map(this.dataFile, this.soapIndexZipDir, outputFile, unmapFile,
        this.soapArgs, this.nbSoapThreads);

    parseSOAPResults(outputFile, unmapFile, context);

    // Remove temporary files
    outputFile.delete();
    // unmapFile.delete();
    this.dataFile.delete();
  }

  private void parseSOAPResults(final File resultFile, final File unmapFile,
      final Context context) throws IOException, InterruptedException {

    final Configuration conf = context.getConfiguration();

    String line;

    final Text outKey = new Text();
    final Text outValue = new Text();

    // Parse SOAP main result file
    final BufferedReader readerResults =
        FileUtils.createBufferedReader(resultFile);
    final AlignResult aln = new AlignResult();
    int moreOneLocus = 0;

    while ((line = readerResults.readLine()) != null) {

      aln.parseResultLine(line);

      if (aln.getNumberOfHits() == 1) {
        outKey.set(aln.getSequenceId());
        outValue.set(StringUtils.subStringAfterFirstTab(line));
        context.write(outKey, outValue);
      } else
        moreOneLocus++;

    }

    readerResults.close();

    // Return reads count with more than one hit
    outKey.set("__COUNT_MORE_ONE_LOCUS__");
    outValue.set("" + moreOneLocus);
    context.write(outKey, outValue);

    // Parse unmap
    final BufferedReader readerUnmap =
        FileUtils.createBufferedReader(unmapFile);
    int countUnMap = 0;

    while ((line = readerUnmap.readLine()) != null)
      if (line.trim().startsWith(">"))
        countUnMap++;

    readerUnmap.close();

    // Return unmaps reads
    outKey.set("__COUNT_UNMAP_READS__");
    outValue.set("" + countUnMap);
    context.write(outKey, outValue);

    // Move unmap file to HDFS
    final Path unmapPath =
        new Path(this.unmapFilesDirPath, unmapFile.getName());
    PathUtils.copyLocalFileToPath(unmapFile, unmapPath, true, conf);

    // Set the unmap file
    outKey.set("__UNMAP_FILE__");
    outValue.set(unmapPath.toString());
    context.write(outKey, outValue);

  }

}
