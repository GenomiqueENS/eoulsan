package fr.ens.transcriptome.eoulsan.io;

import static com.google.common.collect.Maps.newHashMap;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.io.FastqCompareFiles;
import fr.ens.transcriptome.eoulsan.bio.io.SAMCompareFiles;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataSetAnalysis;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class ComparatorDirectories {

  /** LOGGER */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);
  private static final Splitter SPLITTER = Splitter.on(',').trimResults()
      .omitEmptyStrings();

  private boolean useSerialization = false;

  private final Collection<String> allExtensionsTreated = Lists.newArrayList();
  private final Set<CompareFiles> typeComparatorFiles = Sets.newHashSet();
  private final Map<Pattern, CompareFiles> pairPatternCompareFiles =
      newHashMap();

  private int comparisonSuccesCount = 0;
  private int comparisonFailCount = 0;
  private int filesTreatedCount = 0;
  private int filesComparables = 0;
  private int filesExistsExpectedDirCount = 0;
  private int filesExistsTestedDirCount = 0;

  private boolean asRegression = false;

  public String buildReport(final boolean isCheckingExistingFiles,
      final String testName) {

    boolean allComparisonsSuccessed = this.comparisonFailCount == 0;

    boolean noDifferentsFilesBetweenDirectories =
        (isCheckingExistingFiles
            ? (filesExistsExpectedDirCount == 0 && filesExistsTestedDirCount == 0)
            : true);

    LOGGER.info("File(s) treated: "
        + ": " + filesComparables + " file(s) comparable(s) on "
        + filesTreatedCount + "\t" + this.comparisonSuccesCount + " True \t"
        + this.comparisonFailCount + " False");

    LOGGER.info("File(s) presents only in expected directory "
        + filesExistsExpectedDirCount);
    LOGGER.info("File(s) presents only in tested directory "
        + filesExistsTestedDirCount);

    String assessment =
        this.comparisonFailCount
            + " comparison(s) failed on " + filesComparables + "; "
            + filesExistsExpectedDirCount + " file(s) missing in directory ; "
            + filesExistsTestedDirCount + " file(s) too many in directory.";

    if (allComparisonsSuccessed && noDifferentsFilesBetweenDirectories) {
      assessment =
          "For test " + testName + ": no regression detected; " + assessment;
      LOGGER.info(assessment);

    } else {
      assessment =
          "For test " + testName + ": regression detected; " + assessment;
      LOGGER.severe(assessment);
      asRegression = true;
    }

    return assessment.toString();
  }

  /**
   * Parse DataSetAnalysis corresponding to directory result Eoulsan and launch
   * comparison on each pair files.
   * @param expected DatasetAnalysis represents source directory analysis
   * @param tested DatasetAnalysis represents test directory analysis
   * @throws IOException
   */
  public void compareDataSet(final DataSetAnalysis dataSetExpected,
      final DataSetAnalysis dataSetTested, final String testName)
      throws EoulsanException, IOException {

    LOGGER.info("Start comparison between to result analysis for " + testName);

    final Stopwatch timer = Stopwatch.createStarted();

    // Map associates filename and path
    Map<String, DataFile> filesOnlyInTestDir =
        Maps.newHashMap(dataSetTested.getAllFilesAnalysis());
    CompareFiles compareFileUsed = null;

    // Build pair files with same names
    for (Map.Entry<String, DataFile> entry : dataSetExpected
        .getAllFilesAnalysis().entrySet()) {
      filesTreatedCount++;

      DataFile dfExpected = entry.getValue();
      // Search file with same in test directory
      DataFile dfTested = dataSetTested.searchFileByName(entry.getKey());

      // Identify compare to use
      compareFileUsed = identifyCompareFile(entry.getKey());

      if (compareFileUsed != null) {
        filesComparables++;
        execute(dfExpected.toFile(), dfTested.toFile(), compareFileUsed);

      } else {
        // None comparison
        LOGGER.fine("true"
            + "\t" + (dfTested != null) + "\tNA\t" + entry.getKey());

        if (dfTested == null) {
          filesExistsExpectedDirCount++;
        }
      }
      // Remove this file in list to test directory
      filesOnlyInTestDir.remove(entry.getKey());
    }

    // Parse files exists only in test directory
    if (filesOnlyInTestDir.size() > 0) {

      for (Map.Entry<String, DataFile> entry : filesOnlyInTestDir.entrySet()) {
        filesTreatedCount++;
        filesExistsTestedDirCount++;
        // None comparison
        LOGGER.fine("false\ttrue\tNA\t" + entry.getKey());
      }
    }
    timer.stop();
    LOGGER.info("All comparison in  "
        + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

  }

  private CompareFiles identifyCompareFile(final String filename) {

    // Parse pattern
    for (Map.Entry<Pattern, CompareFiles> entry : this.pairPatternCompareFiles
        .entrySet()) {
      Pattern pattern = entry.getKey();

      if (pattern.matcher(filename).matches())
        return entry.getValue();
    }

    return null;
  }

  private String extensionFilenameWithCompressionFromPattern(final String regex) {

    // Delete special character in regex
    String filename = regex.replace("$", "");
    filename = filename.replace("\\", "");

    // Extract extension file from regex
    return StringUtils.extension(StringUtils
        .filenameWithoutCompressionExtension(filename));
  }

  /**
   * Compare two files with corresponding CompareFiles from extension files.
   * @param pathFileA first file to compare, it is the reference
   * @param pathFileB second file to compare
   * @throws EoulsanException it occurs if comparison fails
   */
  public void execute(final File fileA, final File fileB,
      final CompareFiles compareFileNeeded) throws EoulsanException {

    try {

      ComparatorPairFile comparePairFile =
          new ComparatorPairFile(fileA, fileB, compareFileNeeded);

      if (comparePairFile.compare()) {
        this.comparisonSuccesCount++;
      } else {
        this.comparisonFailCount++;
      }
    } catch (IOException io) {
      throw new EoulsanException("Compare pair file file " + io.getMessage());
    }

  }

  private boolean isExtensionTreated(final String ext) {
    return allExtensionsTreated.contains(ext);
  }

  public void setPatternToCompare(final String patterns) {

    // Initialization pattern
    List<String> s = SPLITTER.splitToList(patterns);

    if (s.isEmpty())
      return;

    for (String regex : s) {
      Pattern pattern = Pattern.compile(regex);

      // Check patterns already save
      if (!this.pairPatternCompareFiles.containsKey(pattern)) {

        // Retrieve class comparator file corresponds on extension file
        String extension = extensionFilenameWithCompressionFromPattern(regex);

        if (isExtensionTreated(extension)) {
          // Set Comparator file according to extension file
          for (CompareFiles compareFiles : typeComparatorFiles) {

            if (compareFiles.getExtensionReaded().contains(extension))
              this.pairPatternCompareFiles.put(pattern, compareFiles);
          }
        }
      }
    }
  }

  //
  // Getter
  //

  public boolean asRegression() {

    return asRegression;
  }

  //
  // Constructor
  //

  /**
   * @param useSerialization use bloom filter serialized on one file
   * @throws EoulsanException
   */
  public ComparatorDirectories(final boolean useSerialization) {

    this.useSerialization = useSerialization;

    // Build map type files can been compare
    typeComparatorFiles.add(new FastqCompareFiles());
    typeComparatorFiles.add(new SAMCompareFiles("PG"));
    typeComparatorFiles.add(new TextCompareFiles());
    typeComparatorFiles.add(new LogCompareFiles());

    for (CompareFiles comp : typeComparatorFiles) {
      allExtensionsTreated.addAll(comp.getExtensionReaded());
    }

  }

  //
  // Internal class
  //

  class ComparatorPairFile {

    private final File fileExpected;
    private final File fileTested;

    private CompareFiles compareFile = null;
    // private final boolean useSerialization;

    private boolean result;

    public boolean compare() throws IOException, EoulsanException {

      // Launch compare
      final Stopwatch timer = Stopwatch.createStarted();
      result =
          this.compareFile.compareFiles(fileExpected, fileTested,
              useSerialization);

      String msg =
          "true\ttrue\t"
              + result + "\t" + fileExpected.getName() + "\tin "
              + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS));

      if (result) {
        LOGGER.info(msg);
      } else {
        LOGGER.severe(msg);
      }

      timer.stop();

      return result;
    }

    //
    // Getter
    //

    @Override
    public String toString() {
      return "result: "
          + result + " " + compareFile.getName() + "\t"
          + this.fileExpected.getName() + " (" + getLengthFileA() + ") "
          + " vs \t" + this.fileTested.getName() + " (" + getLengthFileB()
          + ") ";
    }

    public String getLengthFileA() {
      return StringUtils.sizeToHumanReadable(this.fileExpected.length());
    }

    public String getLengthFileB() {
      return StringUtils.sizeToHumanReadable(this.fileTested.length());
    }

    //
    // Constructor
    //

    public ComparatorPairFile(final File fileA, final File fileB,
        final CompareFiles compareFile) throws EoulsanException, IOException {

      checkExistingFile(fileA, " fileA doesn't exists for comparison ");
      checkExistingFile(fileB, " fileB doesn't exists for comparison ");

      if (compareFile == null)
        throw new EoulsanException("For comparison, no comparator file define.");

      this.fileExpected = fileA;
      this.fileTested = fileB;
      this.compareFile = compareFile;
    }
  }
}
