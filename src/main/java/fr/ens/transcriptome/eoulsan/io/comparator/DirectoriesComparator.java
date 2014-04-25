package fr.ens.transcriptome.eoulsan.io.comparator;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.io.Files.newWriter;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanITRuntimeException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.it.DataSetAnalysis;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class DirectoriesComparator {

  private static final Splitter SPLITTER = Splitter.on(',').trimResults()
      .omitEmptyStrings();

  private boolean useSerialization = false;

  private final Collection<String> allExtensionsTreated = Lists.newArrayList();
  private final Set<Comparator> comparatorsFiles = Sets.newHashSet();
  private final Map<Pattern, Comparator> pairPatternCompareFiles = newHashMap();

  private int comparisonSuccesCount = 0;
  private int comparisonFailCount = 0;
  private int filesTreatedCount = 0;
  private int filesComparables = 0;
  private int filesExistsExpectedDirCount = 0;
  private int filesExistsTestedDirCount = 0;
  private String testName;
  private File outputDirectory;

  private StringBuilder reportText = new StringBuilder();

  private boolean asRegression = false;

  private void buildReport() throws IOException {

    this.comparisonFailCount =
        this.filesComparables - this.comparisonSuccesCount;

    boolean allComparisonsSuccessed = this.comparisonFailCount == 0;

    reportText.append("\nfile(s) treated: "
        + ": " + filesComparables + " file(s) comparable(s) on "
        + filesTreatedCount + ":\t" + this.comparisonSuccesCount + " True \t"
        + this.comparisonFailCount + " False");

    reportText.append("\n"
        + this.comparisonFailCount + " comparison(s) failed on "
        + filesComparables + "; " + filesExistsExpectedDirCount
        + " file(s) missing in directory ; " + filesExistsTestedDirCount
        + " file(s) too many in directory.");

    if (allComparisonsSuccessed) {
      reportText.append("\nFor test " + testName + ": no regression detected");

    } else {
      reportText.append("\nFor test " + testName + ": regression detected");
      asRegression = true;
    }

    final String fileName = (asRegression() ? "FAIL" : "SUCCESS");

    // Build report file
    final File reportFile = new File(this.outputDirectory, fileName);

    final Writer fw =
        newWriter(reportFile, Charset.forName(Globals.DEFAULT_FILE_ENCODING));

    fw.write(reportText.toString());
    fw.write("\n");

    fw.flush();
    fw.close();
  }

  /**
   * Parse DataSetAnalysis corresponding to directory result Eoulsan and launch
   * comparison on each pair files.
   * @param expected DatasetAnalysis represents source directory analysis
   * @param tested DatasetAnalysis represents test directory analysis
   * @throws IOException
   */
  public void compareDataSet(final DataSetAnalysis dataSetExpected,
      final DataSetAnalysis dataSetTested, final String reportTestAnalysis)
      throws EoulsanException, IOException {

    final Stopwatch timer = Stopwatch.createStarted();

    this.reportText.append(reportTestAnalysis);

    // Map associates filename and path
    Map<String, File> filesOnlyInTestDir =
        Maps.newHashMap(dataSetTested.getOutputFilesAnalysis());
    Comparator compareFileUsed = null;

    // Build pair files with same names
    for (Map.Entry<String, File> entry : dataSetExpected
        .getOutputFilesAnalysis().entrySet()) {
      filesTreatedCount++;

      File dfExpected = entry.getValue();
      // Search file with same in test directory
      File dfTested = dataSetTested.searchFileByName(entry.getKey());

      // Identify compare to use
      compareFileUsed = identifyCompareFile(entry.getKey());

      if (compareFileUsed != null) {
        filesComparables++;
        execute(dfExpected, dfTested, compareFileUsed);

      } else {
        // None comparison
        reportText.append("\n\ttrue"
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

      for (Map.Entry<String, File> entry : filesOnlyInTestDir.entrySet()) {
        filesTreatedCount++;
        filesExistsTestedDirCount++;
        // None comparison
        reportText.append("\n\tfalse\ttrue\tNA\t" + entry.getKey());
      }
    }
    timer.stop();
    reportText.append("\nAll comparison in  "
        + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

    buildReport();
  }

  private Comparator identifyCompareFile(final String filename) {

    // Parse pattern
    for (Map.Entry<Pattern, Comparator> entry : this.pairPatternCompareFiles
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
      final Comparator compareFileNeeded) throws EoulsanException,
      EoulsanITRuntimeException {

    try {

      ComparatorPairFiles comparePairFile =
          new ComparatorPairFiles(fileA, fileB, compareFileNeeded);

      if (comparePairFile.compare()) {
        this.comparisonSuccesCount++;
      } else {
        final String msg =
            "Comparison failed for test "
                + testName + "\n\t file " + fileB.getAbsolutePath();
        this.reportText.append("\n" + msg);

        buildReport();
        throw new EoulsanITRuntimeException(msg);
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
          for (Comparator compareFiles : comparatorsFiles) {

            if (compareFiles.getExtensions().contains(extension))
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
  public DirectoriesComparator(final File outputDir, final String testName,
      final boolean useSerialization) {

    this.outputDirectory = outputDir;
    this.useSerialization = useSerialization;
    this.testName = testName;

    // Build map type files can been compare
    comparatorsFiles.add(new FastqComparator());
    comparatorsFiles.add(new SAMComparator("PG"));
    comparatorsFiles.add(new TextComparator());
    comparatorsFiles.add(new LogComparator());

    for (Comparator comp : comparatorsFiles) {
      allExtensionsTreated.addAll(comp.getExtensions());
    }

  }

  //
  // Internal class
  //

  class ComparatorPairFiles {

    private final File fileExpected;
    private final File fileTested;

    private Comparator comparatorFiles = null;
    // private final boolean useSerialization;

    private boolean result;

    public boolean compare() throws IOException, EoulsanException {

      // Launch compare
      final Stopwatch timer = Stopwatch.createStarted();
      result =
          this.comparatorFiles.compareFiles(fileExpected, fileTested,
              useSerialization);

      String msg =
          "true\ttrue\t"
              + result + "\t" + fileExpected.getName() + "\tin "
              + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS));

      if (result) {
        reportText.append("\n\t" + msg);
      } else {
        reportText.append("\n\t" + msg);
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
          + result + " " + comparatorFiles.getName() + "\t"
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

    public ComparatorPairFiles(final File fileA, final File fileB,
        final Comparator comparator) throws EoulsanException, IOException {

      checkExistingFile(fileA, " fileA doesn't exists for comparison ");
      checkExistingFile(fileB, " fileB doesn't exists for comparison ");

      if (comparator == null)
        throw new EoulsanException("For comparison, no comparator file define.");

      this.fileExpected = fileA;
      this.fileTested = fileB;
      this.comparatorFiles = comparator;
    }
  }
}
