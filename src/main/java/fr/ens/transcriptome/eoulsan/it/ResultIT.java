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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static com.google.common.collect.Sets.newHashSet;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingDirectoryFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * @since 2.0
 */
public class ResultIT {

  private static final Splitter COMMA_SPLITTER = Splitter.on(' ').trimResults()
      .omitEmptyStrings();

  private static PathMatcher ALL_PATH_MATCHER = FileSystems.getDefault()
      .getPathMatcher("glob:*");

  private final Set<PathMatcher> patternsFilesTreated;
  private final Set<PathMatcher> allPatternsFiles;
  private final Set<PathMatcher> outputExcludePatternsFiles;
  private final List<File> filesList;
  private final File directory;

  /**
   * Move all files matching to a pattern in the destination directory, then
   * clean directory. If no pattern defined, moving all files.
   * @param destinationDirectory destination directory
   * @throws IOException if an error occurs while moving file
   * @throws EoulsanException if no file copy in destination directory
   */
  public final void copyFiles(final File destinationDirectory)
      throws IOException, EoulsanException {

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
   * @return instance of {@link fr.ens.transcriptome.eoulsan.it.OutputExecution}
   *         which summary result of directories comparison
   * @throws IOException if on error occurs while clean directory or compare
   *           file
   */
  public final OutputExecution compareTo(final ResultIT expectedOutput)
      throws IOException {

    // Copy list files
    final List<File> allFilesFromTest = new ArrayList<File>(this.filesList);

    // Build map filename with files path
    Map<String, File> filesTestedMap =
        newHashMapWithExpectedSize(expectedOutput.getFilesList().size());

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
      final FilesComparator fc = new FilesComparator(fileExpected, fileTested);
      boolean res = fc.compare();

      if (!res) {
        msg =
            "Fail comparison with file: "
                + fileExpected.getAbsolutePath() + " vs "
                + fileTested.getAbsolutePath() + "\n\tdetail: "
                + fc.getDetailComparison();
        
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
   * patterns files definedsourceDirectory
   * @param sourceDirectory source directory
   * @return a map with all files which match with pattern
   * @throws IOException if an error occurs while parsing input directory
   */
  private List<File> createListFiles(final File sourceDirectory)
      throws IOException {

    checkExistingDirectoryFile(sourceDirectory, "source directory");

    final List<File> files = newArrayList();

    for (File file : sourceDirectory.listFiles()) {

      // Apply excluding pattern
      if (isFilenameMatches(this.outputExcludePatternsFiles, file.getName())) {
        continue;
      }

      // Process sub-directories
      if (file.isDirectory()) {
        files.addAll(createListFiles(file));
      }

      // Search files that match with one of all patterns
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

    checkNotNull(patterns, "patterns argument cannot be null");

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
   * @param patterns sequences of patterns files
   * @return collection of PathMatcher, one per pattern
   */
  private static Set<PathMatcher> createPathMatchers(final String patterns) {

    // No pattern defined
    if (patterns == null || patterns.trim().isEmpty()) {

      return Collections.singleton(ALL_PATH_MATCHER);
    }

    // Init collection
    final Set<PathMatcher> result = newHashSet();

    // Parse patterns
    for (String globSyntax : COMMA_SPLITTER.split(patterns)) {

      // Convert in syntax reading by Java
      final PathMatcher matcher =
          FileSystems.getDefault().getPathMatcher("glob:" + globSyntax);

      // Add in list patterns files to treat
      result.add(matcher);
    }

    // Return unmodifiable collection
    return Collections.unmodifiableSet(result);
  }

  //
  // Getter & setter
  //

  /**
   * Return a map with file name and instance of file, only file matching to a
   * pattern. If no pattern define, all files of source directory
   * @return map with file name and instance of file
   */
  public final List<File> getFilesList() {
    return this.filesList;
  }

  //
  // Constructor
  //

  /**
   * Public constructor, it build list patterns and create list files from the
   * source directory.
   * @param outputTestDirectory source directory
   * @param inputPatternsFiles sequences of patterns, separated by a space
   * @param outputPatternsFiles sequences of patterns, separated by a space
   * @param outputExcludePattern sequences of patterns, separated by a space
   * @throws IOException if an error occurs while parsing input directory
   */
  public ResultIT(final File outputTestDirectory,
      final String inputPatternsFiles, final String outputPatternsFiles,
      final String outputExcludePattern) throws IOException {
    this.directory = outputTestDirectory;

    // Build list patterns
    this.patternsFilesTreated = createPathMatchers(outputPatternsFiles);
    this.allPatternsFiles =
        createPathMatchers(inputPatternsFiles + " " + outputPatternsFiles);
    this.outputExcludePatternsFiles = createPathMatchers(outputExcludePattern);

    this.filesList = createListFiles(this.directory);
  }

  //
  // Internal class
  //

  /**
   * The internal class represents output result of directories comparison with
   * boolean of the global result and a report text.
   * @author Sandrine Perrin
   * @since 2.0
   */
  final class OutputExecution {

    private StringBuilder report = new StringBuilder();
    private boolean result = true;

    /**
     * Gets the report.
     * @return the report
     */
    public String getReport() {
      return report.toString();
    }

    /**
     * Checks if is result.
     * @return true, if is result
     */
    public boolean isResult() {
      return result;
    }

    /**
     * Sets the result.
     * @param res the new result
     */
    public void setResult(final boolean res) {
      this.result = res;
    }

    /**
     * Update report to directories comparison
     * @param msg message added to the report text
     */
    public void appendReport(final String msg) {
      if (report.length() == 0)
        this.report.append(msg);
      else {
        this.report.append("\n");
        this.report.append(msg);
      }
    }

    /**
     * Update report and result to directories comparison
     * @param msg message added to the report text
     * @param resultIntermedary boolean result of comparison for a file between
     *          two directories
     */
    public void appendComparison(final String msg,
        final boolean resultIntermedary) {
      appendReport(msg);
      setResult(resultIntermedary && isResult());
    }
  }

  /**
   * The internal class choice the comparator matching to filename and compare
   * two files.
   * @author Sandrine Perrin
   * @since 2.0
   */
  private static final class FilesComparator {

    private final List<Comparator> comparators = newArrayList();
    private static final boolean USE_SERIALIZATION_FILE = true;

    private final File fileA;
    private final File fileB;
    private final Comparator comparator;

    private String detailComparison = "SUCCESS";

    /**
     * Compare two files
     * @return true if files are the same
     * @throws IOException if an error occurs while reading file.
     */
    public boolean compare() throws IOException {
      final boolean b = comparator.compareFiles(fileA, fileB);

      if (!b) {
        this.detailComparison =
            "fail at "
                + comparator.getNumberElementsCompared()
                + " comparisons, with this line "
                + comparator.getCauseFailComparison();
      }
      return b;
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
    // Getter
    //
    public String getDetailComparison() {
      return this.detailComparison;
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

      comparators.add(new FastqComparator(USE_SERIALIZATION_FILE));
      comparators.add(new SAMComparator(USE_SERIALIZATION_FILE, "PG", "HD"));
      comparators.add(new TextComparator(USE_SERIALIZATION_FILE));
      comparators.add(new LogComparator());

      this.comparator = findComparator(this.fileA.getName());

    }
  }

}
