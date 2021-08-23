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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.bio.readsmappers;

import static java.util.Objects.requireNonNull;

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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.MapperExecutor.Result;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;

/**
 * This class define an abstract class that is returned by a mapper.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class MapperProcess {

  private final String mapperName;
  private final String uuid;
  private final MapperExecutor executor;
  private final boolean pairedEnd;

  private final List<Result> processResults = new ArrayList<>();

  private InputStream stdout;

  private final File pipeFile1;
  private final File pipeFile2;
  private final File stdErrFile;

  private final FastqWriter writer1;
  private final FastqWriter writer2;

  private final File temporaryDirectory;

  private String commandLine;
  private ReporterIncrementer incrementer;
  private String counterGroup;

  private final List<File> filesToRemove = new ArrayList<>();

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

      final int exitValue =
          MapperProcess.this.getStdoutProcessResult().waitFor();

      executor.getLogger()
          .debug("End of process with " + exitValue + " exit value");

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

  /**
   * This interface define how to write read to the mapper input.
   */
  private interface FastqWriter extends AutoCloseable {

    /**
     * Write a string to the pipe.
     * @param s string to write
     * @throws IOException if an error has occurred in writings
     */
    void write(final String s) throws IOException;

    /**
     * Close the writer.
     */
    void close() throws IOException;

  }

  /**
   * This class allow to do synchronous writes in a named piped.
   */
  static class FastqWriterNoThread implements FastqWriter {

    final Writer writer;

    @Override
    public void write(final String s) throws IOException {

      this.writer.write(s);
    }

    @Override
    public void close() throws IOException {

      this.writer.close();
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param writer the writer to use to write data
     */
    public FastqWriterNoThread(final Writer writer) {

      this.writer = writer;
    }

    /**
     * Constructor.
     * @param namedPipeFile the named pipe file
     */
    public FastqWriterNoThread(final File namedPipeFile) throws IOException {

      this(createPipeWriter(namedPipeFile));
    }
  }

  /**
   * This class allow to do asynchronous writes in a named piped.
   */
  static class FastqWriterThread extends Thread implements FastqWriter {

    // The queue can store a little more than 1,00,000 * 1000 = 100,000,000
    // characters
    private static final int MAX_CAPACITY = 100000;
    private static final int MIN_LINE_SIZE = 1000;

    private volatile boolean closed;
    private final BlockingDeque<String> queue =
        new LinkedBlockingDeque<>(MAX_CAPACITY);
    private final Writer writer;
    private Exception exception;

    private int lineCount;
    private final StringBuilder buffer = new StringBuilder();

    @Override
    public void run() {

      try {
        while (!this.closed || !queue.isEmpty()) {

          if (!this.queue.isEmpty()) {

            while (!this.queue.isEmpty()) {

              this.writer.write(queue.take());
            }

          } else {
            Thread.sleep(1000);
          }
        }

        this.writer.close();

      } catch (IOException e) {
        this.exception = e;
      } catch (InterruptedException e) {
        this.exception = new IOException(e);
      }
    }

    /**
     * Write a string to the pipe. This method is not synchronized.
     * @param s string to write
     * @throws IOException if an error has occurred in writings
     */
    @Override
    public void write(final String s) throws IOException {

      if (this.closed) {
        throw new IllegalStateException("FastqWriterThread is closed");
      }

      this.buffer.append(s);
      this.lineCount++;

      // We only add lines of about 1000 character in the queue
      if (this.buffer.length() < MIN_LINE_SIZE || this.lineCount % 4 != 0) {
        return;
      }

      if (this.queue.remainingCapacity() == 0) {

        this.writer.flush();

        while (this.queue.remainingCapacity() == 0) {

          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
          }
        }
      }

      this.queue.add(this.buffer.toString());
      this.buffer.setLength(0);
      this.lineCount = 0;

      throwExceptionIfExists();
    }

    /**
     * Asynchronous close. This method is not synchronized. A call to write()
     * just after close() may to lead to lose data.
     */
    @Override
    public void close() throws IOException {

      this.queue.add(buffer.toString());
      this.buffer.setLength(0);
      this.closed = true;

      try {
        join();
      } catch (InterruptedException e) {
        throw new IOException(e);
      }

      throwExceptionIfExists();
    }

    /**
     * Throw an exception if an exception has occurred while writing data.
     * @throws IOException if an exception has occurred while writing data
     */
    private void throwExceptionIfExists() throws IOException {

      if (this.exception != null) {
        throw new IOException(this.exception);
      }
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param writer the writer to use to write data
     */
    public FastqWriterThread(final Writer writer, final String threadName) {

      super(threadName);

      this.writer = writer;

      // Start the thread
      start();
    }

    /**
     * Constructor.
     * @param namedPipeFile the named pipe file
     */
    public FastqWriterThread(final File namedPipeFile, final String threadName)
        throws IOException {

      this(createPipeWriter(namedPipeFile), threadName);
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

  /**
   * Get the the UUID generated for the mapper process.
   * @return the UUID generated for the mapper process
   */
  protected String getUUID() {

    return this.uuid;
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

  /**
   * Return the executed command line.
   * @return the executed command line
   */
  public String getCommandLine() {
    return this.commandLine;
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
    final Thread tout =
        new Thread(new ProcessThreadStdOut(getStdoutProcessResult(),
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

      // Wait few seconds before closing the pipe
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
      }

      this.writer1.close();
    }
  }

  /**
   * Close writer 2.
   * @throws IOException if an error occurs while closing the first writer
   */
  public void closeWriter2() throws IOException {

    if (this.writer2 != null) {

      // Wait few seconds before closing the pipe
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
      }

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
   * Get the process result object which stdout is used to the SAM output.
   * @return a Result object
   */
  private Result getStdoutProcessResult() {

    final int index = this.processResults.size() - 1;

    if (index < 0) {
      throw new IllegalStateException("No mapper process has been launched");
    }

    return this.processResults.get(index);
  }

  /**
   * Start the process.
   * @throws IOException if an error occurs while starting the process
   */
  void startProcess() throws IOException {

    try {
      // Start mapper instance
      startProcess(this.temporaryDirectory);
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  /**
   * Start the process(es) of the mapper.
   * @param tmpDirectory temporary directory
   * @throws IOException if an error occurs while starting the process(es)
   * @throws InterruptedException if an error occurs while starting the
   *           process(es)
   */
  private void startProcess(final File tmpDirectory)
      throws IOException, InterruptedException {

    final List<List<String>> cmds = createCommandLines();
    this.commandLine = commandLinesToString(cmds);
    final File executionDirectory =
        executionDirectory() == null ? tmpDirectory : executionDirectory();

    // Launch all the commands
    for (int i = 0; i < cmds.size(); i++) {

      final boolean last = i == cmds.size() - 1;

      final Result result =
          this.executor.execute(cmds.get(i), executionDirectory, last,
              this.stdErrFile, false, this.pipeFile1, this.pipeFile2);

      this.processResults.add(result);

      if (!last) {

        Thread.sleep(1000);
      } else {

        this.stdout = new InputStreamWrapper(
            createCustomInputStream(result.getInputStream()));
      }
    }
  }

  /**
   * Wait the end of the main process.
   * @throws IOException if an error occurs while waiting the end of the process
   */
  public void waitFor() throws IOException {

    for (Result result : this.processResults) {

      final int exitValue = result.waitFor();
      this.executor.getLogger()
          .debug("End of process with " + exitValue + " exit value");

      if (exitValue != 0) {
        throw new IOException("Bad error result for "
            + this.mapperName + " execution: " + exitValue);
      }
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

    if (f != null && f.exists()) {

      if (!f.delete()) {
        this.executor.getLogger().warn("Cannot remove temporary file: " + f);
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

    Collections.addAll(this.filesToRemove, files);
  }

  protected void additionalInit() throws IOException {

  }

  /**
   * Convert command lines to a String.
   * @param cmds the command lines
   * @return a String with the command lines
   */
  private static String commandLinesToString(List<List<String>> cmds) {

    if (cmds == null) {
      return "";
    }

    boolean first = true;

    StringBuilder sb = new StringBuilder();

    for (List<String> cmd : cmds) {

      if (cmd == null) {
        continue;
      }

      if (first) {
        first = false;
      } else {
        sb.append(" ; ");
      }

      sb.append(String.join(" ", cmd));

    }

    return sb.toString();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param mapperName mapper name
   * @param executor executor
   * @param temporaryDirectory temporary directory
   * @param pairedEnd paired-end mode
   * @throws IOException if an error occurs
   */
  protected MapperProcess(final String mapperName, MapperExecutor executor,
      final File temporaryDirectory, final File stdErrFile,
      final boolean pairedEnd) throws IOException {

    this(mapperName, executor, temporaryDirectory, stdErrFile, pairedEnd,
        false);
  }

  /**
   * Constructor.
   * @param mapperName mapper name
   * @param executor executor
   * @param temporaryDirectory temporary directory
   * @param pairedEnd paired-end mode
   * @param inputFile1 first file to map
   * @param inputFile2 second file to map
   * @throws IOException if an error occurs
   */
  protected MapperProcess(final String mapperName, MapperExecutor executor,
      final File temporaryDirectory, final File stdErrFile,
      final boolean pairedEnd, final File inputFile1, final File inputFile2)
      throws IOException {

    this(mapperName, executor, temporaryDirectory, stdErrFile, pairedEnd, false,
        inputFile1, inputFile2);
  }

  /**
   * Constructor.
   * @param mapperName mapper name
   * @param executor executor
   * @param temporaryDirectory temporary directory
   * @param pairedEnd paired-end mode
   * @param inputFile first file to map
   * @throws IOException if an error occurs
   */
  protected MapperProcess(final String mapperName, MapperExecutor executor,
      final File temporaryDirectory, final File stdErrFile,
      final boolean pairedEnd, final File inputFile) throws IOException {

    this(mapperName, executor, temporaryDirectory, stdErrFile, pairedEnd, false,
        inputFile, null);
  }

  /**
   * Constructor.
   * @param mapperName mapper name
   * @param executor executor
   * @param temporaryDirectory temporary directory
   * @param pairedEnd paired-end mode
   * @throws IOException if en error occurs
   */
  protected MapperProcess(final String mapperName, MapperExecutor executor,
      final File temporaryDirectory, final File stdErrFile,
      final boolean pairedEnd, final boolean threadForRead1)
      throws IOException {

    this(mapperName, executor, temporaryDirectory, stdErrFile, pairedEnd,
        threadForRead1, null, null);
  }

  /**
   * Constructor.
   * @param mapperName mapper name
   * @param executor executor
   * @param temporaryDirectory temporary directory
   * @param pairedEnd paired-end mode
   * @param inputFile1 first file to map
   * @param inputFile2 second file to map
   * @throws IOException if en error occurs
   */
  protected MapperProcess(final String mapperName, MapperExecutor executor,
      final File temporaryDirectory, final File stdErrFile,
      final boolean pairedEnd, final boolean threadForRead1,
      final File inputFile1, final File inputFile2) throws IOException {

    requireNonNull(mapperName, "mapperName argument cannot be null");
    requireNonNull(executor, "executor argument cannot be null");
    requireNonNull(temporaryDirectory,
        "temporaryDirectory argument cannot be null");

    this.mapperName = mapperName;
    this.uuid = UUID.randomUUID().toString();

    this.executor = executor;
    this.pairedEnd = pairedEnd;

    this.temporaryDirectory = temporaryDirectory;

    this.pipeFile1 = inputFile1 != null
        ? inputFile1 : new File(this.temporaryDirectory,
            "mapper-inputfile1-" + uuid + ".fq");
    this.pipeFile2 = inputFile2 != null
        ? inputFile2 : new File(this.temporaryDirectory,
            "mapper-inputfile2-" + uuid + ".fq");

    this.stdErrFile = stdErrFile;

    // If in entry mode
    if (inputFile1 == null) {

      this.writer1 = threadForRead1
          ? new FastqWriterThread(this.pipeFile1, "FastqWriterThread fastq1")
          : new FastqWriterNoThread(this.pipeFile1);
      this.writer2 = pairedEnd
          ? new FastqWriterThread(this.pipeFile2, "FastqWriterThread fastq2")
          : null;

      addFilesToRemove(this.pipeFile1, this.pipeFile2);
    } else {
      this.writer1 = null;
      this.writer2 = null;
    }

    // Launch addition initialization
    additionalInit();
  }
}
