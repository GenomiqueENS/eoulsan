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

package fr.ens.transcriptome.eoulsan.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.base.Objects;

import fr.ens.transcriptome.eoulsan.Globals;

/**
 * This class implements a pseudo map-reduce framework.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class PseudoMapReduce {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  /* Default Charset. */
  private static final Charset CHARSET = Charset.forName(System
      .getProperty("file.encoding"));

  private File tmpDir;
  private File mapOutputFile;
  private List<File> listMapOutputFile = new ArrayList<File>();

  private File sortOutputFile;
  private Reporter reporter = new Reporter();

  /**
   * This class avoid storing repeated entries of a list in memory.
   * @author Laurent Jourdren
   */
  private static final class RepeatedEntriesList<E> extends AbstractList<E> {

    private int count;
    private final Map<E, Integer> map = new LinkedHashMap<E, Integer>();

    @Override
    public E get(final int index) {

      throw new UnsupportedOperationException();
    }

    @Override
    public int size() {

      return count;
    }

    @Override
    public boolean add(final E e) {

      if (this.map.containsKey(e)) {

        final int count = this.map.get(e);
        this.map.put(e, count + 1);

        return true;
      }
      this.map.put(e, 1);

      return true;
    }

    @Override
    public Iterator<E> iterator() {

      return new Iterator<E>() {

        private final Iterator<Map.Entry<E, Integer>> it = map.entrySet()
            .iterator();
        private E currentValue;
        private int currentCount;

        @Override
        public boolean hasNext() {

          return this.it.hasNext() || this.currentCount > 0;
        }

        @Override
        public E next() {

          if (this.currentCount == 0) {

            if (!this.it.hasNext())
              return null;

            final Map.Entry<E, Integer> e = this.it.next();
            this.currentValue = e.getKey();
            this.currentCount = e.getValue();
          }

          this.currentCount--;
          return this.currentValue;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }

      };
    }

    @Override
    public void clear() {

      this.map.clear();
    }

  }

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

  protected File getMapOutputTempFile() throws IOException {

    final File outputFile = File.createTempFile("map-", ".txt", this.tmpDir);
    listMapOutputFile.add(outputFile);

    return outputFile;
  }

  /**
   * Execute the map phase with an Inputstream as input Create a list of file :
   * one for each index file used
   * @param is input stream for the mapper
   */
  public void doMap(final InputStream is) throws IOException {

    if (is == null)
      throw new NullPointerException("The input stream is null.");

    this.reporter.clear();

    final BufferedReader br =
        new BufferedReader(new InputStreamReader(is, CHARSET));
    final UnSynchronizedBufferedWriter bw =
        FileUtils.createFastBufferedWriter(getMapOutputTempFile());

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

  /**
   * TODO save or remove ?? Execute the map phase with an Inputstream as input.
   * @param is input stream for the mapper
   */
  public void doMap_OLD(final InputStream is) throws IOException {

    if (is == null)
      throw new NullPointerException("The input stream is null.");

    this.reporter.clear();

    this.mapOutputFile = File.createTempFile("map-", ".txt", this.tmpDir);

    final BufferedReader br =
        new BufferedReader(new InputStreamReader(is, CHARSET));
    final UnSynchronizedBufferedWriter bw =
        FileUtils.createFastBufferedWriter(this.mapOutputFile);

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
   * @param directory the temporary directory
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

  /**
   * TODO save or remove ??
   */
  private boolean sort_OLD() throws IOException {

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
      LOGGER.warning("Can not delete map output file: "
          + this.mapOutputFile.getAbsolutePath());

    return result;
  }

  /**
   * Sort several files in sortOutputFile
   * @return true if success sort
   * @throws IOException
   */
  private boolean sort() throws IOException {

    this.sortOutputFile = File.createTempFile("sort-", ".txt", this.tmpDir);

    String listFile = "";
    for (File mapOutputFile : listMapOutputFile) {
      listFile +=
          StringUtils.bashEscaping(mapOutputFile.getAbsolutePath()) + " ";
    }

    final String cmd =
        "sort"
            + (this.tmpDir != null ? " -T "
                + StringUtils.bashEscaping(this.tmpDir.getAbsolutePath()) : "")
            + " -o "
            + StringUtils.bashEscaping(this.sortOutputFile.getAbsolutePath())
            + " " + listFile;

    final boolean result = ProcessUtils.system(cmd) == 0;
    for (File mapOutputFile : listMapOutputFile) {
      if (!mapOutputFile.delete())
        LOGGER.warning("Can not delete map output file: "
            + mapOutputFile.getAbsolutePath());
    }
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
    final BufferedWriter bw =
        new BufferedWriter(new OutputStreamWriter(os, CHARSET));

    String line = null;
    String currentKey = null;
    final List<String> values = new RepeatedEntriesList<String>();
    final List<String> results = new ArrayList<String>();

    final StringBuilder sb = new StringBuilder();

    while ((line = br.readLine()) != null) {

      final int indexFirstTab = line.indexOf('\t');

      final String key = line.substring(0, indexFirstTab);
      final String value = line.substring(indexFirstTab + 1);

      if (currentKey == null) {
        currentKey = key;
      } else if (!key.equals(currentKey)) {

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
    LOGGER.warning("Can not delete sort output file: "
        + this.sortOutputFile.getAbsolutePath());
  }

}
