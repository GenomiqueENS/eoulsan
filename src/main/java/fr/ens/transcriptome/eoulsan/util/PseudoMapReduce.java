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

package fr.ens.transcriptome.eoulsan.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;

/**
 * This class implements a pseudo map-reduce framework.
 * @author Laurent Jourdren
 */
public abstract class PseudoMapReduce {

  /** Logger. */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private File tmpDir;
  private File mapOutputFile;
  private File sortOutputFile;
  private Reporter reporter = new Reporter();

  //
  // Abstract methods
  //

  /**
   * Mapper.
   * @param value input of the mapper
   * @param output List of output of the mapper
   * @param reporter reporter
   * @throws IOException if an error occurs while executing the mapper
   */
  public abstract void map(final String value, final List<String> output,
      final Reporter reporter) throws IOException;

  /**
   * Reducer
   * @param key input key of the reducer
   * @param values values for the key
   * @param output list of output values of the reducer
   * @param reporter reporter
   * @throws IOException if an error occurs while executing the reducer
   */
  public abstract void reduce(final String key, Iterator<String> values,
      final List<String> output, final Reporter reporter) throws IOException;

  //
  // Getter
  //

  /**
   * Get the reporter object of the Pseudo map reduce.
   * @return the reporter object.
   */
  public Reporter getReporter() {

    return this.reporter;
  }

  //
  // Mapper management
  //

  /**
   * Execute the map phase with a file as input.
   * @param inputFile input file for the mapper
   */
  public void doMap(final File inputFile) throws IOException {

    if (inputFile == null)
      throw new NullPointerException("The input file is null.");

    doMap(FileUtils.createInputStream(inputFile));
  }

  /**
   * Execute the map phase with an Inputstream as input.
   * @param is input stream for the mapper
   */
  public void doMap(final InputStream is) throws IOException {

    if (is == null)
      throw new NullPointerException("The input stream is null.");

    this.reporter.clear();

    this.mapOutputFile = File.createTempFile("map-", ".txt", this.tmpDir);

    final BufferedReader br = new BufferedReader(new InputStreamReader(is));
    final UnSynchronizedBufferedWriter bw =
        FileUtils.createBufferedWriter(this.mapOutputFile);

    final List<String> results = new ArrayList<String>();
    String line;

    final StringBuilder sb = new StringBuilder();

    while ((line = br.readLine()) != null) {

      map(line, results, this.reporter);

      for (String r : results) {
        sb.setLength(0);
        sb.append(r);
        sb.append('\n');
        bw.write(sb.toString());
      }

      results.clear();
    }

    br.close();
    bw.close();
  }

  //
  // Sort management
  //

  /**
   * Set the temporary directory.
   * @param dirPath the temporary directory
   */
  public void setMapReduceTemporaryDirectory(final File directory) {

    this.tmpDir = directory;
  }

  /**
   * Get the temporary directory.
   * @return the temporary directory
   */
  public File getMapReduceTemporaryDirectory() {

    return this.tmpDir;
  }

  private boolean sort() throws IOException {

    this.sortOutputFile = File.createTempFile("sort-", ".txt", this.tmpDir);

    final String cmd =
        "sort"
            + (this.tmpDir != null ? " -T "
                + StringUtils.bashEscaping(this.tmpDir.getAbsolutePath()) : "")
            + " -o "
            + StringUtils.bashEscaping(this.sortOutputFile.getAbsolutePath())
            + " "
            + StringUtils.bashEscaping(this.mapOutputFile.getAbsolutePath());

    final boolean result = ProcessUtils.system(cmd) == 0;
    if (!this.mapOutputFile.delete())
      logger.warning("Can not delete map output file: "
          + this.mapOutputFile.getAbsolutePath());

    return result;
  }

  //
  // Reducer management
  //

  /**
   * Execute the reduce phase with a file as output.
   * @param outputFile output file for the reducer
   */
  public void doReduce(final File outputFile) throws IOException {

    if (outputFile == null)
      throw new NullPointerException("The output file is null.");

    doReduce(FileUtils.createOutputStream(outputFile));
  }

  /**
   * Execute the reduce phase with an OutputStream as output.
   * @param os output stream for the reducer
   */
  public void doReduce(final OutputStream os) throws IOException {

    if (os == null)
      throw new NullPointerException("The output stream is null.");

    if (!sort())
      throw new IOException("Unable to sort/shuffle data.");

    // Create reader
    final BufferedReader br =
        FileUtils.createBufferedReader(this.sortOutputFile);

    // Create writer
    final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));

    String line = null;
    String currentKey = null;
    final List<String> values = new ArrayList<String>();
    final List<String> results = new ArrayList<String>();

    final StringBuilder sb = new StringBuilder();

    while ((line = br.readLine()) != null) {

      final int indexFirstTab = line.indexOf('\t');

      final String key = line.substring(0, indexFirstTab);
      final String value = line.substring(indexFirstTab + 1);

      if (!key.equals(currentKey)) {

        reduce(currentKey, values.iterator(), results, this.reporter);

        for (String result : results) {
          sb.setLength(0);
          sb.append(result);
          sb.append('\n');
          bw.write(sb.toString());
        }

        results.clear();
        values.clear();
        currentKey = key;
      }

      values.add(value);
    }

    // Process lasts values
    reduce(currentKey, values.iterator(), results, this.reporter);
    for (String result : results)
      bw.write(result);

    br.close();
    bw.close();
    if (!this.sortOutputFile.delete())
      logger.warning("Can not delete sort output file: "
          + this.sortOutputFile.getAbsolutePath());
  }

}
