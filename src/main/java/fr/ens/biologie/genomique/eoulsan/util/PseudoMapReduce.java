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

package fr.ens.biologie.genomique.eoulsan.util;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

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

import fr.ens.biologie.genomique.kenetre.io.FileUtils;
import fr.ens.biologie.genomique.kenetre.io.UnSynchronizedBufferedWriter;
import fr.ens.biologie.genomique.kenetre.util.LocalReporter;
import fr.ens.biologie.genomique.kenetre.util.Reporter;

/**
 * This class implements a pseudo map-reduce framework.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class PseudoMapReduce {

  /* Default Charset. */
  private static final Charset CHARSET =
      Charset.forName(System.getProperty("file.encoding"));

  private File tmpDir;
  private final List<File> listMapOutputFile = new ArrayList<>();

  private File sortOutputFile;
  private final LocalReporter reporter = new LocalReporter();

  /**
   * This class avoid storing repeated entries of a list in memory.
   * @author Laurent Jourdren
   */
  private static final class RepeatedEntriesList<E> extends AbstractList<E> {

    private int count;
    private final Map<E, Integer> map = new LinkedHashMap<>();

    @Override
    public E get(final int index) {

      throw new UnsupportedOperationException();
    }

    @Override
    public int size() {

      return this.count;
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

        private final Iterator<Map.Entry<E, Integer>> it =
            RepeatedEntriesList.this.map.entrySet().iterator();
        private E currentValue;
        private int currentCount;

        @Override
        public boolean hasNext() {

          return this.it.hasNext() || this.currentCount > 0;
        }

        @Override
        public E next() {

          if (this.currentCount == 0) {

            if (!this.it.hasNext()) {
              return null;
            }

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

    if (inputFile == null) {
      throw new NullPointerException("The input file is null.");
    }

    doMap(FileUtils.createInputStream(inputFile));
  }

  protected File getMapOutputTempFile() throws IOException {

    final File outputFile = File.createTempFile("map-", ".txt", this.tmpDir);
    this.listMapOutputFile.add(outputFile);

    return outputFile;
  }

  /**
   * Execute the map phase with an InputStream as input Create a list of file :
   * one for each index file used
   * @param is input stream for the mapper
   */
  public void doMap(final InputStream is) throws IOException {

    if (is == null) {
      throw new NullPointerException("The input stream is null.");
    }

    this.reporter.clear();

    final BufferedReader br =
        new BufferedReader(new InputStreamReader(is, CHARSET));
    final UnSynchronizedBufferedWriter bw =
        FileUtils.createFastBufferedWriter(getMapOutputTempFile());

    final List<String> results = new ArrayList<>();
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
   * Sort several files in sortOutputFile
   * @return true if success sort
   * @throws IOException if an error occurs while executing external sort
   *           command
   */
  private boolean sort() throws IOException {

    this.sortOutputFile = File.createTempFile("sort-", ".txt", this.tmpDir);

    // Create command line to execute
    final List<String> command = new ArrayList<>();
    command.add("sort");

    // Set the temporary directory if needed
    if (this.tmpDir != null) {
      command.add("-T");
      command.add(this.tmpDir.getAbsolutePath());
    }

    // Set the output file
    command.add("-o");
    command.add(this.sortOutputFile.getAbsolutePath());

    // Set the files to sort
    for (File mapOutputFile : this.listMapOutputFile) {
      command.add(mapOutputFile.getAbsolutePath());
    }

    // Execute command
    final boolean result;
    try {
      result = new ProcessBuilder(command).start().waitFor() == 0;
    } catch (InterruptedException e) {
      throw new IOException(e);
    }

    // Remove temporary map output files
    for (File mapOutputFile : this.listMapOutputFile) {
      if (!mapOutputFile.delete()) {
        getLogger().warning("Can not delete map output file: "
            + mapOutputFile.getAbsolutePath());
      }
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

    if (outputFile == null) {
      throw new NullPointerException("The output file is null.");
    }

    doReduce(FileUtils.createOutputStream(outputFile));
  }

  /**
   * Execute the reduce phase with an OutputStream as output.
   * @param os output stream for the reducer
   */
  public void doReduce(final OutputStream os) throws IOException {

    if (os == null) {
      throw new NullPointerException("The output stream is null.");
    }

    if (!sort()) {
      throw new IOException("Unable to sort/shuffle data.");
    }

    // Create reader
    final BufferedReader br =
        FileUtils.createBufferedReader(this.sortOutputFile);

    // Create writer
    final BufferedWriter bw =
        new BufferedWriter(new OutputStreamWriter(os, CHARSET));

    String line = null;
    String currentKey = null;
    final List<String> values = new RepeatedEntriesList<>();
    final List<String> results = new ArrayList<>();

    final StringBuilder sb = new StringBuilder();

    while ((line = br.readLine()) != null) {

      final int indexFirstTab = line.indexOf('\t');

      // Do not process line
      if (line.isEmpty() || indexFirstTab == -1) {
        continue;
      }

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
    if (currentKey != null) {
      reduce(currentKey, values.iterator(), results, this.reporter);
    }

    for (String result : results) {
      bw.write(result);
    }

    br.close();
    bw.close();
    if (!this.sortOutputFile.delete()) {
      getLogger().warning("Can not delete sort output file: "
          + this.sortOutputFile.getAbsolutePath());
    }
  }

}
