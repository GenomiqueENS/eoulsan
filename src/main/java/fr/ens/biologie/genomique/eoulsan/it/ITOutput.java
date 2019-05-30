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
package fr.ens.biologie.genomique.eoulsan.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.io.comparators.BAMComparator;
import fr.ens.biologie.genomique.eoulsan.io.comparators.BinaryComparator;
import fr.ens.biologie.genomique.eoulsan.io.comparators.Comparator;
import fr.ens.biologie.genomique.eoulsan.io.comparators.FastqComparator;
import fr.ens.biologie.genomique.eoulsan.io.comparators.LogComparator;
import fr.ens.biologie.genomique.eoulsan.io.comparators.SAMComparator;
import fr.ens.biologie.genomique.eoulsan.io.comparators.TextComparator;
import fr.ens.biologie.genomique.eoulsan.it.ITOutputComparisonResult.StatusComparison;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * The class manage the output directory of the integrated test for the
 * comparison.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class ITOutput {

  private static final Splitter SPACE_SPLITTER =
      Splitter.on(' ').trimResults().omitEmptyStrings();

  private static final PathMatcher ALL_PATH_MATCHER =
      FileSystems.getDefault().getPathMatcher("glob:*");

  private static final boolean USE_DEFAULT_PATTERN = true;

  /**
   * Percent difference length between expected and tested file, when need to
   * check existing.
   */
  private static final double PART_DIFFERENCE_LENGTH_FILE = 0.01;

  private final String fileToComparePatterns;
  private final String fileToRemovePatterns;
  private final String excludeToComparePatterns;
  /** Patterns to check file and is not empty */
  private final String checkExistenceFilePatterns;
  /** Patterns to check file not exist in test directory */
  private final String checkAbsenceFilePatterns;
  /** Patterns to check file and compare size */
  private final String checkLengthFilePatterns;

  private final List<File> filesToCompare;
  private final List<File> filesToRemove;
  private final List<File> filesToExclude;
  private final List<File> filesToCheckExistence;
  private final List<File> filesToCheckLength;
  private final List<File> filesToCheckAbsence;
  private final List<File> filesToCheckContent;

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
    boolean noFileFoundToCopy = true;

    if (this.filesToCompare.isEmpty()) {
      // No file to copy in expected directory
      return;
    }

    // Copy output files
    for (final File f : this.filesToCompare) {

      final String filename = f.getName();

      // Check file doesn't exist
      if (!new File(destinationDirectory, filename).exists()) {
        final File dest = new File(destinationDirectory, filename);

        if (Files.copy(f.toPath(), dest.toPath()) == null) {
          throw new IOException("Error when moving file "
              + filename + " to " + destinationDirectory.getAbsolutePath()
              + ".");
        }

        noFileFoundToCopy = false;
      }
    }

    if (noFileFoundToCopy) {
      final String msg = "Fail: none file to copy in destination "
          + destinationDirectory.getAbsolutePath();
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
   * @return a set of ITOutputComparisonResult which summary result of
   *         directories comparison
   * @throws IOException if on error occurs while clean directory or compare
   *           file
   */
  public final Set<ITOutputComparisonResult> compareTo(
      final ITOutput expectedOutput) throws IOException {

    final Set<ITOutputComparisonResult> results = new TreeSet<>();

    // Copy list files
    final List<File> allFilesFromTest = Lists.newArrayList(this.filesToCompare);

    // Build map filename with files path
    final Map<String, File> filesTestedMap =
        new HashMap<>(this.filesToCompare.size());

    for (final File f : allFilesFromTest) {
      filesTestedMap.put(f.getName(), f);
    }

    // Parse expected files
    for (final File fileExpected : expectedOutput.getFilesToCompare()) {

      final String filename = fileExpected.getName();
      final File fileTested = filesTestedMap.get(filename);

      final ITOutputComparisonResult comparisonResult =
          new ITOutputComparisonResult(filename);

      if (fileTested == null) {
        comparisonResult.setResult(StatusComparison.MISSING,
            "missing file in output test directory "
                + this.directory.getAbsolutePath());
      } else {

        compareFiles(comparisonResult, fileExpected, fileTested);
      }
      // Remove file from list
      allFilesFromTest.remove(fileTested);

      // Compile result
      results.add(comparisonResult);
    }

    // Check file from test are not compare
    if (!allFilesFromTest.isEmpty()) {

      for (final File f : allFilesFromTest) {

        final ITOutputComparisonResult ocr = new ITOutputComparisonResult(
            f.getName(), StatusComparison.UNEXPECTED,
            "unexpected file in test data directory "
                + expectedOutput.getDirectory().getAbsolutePath());

        results.add(ocr);
      }
    }

    // Check absence file
    results.addAll(checkAbsenceFileFromPatterns());

    // TODO active after test
    // Remove all files not need to compare
    // this.cleanDirectory();

    if (results.isEmpty()) {
      return Collections.emptySet();
    }

    return results;
  }

  /**
   * Delete file matching on pattern, if is a link, delete the real file too.
   * @param itResult the it result
   * @param isDeleteFileRequired the is delete file required.
   */
  public void deleteFileMatchingOnPattern(final ITResult itResult,
      final boolean isDeleteFileRequired) {

    // Save all symbolic link found with patterns
    final List<File> linksSymbolic = new ArrayList<>();

    StringBuilder msg = new StringBuilder();
    msg.append("\nClean output directory:\n");

    boolean success = true;

    if (!itResult.isSuccess()) {
      msg.append(isDeleteFileRequired
          ? "\tConfiguration required to delete files, but test fail. Files still exist in "
          : "\tConfiguration required always to keep files in ");
      msg.append(this.directory.getAbsolutePath());
    }

    // Case to delete files
    if (itResult.isSuccess() && isDeleteFileRequired) {

      msg.append("\tTest succeeded.");
      msg.append("\n\tConfiguration required to delete files from directory ");
      msg.append(this.directory.getAbsolutePath());

      for (File f : getFilesToRemove()) {

        // Check is a symbolic link or a real path
        if (Files.isSymbolicLink(f.toPath())) {

          linksSymbolic.add(f);
          continue;
        }

        // Delete file
        if (!f.delete()) {
          success = false;
          msg.append("\n\tFail to delete file ");
          msg.append(f.getAbsolutePath());
        }
      }

      // Clean broken symbolic link
      final String s = removeBrokenSymbolicLink(linksSymbolic);
      msg.append(s);

      if (success) {
        msg.append("\n\tAll deletions successful.");
      }
    } else {
      // No required delete file
      msg.append("\n\tDelete file matching on patterns no required.");
    }

    // Update itResult
    itResult.addCommentsIntoTextReport(msg.toString());

  }

  //
  // Private methods
  //

  /**
   * Removes broken symbolic links.
   * @param linksSymbolic the links symbolic
   * @return message to final report
   */
  private String removeBrokenSymbolicLink(final List<File> linksSymbolic) {

    final StringBuilder msg = new StringBuilder();

    for (File link : linksSymbolic) {

      File realFile = null;
      try {
        // Is a symbolic link
        realFile = Files.readSymbolicLink(link.toPath()).toFile();

        // Remove real file
      } catch (IOException e) {
        // Nothing to do
      }

      if (realFile == null)
        continue;

      if (!realFile.exists()) {

        // TODO
        if (link.delete()) {
          msg.append("\n\tFail to delete broken symbolic link file ");
          msg.append(link.getName());
        }
      }
    }

    return msg.toString();
  }

  /**
   * Compare files.
   * @param comparisonResult the comparison result
   * @param fileExpected the file expected
   * @param fileTested the file tested
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void compareFiles(final ITOutputComparisonResult comparisonResult,
      final File fileExpected, final File fileTested) throws IOException {

    if (this.filesToCheckContent.contains(fileTested)) {
      compareFilesContent(comparisonResult, fileExpected, fileTested);

    } else if (this.filesToCheckLength.contains(fileTested)) {
      compareFilesLength(comparisonResult, fileExpected, fileTested);

    } else if (this.filesToCheckExistence.contains(fileTested)) {
      compareFilesExistence(comparisonResult, fileExpected, fileTested);
    }

  }

  /**
   * Compare content on expected file from tested file with same filename, save
   * result in outputExecution instance.
   * @param comparisonResult outputExecution object
   * @param fileExpected file from expected directory
   * @param fileTested file from tested directory
   * @throws IOException if an error occurs during comparison file
   */
  private void compareFilesContent(
      final ITOutputComparisonResult comparisonResult, final File fileExpected,
      final File fileTested) throws IOException {

    // Comparison two files with same filename
    final FilesComparator fc = new FilesComparator(fileExpected, fileTested);
    // Compare files with comparator
    final boolean res = fc.compare();

    if (!res) {
      comparisonResult.setResult(StatusComparison.NOT_EQUALS, fileExpected,
          fileTested, fc.getDetailComparison());
    } else {

      // Add comparison in the report text
      comparisonResult.setResult(StatusComparison.EQUALS);
    }
  }

  /**
   * Compare length on expected file from tested file with same filename, save
   * result in outputExecution instance.
   * @param comparisonResult outputExecution object
   * @param fileExpected file from expected directory
   * @param fileTested file from tested directory
   */
  private void compareFilesLength(
      final ITOutputComparisonResult comparisonResult, final File fileExpected,
      final File fileTested) {

    // Compare size file
    final long fileExpectedLength = fileExpected.length();
    final long fileTestedLength = fileTested.length();

    final long diffLengthMax =
        (long) (fileExpectedLength * PART_DIFFERENCE_LENGTH_FILE);

    final long diffLength = fileExpectedLength - fileTestedLength;
    final boolean isEqualsLength = diffLength < diffLengthMax;

    String msg = "";

    if (isEqualsLength) {

      // Add comparison in the report text
      comparisonResult.setResult(StatusComparison.EQUALS);

    } else {
      msg = String.format("length expected: %s (%d) vs tested %s (%d)%n",
          fileExpected.getAbsolutePath(), fileExpectedLength,
          fileTested.getAbsolutePath(), fileTestedLength);

      comparisonResult.setResult(StatusComparison.NOT_EQUALS, fileExpected,
          fileTested, msg);

    }
  }

  /**
   * Compare files existence, fail if the file tested can not be empty.
   * @param comparisonResult outputExecution object
   * @param fileExpected file from expected directory
   * @param fileTested file from tested directory
   */
  private void compareFilesExistence(
      final ITOutputComparisonResult comparisonResult, final File fileExpected,
      final File fileTested) {

    final long fileExpectedLength = fileExpected.length();
    final long fileTestedLength = fileTested.length();

    // Check if file tested not empty or file expected is empty
    if (fileTestedLength > 0 || fileExpectedLength == 0) {
      // Add comparison in the report text
      comparisonResult.setResult(StatusComparison.EQUALS);
    } else {

      comparisonResult.setResult(StatusComparison.NOT_EQUALS, fileExpected,
          fileTested, "file tested can not be empty.");
    }

  }

  /**
   * Check if can be find files matching to patterns setting to absence file
   * expected after run test.
   * @return comparisons result, one per comparison file realized
   * @throws IOException if an error occurs when list file matched to patterns.
   */
  private Set<ITOutputComparisonResult> checkAbsenceFileFromPatterns()
      throws IOException {

    final Set<ITOutputComparisonResult> results = new HashSet<>();

    for (final File f : this.filesToCheckAbsence) {

      final ITOutputComparisonResult ocr =
          new ITOutputComparisonResult(f.getName(), StatusComparison.UNEXPECTED,
              "Unexpected file in output test directory matched to patterns "
                  + this.checkAbsenceFilePatterns);

      // Compile result comparison
      results.add(ocr);
    }

    return results;
  }

  /**
   * Listing recursively all files in the source directory which match with
   * patterns files
   * @param patternKey the pattern key
   * @param defaultAllPath the default all path
   * @return the list with all files which match with pattern
   * @throws IOException if an error occurs while parsing input directory
   */
  private List<File> collectFilesFromPattern(final String patternKey,
      final boolean defaultAllPath) throws IOException {

    final Set<PathMatcher> fileMatcher =
        createPathMatchers(patternKey, defaultAllPath);

    final List<File> files = listingFilesFromPatterns(fileMatcher);

    // Remove exclude files if exists
    if (!(this.filesToExclude == null || this.filesToExclude.isEmpty())) {
      files.removeAll(this.filesToExclude);
    }

    if (files.isEmpty()) {
      return Collections.emptyList();
    }
    // Return unmodifiable list
    return Collections.unmodifiableList(files);
  }

  /**
   * Select files from pattern.
   * @param patternKey the pattern key
   * @return the list
   * @throws IOException if an error occurs while parsing input directory
   */
  private List<File> collectFilesFromPattern(final String patternKey)
      throws IOException {

    return collectFilesFromPattern(patternKey, false);
  }

  /**
   * Create list files matching to the patterns
   * @param patterns set of pattern to filter file in result directory
   * @return unmodifiable list of files or empty list
   * @throws IOException if an error occurs while listing files
   */
  private List<File> listingFilesFromPatterns(final Set<PathMatcher> patterns)
      throws IOException {

    final List<File> matchedFiles = new ArrayList<>();

    for (final PathMatcher matcher : patterns) {

      Files.walkFileTree(Paths.get(this.directory.toURI()),
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file,
                final BasicFileAttributes attrs) throws IOException {

              if (matcher.matches(file)) {
                matchedFiles.add(file.toFile());
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(final Path file,
                final IOException exc) throws IOException {
              return FileVisitResult.CONTINUE;
            }
          });
    }

    // No file found
    if (matchedFiles.isEmpty()) {
      // return empty list
      return Collections.emptyList();
    }

    return matchedFiles;
  }

  /**
   * Build collection of PathMatcher for selection files to tread according to a
   * pattern file define in test configuration. Patterns set in string with
   * space to separator. Get input and output patterns files.
   * @param patterns sequences of patterns filesList.
   * @param defaultAllPath if true and patterns empty use default pattern
   *          otherwise empty collection
   * @return collection of PathMatcher, one per pattern. Can be empty if no
   *         pattern defined and exclude use default patterns.
   */
  private static Set<PathMatcher> createPathMatchers(final String patterns,
      final boolean defaultAllPath) {

    // No pattern defined
    if (patterns == null || patterns.trim().isEmpty()) {

      if (defaultAllPath) {
        return Collections.singleton(ALL_PATH_MATCHER);
      }
      return Collections.emptySet();
    }

    // Init collection
    final Set<PathMatcher> result = new HashSet<>();

    // Parse patterns
    for (final String globSyntax : SPACE_SPLITTER.split(patterns)) {

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
   * Gets the directory.
   * @return the directory
   */
  private File getDirectory() {
    return this.directory;
  }

  /**
   * Gets the files to check length.
   * @return the files to check length
   */
  public List<File> getFilesToCheckLength() {
    return Collections.unmodifiableList(this.filesToCheckLength);
  }

  public List<File> getFilesToCheckContent() {
    return Collections.unmodifiableList(this.filesToCheckContent);
  }

  /**
   * Gets the files to check absence.
   * @return the files to check absence
   */
  public List<File> getFilesToCheckAbsence() {
    return Collections.unmodifiableList(this.filesToCheckAbsence);
  }

  /**
   * Gets the files to check existence.
   * @return the files to check existence
   */
  public List<File> getFilesToCheckExistence() {
    return Collections.unmodifiableList(this.filesToCheckExistence);
  }

  /**
   * Gets the files to compare.
   * @return the files to compare
   */
  public List<File> getFilesToCompare() {
    return Collections.unmodifiableList(this.filesToCompare);
  }

  /**
   * Gets the files to remove.
   * @return the files to remove
   */
  public List<File> getFilesToRemove() {
    return Collections.unmodifiableList(this.filesToRemove);
  }

  /**
   * Gets the count files to check content.
   * @return the count files to check content
   */
  public int getCountFilesToCheckContent() {
    return this.filesToCheckContent.size();
  }

  /**
   * Gets the count files to check length.
   * @return the count files to check length
   */
  public int getCountFilesToCheckLength() {
    return this.filesToCheckLength.size();
  }

  /**
   * Gets the count files to check existence.
   * @return the count files to check existence
   */
  public int getCountFilesToCheckExistence() {
    return this.filesToCheckExistence.size();
  }

  /**
   * Gets the count files to check absence.
   * @return the count files to check absence
   */
  public int getCountFilesToCheckAbsence() {
    return this.filesToCheckAbsence.size();
  }

  /**
   * Gets the count files to compare.
   * @return the count files to compare
   */
  public int getCountFilesToCompare() {
    return this.filesToCompare.size();
  }

  /**
   * Gets the count files to remove.
   * @return the count files to remove
   */
  public int getCountFilesToRemove() {
    return this.filesToRemove.size();
  }

  //
  // Constructor
  //

  /**
   * Public constructor, it build list patterns and create list files from the
   * source directory.
   * @param outputTestDirectory source directory
   * @param fileToComparePatterns sequences of patterns, separated by a space
   * @param excludeToComparePatterns sequences of patterns, separated by a space
   * @param checkLengthFilePatterns the check length file patterns
   * @param checkExistenceFilePatterns sequences of patterns, separated by a
   *          space
   * @param checkAbsenceFilePatterns sequences of patterns, separated by a space
   * @param fileToRemovePatterns the file to remove patterns, separated by a
   *          space
   * @throws IOException if an error occurs while parsing input directory
   * @throws EoulsanException the Eoulsan exception
   */
  public ITOutput(final File outputTestDirectory,
      final String fileToComparePatterns, final String excludeToComparePatterns,
      final String checkLengthFilePatterns,
      final String checkExistenceFilePatterns,

      final String checkAbsenceFilePatterns, final String fileToRemovePatterns)
      throws IOException, EoulsanException {

    this.directory = outputTestDirectory;
    this.fileToComparePatterns = fileToComparePatterns;
    this.fileToRemovePatterns = fileToRemovePatterns;
    this.excludeToComparePatterns = excludeToComparePatterns;
    this.checkLengthFilePatterns = checkLengthFilePatterns;
    this.checkExistenceFilePatterns = checkExistenceFilePatterns;
    this.checkAbsenceFilePatterns = checkAbsenceFilePatterns;

    // Compile all files must be compare from patterns
    this.filesToCompare = new ArrayList<>();

    // Set specific list, file not use to compare
    this.filesToExclude =
        collectFilesFromPattern(this.excludeToComparePatterns);

    // Set specific list, file compare content, compile in fileToCompare
    this.filesToCheckContent = collectFilesFromPattern(
        this.fileToComparePatterns, USE_DEFAULT_PATTERN);

    // Set specific list, file exist and is not empty, compile in fileToCompare
    this.filesToCheckExistence =
        collectFilesFromPattern(this.checkExistenceFilePatterns);

    // Set specific list, file compare length, compile in fileToCompare
    this.filesToCheckLength =
        collectFilesFromPattern(this.checkLengthFilePatterns);

    this.filesToCheckAbsence =
        collectFilesFromPattern(this.checkAbsenceFilePatterns);

    this.filesToRemove = collectFilesFromPattern(this.fileToRemovePatterns);

    // Compile all file to compare pair to pair
    this.filesToCompare.addAll(this.filesToCheckContent);
    this.filesToCompare.addAll(this.filesToCheckLength);
    this.filesToCompare.addAll(this.filesToCheckExistence);
  }

  //
  // Internal class
  //

  /**
   * The internal class choice the comparator matching to filename and compare
   * two files.
   * @author Sandrine Perrin
   * @since 2.0
   */
  private static final class FilesComparator {

    private final List<Comparator> comparators = new ArrayList<>();
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
      final boolean b = this.comparator.compareFiles(this.fileA, this.fileB);

      if (!b) {
        this.detailComparison = "fail at comparison #"
            + this.comparator.getNumberElementsCompared() + ":[ "
            + this.comparator.getCauseFailComparison() + "]";
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

      for (final Comparator comp : this.comparators) {

        // Check extension file in list extensions define by comparator
        if (comp.getExtensions().contains(extension)) {
          return comp;
        }
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
      this.comparators.add(new BinaryComparator());

      this.comparators.add(new FastqComparator(USE_SERIALIZATION_FILE));
      this.comparators
          .add(new SAMComparator(USE_SERIALIZATION_FILE, "PG", "HD", "CO"));
      this.comparators
          .add(new BAMComparator(USE_SERIALIZATION_FILE, "PG", "HD", "CO"));
      this.comparators.add(new TextComparator(USE_SERIALIZATION_FILE));
      this.comparators.add(new LogComparator());

      this.comparator = findComparator(this.fileA.getName());

    }
  }

}
