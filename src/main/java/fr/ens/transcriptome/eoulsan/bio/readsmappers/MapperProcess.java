/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.logging.Logger;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

public abstract class MapperProcess {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final String mapperName;
  private final boolean inputFileMode;
  private final boolean inputStdinMode;
  private final boolean pairedEnd;

  private Process process;
  private final OutputStream stdin;
  private InputStream stdout;

  private final File tmpInFile1;
  private final File tmpInFile2;
  private final File tmpOutFile1;
  private final File tmpOutFile2;

  private ReporterIncrementer incrementer;
  private String counterGroup;

  public static final class ProcessThreadStdOut extends Thread {

    final OutputStream os;
    final InputStream is;
    private Exception exception;

    @Override
    public void run() {
      try {

        FileUtils.copy(is, os);

      } catch (IOException e) {
        catchException(e);
      }
    }

    private void catchException(final Exception e) {
      this.exception = e;
    }

    public boolean isException() {

      return this.exception != null;
    }

    public Exception getException() {

      return this.exception;
    }

    public ProcessThreadStdOut(final Process p, final OutputStream os) {

      this.is = p.getInputStream();
      this.os = os;
    }
  }

  private final class OutputStreamWrapper extends OutputStream {

    private final OutputStream os;

    @Override
    public void close() throws IOException {

      this.os.close();
      try {
        System.out.println("End of writing tmp");
        startProcess();
      } catch (InterruptedException e) {
        throw new IOException(e.getMessage());
      }
    }

    @Override
    public void flush() throws IOException {

      this.os.flush();
    }

    @Override
    public void write(byte[] arg0, int arg1, int arg2) throws IOException {

      this.os.write(arg0, arg1, arg2);
    }

    @Override
    public void write(byte[] b) throws IOException {

      this.os.write(b);
    }

    @Override
    public void write(int arg0) throws IOException {
      this.os.write(arg0);
    }

    private OutputStreamWrapper(final OutputStream os) {
      this.os = os;
    }

  }

  private final class InputStreamWrapper extends InputStream {

    private final InputStream is;

    @Override
    public int available() throws IOException {

      return this.is.available();
    }

    @Override
    public void close() throws IOException {

      this.is.close();
      try {
        final int exitValue = process.waitFor();

        if (exitValue != 0)
          throw new IOException("Bad error result for "
              + mapperName + " execution: " + exitValue);

      } catch (InterruptedException e) {
        throw new IOException(e.getMessage());
      }
    }

    @Override
    public synchronized void mark(int readlimit) {

      this.is.mark(readlimit);
    }

    @Override
    public boolean markSupported() {

      return this.is.markSupported();
    }

    @Override
    public int read() throws IOException {

      return this.is.read();
    }

    @Override
    public int read(byte[] arg0, int arg1, int arg2) throws IOException {

      return this.is.read(arg0, arg1, arg2);
    }

    @Override
    public int read(byte[] b) throws IOException {

      return this.is.read(b);
    }

    @Override
    public synchronized void reset() throws IOException {

      this.is.reset();
    }

    @Override
    public long skip(long arg0) throws IOException {

      return this.is.skip(arg0);
    }

    private InputStreamWrapper(final InputStream is) {
      this.is = is;
    }
  }

  //
  // Protected methods
  //

  protected abstract List<List<String>> createCommandLines();

  protected File executionDirectory() {

    return null;
  }

  protected InputStream createCustomInputStream(final InputStream stdout)
      throws IOException {

    return stdout;
  }

  protected void clean() {
  }

  //
  // Getters
  //

  public boolean isInputFileMode() {

    return this.inputFileMode;
  }

  public boolean isStdinMode() {

    return this.inputStdinMode;
  }

  public boolean isPairedEnd() {

    return this.pairedEnd;
  }

  protected File getTmpInputFile1() {
    return this.tmpInFile1;
  }

  protected File getTmpInputFile2() {
    return this.tmpInFile2;
  }

  protected File getTmpOutputFile1() {
    return this.tmpOutFile1;
  }

  protected File getTmpOutputFile2() {
    return this.tmpOutFile2;
  }

  protected void inputReadsIncr() {

    if (this.incrementer != null)
      this.incrementer.incrCounter(this.counterGroup, "mapper input reads", 1);
  }

  //
  // Setters
  //

  public void setIncrementer(final ReporterIncrementer incrementer,
      final String counterGroup) {

    if (counterGroup == null)
      throw new NullPointerException("The counterGroup is null");

    this.incrementer = incrementer;
    this.counterGroup = counterGroup;
  }

  //
  // Low level streams
  //

  public OutputStream getStdin() {

    if (this.stdin == null)
      throw new IllegalStateException(
          "Cannot use getStdin when mapper input is a file");

    return this.stdin;
  }

  public InputStream getStout() {

    return this.stdout;
  }

  //
  // High level streams
  //

  public void toFile(final File outputFile) throws FileNotFoundException {

    // Start stdout thread
    final Thread tout =
        new Thread(new ProcessThreadStdOut(this.process, new FileOutputStream(
            outputFile)));
    tout.start();
  }

  private Writer writer;

  public void writeEntry(final String name, final String sequence,
      final String quality) throws IOException {

    if (this.writer == null) {
      this.writer = new OutputStreamWriter(getStdin(), Charsets.ISO_8859_1);
    }

    if (this.isPairedEnd())
      throw new IllegalStateException(
          "Cannot use this writeEntry method in paired-end mode");

    this.writer.write(ReadSequence.toFastQ(name, sequence, quality));
    inputReadsIncr();
  }

  public void writeEntry(final String name1, final String sequence1,
      final String quality1, final String name2, final String sequence2,
      final String quality2) throws IOException {

    if (this.writer == null) {
      this.writer = new OutputStreamWriter(getStdin(), Charsets.ISO_8859_1);
    }

    if (!this.isPairedEnd())
      throw new IllegalStateException(
          "Cannot use this writeEntry method in single-end mode");

    this.writer.write(ReadSequence.toFastQ(name1, sequence1, quality1));
    this.writer.write(ReadSequence.toFastQ(name2, sequence2, quality2));
    inputReadsIncr();
  }

  public void closeEntriesWriter() throws IOException {

    if (this.writer != null)
      this.writer.close();
  }

  //
  // Process management
  //

  private void startProcess() throws IOException, InterruptedException {

    final List<List<String>> cmds = createCommandLines();

    // Launch all the commands
    for (int i = 0; i < cmds.size(); i++) {
      final ProcessBuilder builder = new ProcessBuilder(cmds.get(i));

      if (executionDirectory() != null)
        builder.directory(executionDirectory());

      LOGGER.info("Process command: " + Joiner.on(' ').join(builder.command()));
      LOGGER.info("Process directory: " + builder.directory());

      // Start command
      this.process = builder.start();

      if (i < cmds.size() - 1) {
        final int exitValue = this.process.waitFor();
        if (exitValue != 0)
          throw new IOException("Bad error result for "
              + this.mapperName + " execution: " + exitValue);
      } else
        this.stdout =
            new InputStreamWrapper(
                createCustomInputStream(this.process.getInputStream()));
    }

  }

  public void waitFor() throws InterruptedException {

    this.process.waitFor();
    clean();
  }

  //
  // Constructor
  //

  protected MapperProcess(final String mapperName, final boolean useFile,
      final boolean useStdin, final boolean pairedEnd) throws IOException {

    try {
      this.mapperName = mapperName;
      this.inputFileMode = useFile;
      this.inputStdinMode = useStdin;
      this.pairedEnd = pairedEnd;

      if (useFile) {
        this.stdin = null;
        startProcess();
        this.stdout =
            new InputStreamWrapper(
                createCustomInputStream(this.process.getInputStream()));
      } else if (useStdin) {
        startProcess();
        this.stdin = this.process.getOutputStream();
        this.stdout =
            new InputStreamWrapper(
                createCustomInputStream(this.process.getInputStream()));
      } else {
        this.stdin =
            new OutputStreamWrapper(new FileOutputStream(this.tmpInFile1));
      }

      final File tmpDir = new File("/tmp");
      this.tmpInFile1 =
          FileUtils.createTempFile(tmpDir, "mapper-inputfile1-", ".fq");
      this.tmpInFile2 =
          FileUtils.createTempFile(tmpDir, "mapper-inputfile2-", ".fq");
      this.tmpOutFile1 =
          FileUtils.createTempFile(tmpDir, "mapper-outputfile1-", ".data");
      this.tmpOutFile2 =
          FileUtils.createTempFile(tmpDir, "mapper-outputfile2-", ".data");

    } catch (InterruptedException e) {
      throw new IOException(e.getMessage());
    }
  }

}
