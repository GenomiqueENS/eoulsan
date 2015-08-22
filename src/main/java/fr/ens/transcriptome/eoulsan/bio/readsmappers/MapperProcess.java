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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.MapperExecutor.Result;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define an abstract class that is returned by a mapper.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class MapperProcess {

  private final String mapperName;
  private final MapperExecutor executor;
  private final boolean pairedEnd;

  private Result process;

  private InputStream stdout;

  private final File pipeFile1;
  private final File pipeFile2;

  private final Writer writer1;
  private final Writer writer2;

  private ReporterIncrementer incrementer;
  private String counterGroup;

  private List<File> filesToRemove = new ArrayList<>();

  //
  // Inner classes
  //

  /**
   * This class allow to write standard output of a MapperProcess in an
   * OutputStream.
   * @author Laurent Jourdren
   */
  public static final class ProcessThreadStdOut extends Thread {

    final OutputStream os;
    final InputStream is;
    private Exception exception;

    @Override
    public void run() {
      try {

        FileUtils.copy(this.is, this.os);

      } catch (IOException e) {
        catchException(e);
      }
    }

    /**
     * Save exception.
     * @param e Exception to save
     */
    private void catchException(final Exception e) {
      this.exception = e;
    }

    /**
     * Test is an exception has been thrown.
     * @return true if is an exception has been thrown
     */
    public boolean isException() {

      return this.exception != null;
    }

    /**
     * Get the exception.
     * @return the exception
     */
    public Exception getException() {

      return this.exception;
    }

    /**
     * Constructor.
     * @param process process
     * @param os output stream
     * @throws IOException if an error occurs while creating the input stream
     */
    public ProcessThreadStdOut(final Result process, final OutputStream os)
        throws IOException {

      if (process == null) {
        throw new NullPointerException("The Process parameter is null");
      }

      if (os == null) {
        throw new NullPointerException("The OutputStream parameter is null");
      }

      this.is = process.getInputStream();
      this.os = os;
    }
  }

  /**
   * Wrapper around an InputStream that call process.waitFor() method when the
   * stream is closed.
   * @author Laurent Jourdren.
   */
  private final class InputStreamWrapper extends InputStream {

    private final InputStream is;

    @Override
    public int available() throws IOException {

      return this.is.available();
    }

    @Override
    public void close() throws IOException {

      this.is.close();

      final int exitValue = MapperProcess.this.process.waitFor();

      getLogger().fine("End of process with " + exitValue + " exit value");

      if (exitValue != 0) {
        throw new IOException("Bad error result for "
            + MapperProcess.this.mapperName + " execution: " + exitValue);
      }
    }

    @Override
    public synchronized void mark(final int readLimit) {

      this.is.mark(readLimit);
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
    public int read(final byte[] arg0, final int arg1, final int arg2)
        throws IOException {

      return this.is.read(arg0, arg1, arg2);
    }

    @Override
    public int read(final byte[] b) throws IOException {

      return this.is.read(b);
    }

    @Override
    public synchronized void reset() throws IOException {

      this.is.reset();
    }

    @Override
    public long skip(final long arg0) throws IOException {

      return this.is.skip(arg0);
    }

    private InputStreamWrapper(final InputStream is) {
      this.is = is;
    }
  }

  //
  // Protected methods
  //

  /**
   * Create the command lines to be executed.
   * @return a List of List of String
   */
  protected abstract List<List<String>> createCommandLines();

  /**
   * Get the execution directory for the mapper.
   * @return a File object or null if the execution directory does not matter
   */
  protected File executionDirectory() {

    return null;
  }

  /**
   * Create a custom InputStream that allow to convert result of mapper in SAM
   * format.
   * @param stdout standard output from the mapper
   * @return a InputStream that contains a SAM File data
   * @throws IOException if an error occurs when creating the InputStream
   */
  protected InputStream createCustomInputStream(final InputStream stdout)
      throws IOException {

    return stdout;
  }

  //
  // Getters
  //

  /**
   * Test if data to process is paired-end data.
   * @return true if data to process is paired-end data
   */
  public boolean isPairedEnd() {

    return this.pairedEnd;
  }

  /**
   * Get File for temporary file for first end FASTQ file.
   * @return a File object
   */
  protected File getNamedPipeFile1() {
    return this.pipeFile1;
  }

  /**
   * Get File for temporary file for second end FASTQ file.
   * @return a File object
   */
  protected File getNamedPipeFile2() {
    return this.pipeFile2;
  }

  /**
   * Increments input reads written by writeEntry() methods.
   */
  protected void inputReadsIncr() {

    if (this.incrementer != null) {
      this.incrementer.incrCounter(this.counterGroup, "mapper input reads", 1);
    }
  }

  //
  // Setters
  //

  /**
   * Set the incrementer.
   * @param incrementer Incrementer to use
   * @param counterGroup the counter group to use
   */
  public void setIncrementer(final ReporterIncrementer incrementer,
      final String counterGroup) {

    if (counterGroup == null) {
      throw new NullPointerException("The counterGroup is null");
    }

    this.incrementer = incrementer;
    this.counterGroup = counterGroup;
  }

  //
  // Low level streams
  //

  /**
   * Get the standard output stream for the process
   * @return the standard output stream for the process
   */
  public InputStream getStout() {

    return this.stdout;
  }

  //
  // High level streams
  //

  /**
   * Convert the output stream from the mapper to a file using a thread.
   * @param outputFile output SAM file
   * @throws IOException if an error occurs while creating the writer thread
   */
  public void toFile(final File outputFile) throws IOException {

    // Start stdout thread
    final Thread tout = new Thread(new ProcessThreadStdOut(this.process,
        new FileOutputStream(outputFile)));
    tout.start();
  }

  /**
   * Write a FASTQ entry in single end mode.
   * @param name name of the sequence
   * @param sequence sequence
   * @param quality quality sequence
   * @throws IOException if an exception occurs while writing the sequence
   */
  public void writeEntry(final String name, final String sequence,
      final String quality) throws IOException {

    if (this.pairedEnd) {
      throw new IllegalStateException(
          "Cannot use this writeEntry method in paired-end mode");
    }

    this.writer1.write(ReadSequence.toFastQ(name, sequence, quality) + '\n');
    inputReadsIncr();
  }

  /**
   * Write a FASTQ entry in single end mode.
   * @param read read to write
   * @throws IOException if an exception occurs while writing the sequence
   */
  public void writeEntry1(final ReadSequence read) throws IOException {

    if (read == null) {
      return;
    }

    this.writer1.write(read.toFastQ() + '\n');
    inputReadsIncr();
  }

  /**
   * Write a FASTQ entry in single end mode.
   * @param read read to write
   * @throws IOException if an exception occurs while writing the sequence
   */
  public void writeEntry2(final ReadSequence read) throws IOException {

    if (!this.pairedEnd) {
      throw new IllegalStateException(
          "Cannot use this writeEntry method in paired-end mode");
    }

    if (read == null) {
      return;
    }

    this.writer2.write(read.toFastQ() + '\n');
  }

  /**
   * Write a FASTQ entry in paired-end mode.
   * @param name1 name of the sequence of the first end
   * @param sequence1 sequence of the first end
   * @param quality1 quality sequence of the first end
   * @param name2 name of the sequence of the second end
   * @param sequence2 sequence of of the second end
   * @param quality2 quality sequence of the second end
   * @throws IOException if an error occurs while writing the entry
   */
  public void writeEntry(final String name1, final String sequence1,
      final String quality1, final String name2, final String sequence2,
      final String quality2) throws IOException {

    if (!this.pairedEnd) {
      throw new IllegalStateException(
          "Cannot use this writeEntry method in single-end mode");
    }

    this.writer1.write(ReadSequence.toFastQ(name1, sequence1, quality1) + '\n');
    this.writer2.write(ReadSequence.toFastQ(name2, sequence2, quality2) + '\n');
    inputReadsIncr();
  }

  /**
   * Close writer 1.
   * @throws IOException if an error occurs while closing the first writer
   */
  public void closeWriter1() throws IOException {

    if (this.writer1 != null) {
      this.writer1.close();
    }
  }

  /**
   * Close writer 2.
   * @throws IOException if an error occurs while closing the first writer
   */
  public void closeWriter2() throws IOException {

    if (this.writer2 != null) {
      this.writer2.close();
    }
  }

  /**
   * Closes the streams for standard input for the mapper. After this the
   * writeEntry() methods cannot be used.
   * @throws IOException if an error occurs while closing stream(s)
   * @throws InterruptedException if an error occurs while closing stream(s)
   */
  public void closeEntriesWriter() throws IOException, InterruptedException {

    if (this.writer1 != null) {
      this.writer1.close();
    }

    if (this.writer2 != null) {
      this.writer2.close();
    }

  }

  //
  // Process management
  //

  /**
   * Start the process(es) of the mapper.
   * @throws IOException if an error occurs while starting the process(es)
   * @throws InterruptedException if an error occurs while starting the
   *           process(es)
   */
  private void startProcess() throws IOException, InterruptedException {

    final List<List<String>> cmds = createCommandLines();

    // Launch all the commands
    for (int i = 0; i < cmds.size(); i++) {

      final boolean last = i == cmds.size() - 1;

      this.process = this.executor.execute(cmds.get(i), executionDirectory(),
          last, this.pipeFile1, this.pipeFile2);

      if (!last) {

        Thread.sleep(1000);
      } else {

        this.stdout = new InputStreamWrapper(
            createCustomInputStream(this.process.getInputStream()));
      }
    }
  }

  /**
   * Wait the end of the main process.
   * @throws InterruptedException if an error occurs while waiting the end of
   *           the process
   * @throws IOException if an error occurs while waiting the end of the process
   */
  public void waitFor() throws IOException {

    final int exitValue = this.process.waitFor();
    getLogger().fine("End of process with " + exitValue + " exit value");

    if (exitValue != 0) {
      throw new IOException("Bad error result for "
          + this.mapperName + " execution: " + exitValue);
    }

    // Remove temporary files
    for (File f : this.filesToRemove) {
      removeFile(f);
    }
  }

  /**
   * Remove a temporary file.
   * @param f f file to remove
   */
  private void removeFile(final File f) {

    if (f.exists()) {

      if (!f.delete()) {
        getLogger().warning("Cannot remove temporary file: " + f);
      }
    }
  }

  /**
   * Create pipe writer.
   * @param file the pipe file to create
   * @return a writer on the pipe
   * @throws IOException if an error occurs while creating the pipe or the
   *           writer
   */
  private static Writer createPipeWriter(final File file) throws IOException {

    FileUtils.createNamedPipe(file);

    @SuppressWarnings("resource")
    final RandomAccessFile raf = new RandomAccessFile(file, "rw");

    final OutputStream os = Channels.newOutputStream(raf.getChannel());

    return new OutputStreamWriter(os, StandardCharsets.ISO_8859_1);
  }

  /**
   * Add a list of temporary files to remove at the end of the mapping.
   * @param files files to remove
   */
  protected void addFilesToRemove(final File... files) {

    if (files == null) {
      return;
    }

    for (File f : files) {
      this.filesToRemove.add(f);
    }
  }

  protected void additionalInit() throws IOException {

  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param mapper mapper to use
   * @param pairedEnd paired-end mode
   * @throws IOException if en error occurs
   */
  protected MapperProcess(final AbstractSequenceReadsMapper mapper,
      final boolean pairedEnd) throws IOException {

    if (mapper == null) {
      throw new NullPointerException("The mapper is null");
    }

    try {
      this.mapperName = mapper.getMapperName();
      this.executor = mapper.getExecutor();
      this.pairedEnd = pairedEnd;

      // Define temporary files
      final File tmpDir = mapper.getTempDirectory();
      final String uuid = UUID.randomUUID().toString();

      this.pipeFile1 = new File(tmpDir, "mapper-inputfile1-" + uuid + ".fq");
      this.pipeFile2 = new File(tmpDir, "mapper-inputfile2-" + uuid + ".fq");

      this.writer1 = createPipeWriter(this.pipeFile1);
      this.writer2 = pairedEnd ? createPipeWriter(this.pipeFile2) : null;

      addFilesToRemove(this.pipeFile1, this.pipeFile2);

      additionalInit();

      // Start mapper instance
      startProcess();

    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }
}
