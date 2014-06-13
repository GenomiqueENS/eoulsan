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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static com.google.common.collect.Sets.newHashSet;

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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

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
public class ResultIT {

  public final static Splitter COMMA_SPLITTER = Splitter.on(' ').trimResults()
      .omitEmptyStrings();

  private final Collection<PathMatcher> patternsFilesTreated;
  private final Collection<PathMatcher> allPatternsFiles;
  private final List<File> filesList;
  private final File directory;

  /**
   * Move all files matching to a pattern in the destination directory, then
   * clean directory. If no pattern defined, moving all files.
   * @param destinationDirectory destination directory
   * @throws IOException if an error occurs while moving file
   * @throws EoulsanException if no file copy in destination directory
   */
  public void copyFiles(final File destinationDirectory) throws IOException,
      EoulsanException {

    // Check at least on file match with a pattern
    boolean noFileMatchPatterns = false;

    // Copy output files
    for (File f : filesList) {

      final String filename = f.getName();

      // Check file doesn't exist
      if (!new File(destinationDirectory, filename).exists()) {
        final File dest = new File(destinationDirectory, filename);

        // Check not exist in parent directory (corresponding to the input
        // directory for a test)
        final File parent =
            new File(destinationDirectory.getParentFile(), filename);

        if (parent.exists()) {
          // No copy
          continue;
        }

        if (!FileUtils.copyFile(f, dest))
          throw new IOException("Error when moving file "
              + filename + " to " + destinationDirectory.getAbsolutePath()
              + ".");

        noFileMatchPatterns =
            noFileMatchPatterns
                || isFilenameMatches(this.patternsFilesTreated, filename);
      }
    }

    if (!noFileMatchPatterns) {
      String msg =
          "Fail: none file in source directory corresponding to output file pattern";
      throw new EoulsanException(msg);
    }

    // TODO active after test
    // Clean directory
    // cleanDirectory();
  }

  /**
   * Compare all files matching to a pattern files.If no pattern defined, moving
   * all files.
   * @param expectedOutput instance of RegressionResultIT to compare with this.
   * @throws IOException if on error occurs while clean directory or compare
   *           file
   */
  public OutputExecution compareTo(final ResultIT expectedOutput)
      throws IOException {

    // Copy list files
    final List<File> allFilesFromTest = new ArrayList<File>(this.filesList);

    Map<String, File> filesTestedMap =
        newHashMapWithExpectedSize(expectedOutput.getFilesList().size());

    // Build map filename with filepath
    for (File f : this.filesList) {
      filesTestedMap.put(f.getName(), f);
    }

    final OutputExecution comparison = new OutputExecution();
    String msg;

    // Parse expected files
    for (File fileExpected : expectedOutput.getFilesList()) {
      final String filename = fileExpected.getName();
      final File fileTested = filesTestedMap.get(filename);

      if (fileTested == null) {
        msg = "Missing file: " + filename + " in test directory";
        comparison.appendComparison(msg, false);

        throw new EoulsanITRuntimeException(msg);
      }

      // Comparison two files with same filename
      boolean res = new FilesComparator(fileExpected, fileTested).compare();

      if (!res) {
        msg =
            "Fail comparison with file: "
                + fileExpected.getAbsolutePath() + " vs "
                + fileTested.getAbsolutePath();
        comparison.appendComparison(msg, false);

        throw new EoulsanITRuntimeException(msg);
      }

      // Remove file from list
      allFilesFromTest.remove(fileTested);

      // Add comparison in the report text
      comparison.appendComparison("Success file comparison: " + filename, true);
    }

    // Check file from test are not compare
    if (!allFilesFromTest.isEmpty()) {
      msg =
          "Unexpected file in data to test directory: "
              + Joiner.on(" ").join(allFilesFromTest);
      comparison.appendComparison(msg, false);

      throw new EoulsanITRuntimeException(msg);
    }

    // TODO active after test
    // Remove all files not need to compare
    // this.cleanDirectory();

    //
    return comparison;
  }

  //
  // Private methods
  //

  /**
   * Listing recursively all files in the source directory which match with
   * patterns files defined
   * @return a map with all files which match with pattern
   */
  private List<File> createListFiles(final File sourceDirectory) {

    final List<File> files = newArrayList();

    for (File file : sourceDirectory.listFiles()) {

      // Treat directory
      if (file.isDirectory())
        files.addAll(createListFiles(file));

      // Search file in all patterns
      if (isFilenameMatches(this.allPatternsFiles, file.getName())) {
        files.add(file);
      }
    }

    // Return unmodifiable list
    return Collections.unmodifiableList(files);
  }

  /**
   * Check a file matching to a pattern. If no pattern define, return always
   * true.
   * @param patterns patterns for filename
   * @param filename the file name
   * @return true if the path match to one pattern or none pattern otherwise
   *         false
   */
  private boolean isFilenameMatches(final Collection<PathMatcher> patterns,
      final String filename) {

    // None pattern, keep all file
    if (patterns.isEmpty())
      return true;

    // Ignore compression extension file
    final String filenameWithoutCompressionExtension =
        StringUtils.filenameWithoutCompressionExtension(filename);
    final Path path = new File(filenameWithoutCompressionExtension).toPath();

    // Parse all patterns
    for (PathMatcher matcher : patterns) {
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
  private Collection<PathMatcher> setPatternFiles(final String patternsFiles) {

    final String patternTestConfigurationFile = "test.conf";

    // No pattern define
    if (patternsFiles == null)
      return Collections.emptySet();

    // Init collection
    final Collection<PathMatcher> setPatterns = newHashSet();

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
  @SuppressWarnings("unused")
  private void cleanDirectory() throws IOException {
    // None pattern files define
    if (this.allPatternsFiles.isEmpty())
      // Keep all files
      return;

    // Remove all files no keeping (no match with patterns)
    cleanDirectory(this.directory);

    // Clean directory
    removeEmptyDirectory(this.directory);
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
      if (!this.filesList.contains(file)) {
        file.delete();
      }
    }
  }

  //
  // Getter & setter
  //

  /**
   * Return a map with file name and instance of file, only file matching to a
   * pattern. If no pattern define, all files of source directory
   * @return map with file name and instance of file
   */
  public List<File> getFilesList() {
    return this.filesList;
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
  public ResultIT(final File outputTestDirectory,
      final String inputPatternsFiles, final String outputPatternsFiles) {
    this.directory = outputTestDirectory;

    // Build list patterns
    this.patternsFilesTreated = setPatternFiles(outputPatternsFiles);
    this.allPatternsFiles =
        setPatternFiles(inputPatternsFiles + " " + outputPatternsFiles);

    this.filesList = createListFiles(this.directory);
  }

  //
  // Internal class
  //
  final class OutputExecution {

    private StringBuilder report = new StringBuilder();
    private boolean result = true;

    public String getReport() {
      return report.toString();
    }

    public boolean isResult() {
      return result;
    }

    public void setResult(boolean result) {
      this.result = result;
    }

    public void appendReport(final String msg) {
      if (report.length() == 0)
        this.report.append(msg);
      else
        this.report.append("\n" + msg);
    }

    public void appendComparison(final String msg, final boolean result) {
      appendReport(msg);
      setResult(result && isResult());
    }
  }

  /**
   * The internal class choice the comparator matching to filename and compare
   * two files.
   * @author Sandrine Perrin
   * @since 1.3
   */
  static final class FilesComparator {

    private final List<Comparator> comparators = newArrayList();
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
