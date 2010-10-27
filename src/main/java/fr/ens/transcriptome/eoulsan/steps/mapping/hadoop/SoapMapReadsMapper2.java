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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.AlignResult;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.bio.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.util.AbstractExternalCommandMapRedPipeThread;
import fr.ens.transcriptome.eoulsan.util.ExecLock;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class SoapMapReadsMapper2 extends Mapper<LongWritable, Text, Text, Text> {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  public static final String COUNTER_GROUP = "Map reads with SOAP";
  private static final Charset CHARSET = Charset.forName("ISO-8859-1");
  private static final boolean USE_LOCK = true;

  private Writer writer;
  private final ReadSequence readSequence = new ReadSequence();
  private static final ExecLock lock = new ExecLock("soap");
  private final String counterGroup = getCounterGroup();
  private SoapPipeThread cmdPipe;
  private Thread thread;

  private static class SoapPipeThread extends
      AbstractExternalCommandMapRedPipeThread {

    private Context context;
    private Writer writer;
    private final Text outKey = new Text();
    private final Text outValue = new Text();
    private final AlignResult aln = new AlignResult();
    private final String counterGroup;
    private String lastSequenceId;

    @Override
    protected void processOutput(String line) throws IOException,
        InterruptedException {

      if (line == null)
        return;

      if (line.indexOf('\t') != -1) {

        try {
          aln.parseResultLine(line);
        } catch (BadBioEntryException e) {

          this.context.getCounter(COUNTER_GROUP, "invalid soap output entries")
              .increment(1);
          logger.info("Invalid soap output entry: "
              + e.getMessage() + " line='" + e.getEntry() + "'");
          return;
        }

        this.context.getCounter(this.counterGroup, "soap alignments")
            .increment(1);

        final String currentSequenceId = aln.getSequenceId();

        if (aln.getNumberOfHits() == 1) {
          outKey.set(aln.getSequenceId());
          outValue.set(StringUtils.subStringAfterFirstTab(line));
          context.write(outKey, outValue);
          this.context.getCounter(this.counterGroup,
              Common.SOAP_ALIGNEMENT_WITH_ONLY_ONE_HIT_COUNTER).increment(1);
        } else if (currentSequenceId != null
            && (!currentSequenceId.equals(lastSequenceId)))
          this.context.getCounter(this.counterGroup,
              "soap alignment with more one hit").increment(1);

        this.lastSequenceId = currentSequenceId;

      } else {

        writer.write(line);
        writer.write("\n");
        if (line.startsWith(">"))
          this.context.getCounter(this.counterGroup, "soap unmap reads")
              .increment(1);
      }

    }

    @Override
    protected void close() throws IOException {

      this.writer.close();
    }

    //
    // Constructor
    //

    public SoapPipeThread(final Configuration conf, final String counterGroup,
        final Context context, final String cmd, final Path unmapDirPath)
        throws IOException {

      super(cmd, CHARSET);
      this.context = context;
      this.counterGroup = counterGroup;

      final FileSystem fs = FileSystem.get(unmapDirPath.toUri(), conf);
      final Path unmapPath =
          new Path(unmapDirPath, UUID.randomUUID().toString()
              + Common.FASTA_EXTENSION);

      this.writer = new OutputStreamWriter(fs.create(unmapPath), CHARSET);
    }

  }

  protected String getCounterGroup() {

    return COUNTER_GROUP;
  }

  @Override
  protected void setup(final Context context) throws IOException,
      InterruptedException {

    // Get configuration
    final Configuration conf = context.getConfiguration();

    // Get SOAP arguments
    final String soapArgs =
        conf.get(Globals.PARAMETER_PREFIX + ".soap.args", ""
            + Common.SOAP_ARGS_DEFAULT);

    // Get SOAP index zip file path
    final String soapIndexZipPath =
        conf.get(Globals.PARAMETER_PREFIX + ".soap.indexzipfilepath");

    if (soapIndexZipPath == null)
      throw new IOException("The SOAP index zip file path is not set");

    final String unmapChunkFilesDir =
        conf.get(Globals.PARAMETER_PREFIX + ".soap.unmap.chunk.prefix.dir");

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
    final int nbSoapThreads =
        Integer.parseInt(conf.get(
            Globals.PARAMETER_PREFIX + ".soap.nb.threads", ""
                + Runtime.getRuntime().availableProcessors()));

    final Path unmapFilesDirPath = new Path(unmapChunkFilesDir);
    PathUtils.mkdirs(unmapFilesDirPath, conf);

    // Download genome reference

    lock.lock();
    final File soapIndexZipDir = installSoapIndex(new Path(soapIndexZipPath), conf);
    lock.unlock();

    if (soapIndexZipPath == null) {
      context.getCounter(this.counterGroup, "ERROR CAN'T INSTALL SOAP INDEX")
          .increment(1);
      return;
    }

    // Lock soap for this process
    if (USE_LOCK)
      lock.lock();

    final String soapCommand =
        SOAPWrapper.mapPipe(soapIndexZipDir, soapArgs, nbSoapThreads);
    // final String soapCommand = "/bin/cat";

    this.cmdPipe =
        new SoapPipeThread(conf, this.counterGroup, context, soapCommand,
            new Path(unmapChunkFilesDir));

    this.writer = new OutputStreamWriter(this.cmdPipe.getOutputStream());
    this.thread = new Thread(this.cmdPipe);

    logger.info("Start command thread.");
    this.thread.start();

    super.setup(context);
  }

  private String getSoapIndexLocalName(final Path soapIndexPath,
      final Configuration conf) throws IOException {

    final FileSystem fs = soapIndexPath.getFileSystem(conf);
    final FileStatus fStatus = fs.getFileStatus(soapIndexPath);

    return "soap-index-"
        + fStatus.getLen() + "-" + fStatus.getModificationTime();
  }

  private File installSoapIndex(final Path soapIndexPath,
      final Configuration conf) {

    try {

      final String dirname = getSoapIndexLocalName(soapIndexPath, conf);
      final File dir = new File("/tmp", dirname);

      if (dir.exists())
        return dir;

      if (!dir.mkdirs())
        return null;

      final FileSystem fs = FileSystem.get(soapIndexPath.toUri(), conf);
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
  protected void map(final LongWritable key, final Text value,
      final Context context) throws IOException, InterruptedException {

    // Parse read entry
    this.readSequence.parse(value.toString());

    // Set a sequence name if does not exist
    if ("".equals(this.readSequence.getName()))
      this.readSequence.setName("s" + key);

    this.writer.write(this.readSequence.toFastQ());
    // this.writer.write(this.readSequence.toFasta());
    context.getCounter(counterGroup, Common.SOAP_INPUT_READS_COUNTER)
        .increment(1);

  }

  @Override
  protected void cleanup(final Context context) throws IOException,
      InterruptedException {

    // Close the data file
    this.writer.close();

    // Wait the end of the process
    final int exitValue = this.cmdPipe.waitFor();
    if (exitValue != 0)
      throw new IOException("Invalid exit value of the process: " + exitValue);

    // Wait the end of the thread
    while (this.thread.isAlive())
      Thread.yield();

    // Unlock soap for other process
    if (USE_LOCK)
      lock.unlock();
  }

}
