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
package fr.ens.transcriptome.eoulsan.it;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.testng.collections.Sets;

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanITRuntimeException;
import fr.ens.transcriptome.eoulsan.io.comparators.BinaryComparator;
import fr.ens.transcriptome.eoulsan.io.comparators.Comparator;
import fr.ens.transcriptome.eoulsan.io.comparators.FastqComparator;
import fr.ens.transcriptome.eoulsan.io.comparators.LogComparator;
import fr.ens.transcriptome.eoulsan.io.comparators.SAMComparator;
import fr.ens.transcriptome.eoulsan.io.comparators.TextComparator;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * The class manage the output directory of the integrated test for the
 * comparison.
 * @author Sandrine Perrin
 * @since 1.3
 */
public class RegressionResultIT {

  public final static Splitter COMMA_SPLITTER = Splitter.on(' ').trimResults()
      .omitEmptyStrings();

  private final File outputTestDirectory;
  private final Collection<PathMatcher> patternsFilesTreated;
  private final Map<String, File> listFiles;
  private boolean resultComparison;

  // Text to report file for a test
  private final StringBuilder report;

  /**
   * Move all files matching to a pattern in the destination directory, then
   * clean directory. If no pattern defined, moving all files.
   * @param destinationDirectory destination directory
   * @throws IOException if an error occurs while moving file
   */
  public void copyFiles(final File destinationDirectory) throws IOException {

    // Copy output files
    for (Map.Entry<String, File> e : listFiles.entrySet()) {

      // Check file doesn't exist
      if (!new File(destinationDirectory, e.getKey()).exists()) {
        String filename = e.getKey();
        File dest = new File(destinationDirectory, filename);

        // Check not exist in parent directory (corresponding to the input
        // directory for a test)
        File parent = new File(destinationDirectory.getParentFile(), filename);
        if (parent.exists()) {
          // No copy
          continue;
        }

        if (!FileUtils.copyFile(e.getValue(), dest))
          throw new IOException("Error when moving file "
              + e.getKey() + " to " + destinationDirectory.getAbsolutePath()
              + ".");
      }
    }

    this.report.append("SUCCESS: copy files to "
        + destinationDirectory.getAbsolutePath());

    cleanDirectory();
  }

  /**
   * Compare all files matching to a pattern files.If no pattern defined, moving
   * all files.
   * @param expectedOutput instance of RegressionResultIT to compare with this.
   * @throws IOException if on error occurs while clean directory or compare
   *           file
   */
  public void compareTo(final RegressionResultIT expectedOutput)
      throws IOException {

    // Copy list files
    Collection<File> allFilesFromTest =
        new ArrayList<File>(this.listFiles.values());

    // Parse expected files
    for (Map.Entry<String, File> entry : expectedOutput.getFiles().entrySet()) {
      File fileA = entry.getValue();
      File fileB = this.listFiles.get(entry.getKey());

      // retrieve pattern from file

      if (fileB == null) {
        this.resultComparison = false;
        this.report.append("\nfile "
            + fileA.getName() + " no exists in test directory.");
        throw new EoulsanITRuntimeException("Missing file: "
            + entry.getKey() + " in test directory");
      }

      // Comparison
      boolean res = new FilesComparator(fileA, fileB).compare();
      if (!res) {
        this.report.append("\nfile " + fileA.getName() + " comparison: false");
        this.resultComparison = false;
        throw new EoulsanITRuntimeException("Fail comparison for file: "
            + entry.getKey());
      }

      // Remove file from list
      allFilesFromTest.remove(fileB);

      // Add comparison in the report text
      this.report.append("\nfile " + fileA.getName() + " comparison: true");
    }

    // Check file from test are not compare
    if (!allFilesFromTest.isEmpty()) {
      this.report.append("\nthis file(s) exists only in test directory: "
          + Joiner.on(", ").join(allFilesFromTest));

      this.resultComparison = false;
      throw new EoulsanITRuntimeException(
          "Unexpected file in data to test directory: "
              + Joiner.on(" ").join(allFilesFromTest));
    }

    // Update result compare test
    this.resultComparison = this.resultComparison && true;

    // Remove all files not need to compare
    this.cleanDirectory();

  }

  //
  // Private methods
  //

  /**
   * Listing recursively all files in the source directory which match with
   * patterns files defined
   * @return a map with all files which match with pattern
   */
  private Map<String, File> createListFiles(final File sourceDirectory) {

    final Map<String, File> files = Maps.newHashMap();

    for (File file : sourceDirectory.listFiles()) {

      // Treat directory
      if (file.isDirectory())
        files.putAll(createListFiles(file));

      final String filename = file.getName();
      final Path path = new File(filename).toPath();

      // Search file in all patterns
      if (isMathFilenameWithPatterns(path)) {
        files.put(filename, file);
      }
    }

    return Collections.unmodifiableMap(files);
  }

  /**
   * Check a file matching to a pattern. If no pattern define, return always
   * true.
   * @param path instance of path for a file
   * @return true if the path match to one pattern or none pattern otherwise
   *         false
   */
  private boolean isMathFilenameWithPatterns(final Path path) {

    // None pattern, keep all file
    if (this.patternsFilesTreated.isEmpty())
      return true;

    // Parse all patterns
    for (PathMatcher matcher : this.patternsFilesTreated) {
      if (matcher.matches(path))
        return true;
    }

    return false;
  }

  /**
   * Build collection of PathMatcher for selection files to tread according to a
   * pattern file define in test configuration. Patterns set in string with
   * space to separator. Get input and output patterns files.
   * @param collectionPatternsFiles sequences of patterns files
   * @return collection of PathMatcher, one per pattern
   */
  private Collection<PathMatcher> setPatternFilesToTreat(
      final String patternsFiles) {

    final String patternTestConfigurationFile = " test.conf";

    // No pattern define
    if (patternsFiles == null)
      return Collections.emptySet();

    // Init collection
    final Collection<PathMatcher> setPatterns = Sets.newHashSet();

    // Parse patterns
    for (String globSyntax : COMMA_SPLITTER.split(patternsFiles)) {

      // Convert in syntax reading by Java
      final PathMatcher matcher =
          FileSystems.getDefault().getPathMatcher("glob:" + globSyntax);

      // Add in list patterns files to treat
      if (!setPatterns.contains(matcher)) {
        setPatterns.add(matcher);
      }
    }

    // If pattern define, add pattern
    if (!setPatterns.isEmpty()) {

      setPatterns.add(FileSystems.getDefault().getPathMatcher(
          "glob:" + patternTestConfigurationFile));
    }

    // Return unmodifiable collection
    return Collections.unmodifiableCollection(setPatterns);
  }

  /**
   * Clean directory, remove all files do not matching to a pattern. If none
   * pattern define, all files are keeping.
   * @throws EoulsanException if an error occurs while removing files
   */
  private void cleanDirectory() throws IOException {
    // None pattern files define
    if (this.patternsFilesTreated.isEmpty())
      // Keep all files
      return;

    // Remove all files no keeping (no match with patterns)
    cleanDirectory(this.outputTestDirectory);

    // Clean directory
    removeEmptyDirectory(this.outputTestDirectory);
  }

  /**
   * Remove empty directories
   * @param directory directory to treat
   */
  private void removeEmptyDirectory(File directory) {

    // Parse list files
    for (File dir : directory.listFiles()) {

      if (dir.isDirectory()) {
        // Treat sub directories
        removeEmptyDirectory(dir);
        // Delete success only if directory is empty
        dir.delete();
      }
    }
  }

  private void cleanDirectory(final File directory) throws IOException {
    // Parse list files
    for (File file : directory.listFiles()) {

      if (file.isDirectory())
        cleanDirectory(file);

      // Check filename save in list files
      if (!this.listFiles.containsKey(file.getName())) {
        file.delete();
      }
    }
  }

  /**
   * Return a map with file name and instance of file, only file matching to a
   * pattern. If no pattern define, all files of source directory
   * @return map with file name and instance of file
   */
  public Map<String, File> getFiles() {
    return this.listFiles;
  }

  // TODO remove after test
  public boolean getResultComparison() {
    return this.resultComparison;
  }

  public String getReport() {
    return report.toString();
  }

  //
  // Constructor
  //
  /**
   * Public constructor, it build list patterns and create list files from the
   * source directory.
   * @param outputTestDirectory source directory
   * @param inputpatternsFiles sequences of patterns, separated by a space
   * @param outputpatternsFiles sequences of patterns, separated by a space
   */
  public RegressionResultIT(final File outputTestDirectory,
      final String inputPatternsFiles, final String outputPatternsFiles) {
    this.outputTestDirectory = outputTestDirectory;

    this.report = new StringBuilder();

    // Build list patterns
    this.patternsFilesTreated =
        setPatternFilesToTreat(inputPatternsFiles + " " + outputPatternsFiles);
    this.listFiles = createListFiles(this.outputTestDirectory);

    // TODO
    System.out.println("list file for "
        + this.outputTestDirectory + "\n"
        + Joiner.on("\n\t").withKeyValueSeparator("\t").join(listFiles));
  }

  //
  // Internal class
  //

  /**
   * The internal class choice the comparator matching to filename and compare
   * two files.
   * @author Sandrine Perrin
   * @since 1.3
   */
  final class FilesComparator {

    private final List<Comparator> comparators = Lists.newArrayList();
    private final static boolean useSerializationFile = true;

    private final File fileA;
    private final File fileB;
    private final Comparator comparator;

    /**
     * Compare two files
     * @return true if files are the same
     * @throws FileNotFoundException if a file not found.
     * @throws IOException if an error occurs while reading file.
     */
    public boolean compare() throws FileNotFoundException, IOException {

      return comparator.compareFiles(fileA, fileB);
    }

    /**
     * Find the comparator adapted to the file
     * @param filename file name
     * @return instance of a comparator
     */
    private Comparator findComparator(final String filename) {

      final String extension =
          StringUtils.extensionWithoutCompressionExtension(filename);

      for (Comparator comp : this.comparators) {

        // Check extension file in list extensions define by comparator
        if (comp.getExtensions().contains(extension))
          return comp;
      }

      // None comparator find by extension file, return the default comparator
      return this.comparators.get(0);
    }

    //
    // Constructor
    //

    /**
     * Public constructor, initialization collection of comparators.
     * @param fileA first file
     * @param fileB second file
     */
    FilesComparator(final File fileA, final File fileB) {

      this.fileA = fileA;
      this.fileB = fileB;

      // Binary comparator is default comparator, always at first position
      comparators.add(new BinaryComparator());

      comparators.add(new FastqComparator(useSerializationFile));
      comparators.add(new SAMComparator(useSerializationFile, "PG"));
      comparators.add(new TextComparator(useSerializationFile));
      comparators.add(new LogComparator());

      this.comparator = findComparator(this.fileA.getName());

    }
  }

}
