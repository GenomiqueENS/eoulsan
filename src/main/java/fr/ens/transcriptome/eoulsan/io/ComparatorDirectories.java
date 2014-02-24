package fr.ens.transcriptome.eoulsan.io;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.io.FastqCompareFiles;
import fr.ens.transcriptome.eoulsan.bio.io.SAMCompareFiles;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataSetAnalysis;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * @author sperrin
 */
public class ComparatorDirectories {

  /** LOGGER */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final StringBuilder report;
  private int failComparisonCountGlobal = 0;
  private int failComparisonCountPerProject = 0;

  private String currentProject = "";
  private boolean firstProject = true;

  private boolean useSerialization = false;
  private boolean checkingFilename = false;

  private final Collection<String> allExtensionsTreated = Sets.newHashSet();
  private final Set<CompareFiles> typeComparatorFiles = Sets.newHashSet();

  private final Set<String> filesToNotCompare = Sets.newHashSet();
  private final Multimap<Boolean, ComparatorDirectories.ComparatorPairFile> resultComparaison =
      HashMultimap.create();

  /**
   * @param dataSetA
   * @param dataSetB
   * @throws EoulsanException
   * @throws IOException
   */
  public void compareDataSet(final DataSetAnalysis dataSetA,
      final DataSetAnalysis dataSetB, final String projectName,
      final String testName) throws EoulsanException, IOException {

    LOGGER.info(projectName
        + ": start comparison between to result analysis for " + testName);

    final DataSetAnalysis dataSetExpected;
    final DataSetAnalysis dataSetTested;

    if (dataSetA.exists()) {
      dataSetExpected = dataSetA;
      dataSetTested = dataSetB;

    } else if (dataSetB.exists()) {
      dataSetExpected = dataSetB;
      dataSetTested = dataSetA;

    } else {
      dataSetExpected = dataSetA;
      dataSetTested = dataSetB;
    }

    parsingDataSet(dataSetExpected, dataSetTested);

    // report comparison
    buildReport(projectName, testName);
  }

  private void buildReport(final String projectName, final String testName) {

    failComparisonCountGlobal += this.resultComparaison.get(false).size();

    if (projectName.equals(currentProject))
      failComparisonCountPerProject += this.resultComparaison.get(false).size();

    else {

      if (firstProject) {
        firstProject = false;
      } else {

        // Synthesis project
        if (failComparisonCountPerProject == 0)
          report
              .append("\n\tno regression found for project " + currentProject);
        else
          report.append("\n\t"
              + failComparisonCountPerProject
              + " comparison(s) are failed, check regression in code Eoulsan.");
      }

      // New Project
      failComparisonCountPerProject = this.resultComparaison.get(false).size();
      currentProject = projectName;

      report.append("\n\nComparison for project " + projectName);
    }

    report.append("\n\tresult for test "
        + testName + ": " + this.resultComparaison.get(true).size() + " T \t"
        + this.resultComparaison.get(false).size() + " F");

    // Clean the map stored result comparison for a project
    clear();

    // report.append("\n\tnumber pair files idem "
    // + this.resultComparaison.get(true).size());
    // report.append("\n\tnumber pair files different "
    // + this.resultComparaison.get(false).size());

    // if (this.resultComparaison.get(false).size() == 0)
    // report.append("No regression identified for " + projectName + "");

    // report.append(" detail false :");
    //
    // boolean first = true;
    // for (ComparatorDirectories.ComparatorPairFile comp : resultComparaison
    // .get(false)) {
    //
    // if (first) {
    // report.append("\ndir "
    // + comp.getFileA().getParent() + " vs "
    // + comp.getFileB().getParent());
    // first = false;
    // }
    //
    // report.append("\n\t" + comp.toString());
    // // TODO for debug
    // report
    // .append("\ndiff " + comp.getPathFileA() + " " + comp.getPathFileB());
    // }
    // report.append("\n\t ");
    //
  }

  public String getReport() {

    // Synthesis project
    if (failComparisonCountPerProject == 0)
      report.append("\n\tno regression found for project " + currentProject);
    else
      report.append("\n\t"
          + failComparisonCountPerProject
          + " comparison(s) are failed, check regression in code Eoulsan.");

    if (failComparisonCountGlobal == 0)
      report.append("\nNo regression found for all projects.");
    else
      report.append("\n"
          + failComparisonCountGlobal
          + " comparison(s) are failed, check regression in code Eoulsan.");

    return this.report.toString();
  }

  /**
   * Parse DataSetAnalysis corresponding to directory result Eoulsan and launch
   * comparison on each pair files.
   * @param expected DatasetAnalysis represents source directory analysis
   * @param tested DatasetAnalysis represents test directory analysis
   * @throws IOException
   */
  private void parsingDataSet(final DataSetAnalysis expected,
      final DataSetAnalysis tested) throws EoulsanException, IOException {

    final Stopwatch timer = Stopwatch.createStarted();

    Map<String, DataFile> filesOnlyInTestDir =
        Maps.newHashMap(tested.getFilesByName());

    // Build pair files with same names
    for (Map.Entry<String, DataFile> entry : expected.getFilesByName()
        .entrySet()) {
      DataFile dfExpected = entry.getValue();

      // Search file with same in test directory
      DataFile dfTested = tested.getFilesByName().get(entry.getKey());

      if (isComparable(dfExpected, dfTested)) {

        execute(dfExpected, dfTested);

      } else {
        // None comparison
        LOGGER.warning("true"
            + "\t" + (dfTested != null) + "\tNA\t" + entry.getKey());
      }

      //
      filesOnlyInTestDir.remove(entry.getKey());
    }

    // Case file present only in tested directory
    // TODO
    System.out.println("only in test dir "
        + Joiner.on("\n").withKeyValueSeparator("\t").join(filesOnlyInTestDir));

    if (filesOnlyInTestDir.size() > 0) {
      for (Map.Entry<String, DataFile> entry : filesOnlyInTestDir.entrySet()) {
        // None comparison
        LOGGER.warning("false\ttrue\tNA\t" + entry.getKey());
      }
    }

    timer.stop();
    LOGGER.info("All comparison in  "
        + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

  }

  private boolean isComparable(final DataFile dfExpected,
      final DataFile dfTested) {

    if (dfExpected == null || dfTested == null)
      return false;

    String pathFileA = dfExpected.toFile().getAbsolutePath();
    String pathFileB = dfTested.toFile().getAbsolutePath();

    checkNotNull(pathFileA, "File " + pathFileA + "doesn't exist");
    checkNotNull(pathFileB, "File " + pathFileB + "doesn't exist");

    if (!dfExpected.getExtension().equals(dfTested.getExtension()))
      return false;

    if (!isExtensionTreated(dfExpected.getExtension()))
      return false;

    // Check files must be skip
    if (filesToNotCompare.contains(pathFileA)
        || filesToNotCompare.contains(pathFileB))
      return false;

    return true;
  }

  private void clear() {
    resultComparaison.clear();
  }

  /**
   * Compare two files with corresponding CompareFiles from extension files.
   * @param pathFileA first file to compare, it is the reference
   * @param pathFileB second file to compare
   * @throws EoulsanException it occurs if comparison fails
   */
  public void execute(final DataFile fileA, final DataFile fileB)
      throws EoulsanException {

    ComparatorPairFile comparePairFile = new ComparatorPairFile(fileA, fileB);

    try {
      // Check if extension file included in type list
      if (comparePairFile.isComparable()) {

        boolean result = comparePairFile.compare();
        this.resultComparaison.put(result, comparePairFile);
      }
    } catch (IOException io) {
      LOGGER.severe("Compare pair file fail! " + io.getMessage());
    }
  }

  public boolean isExtensionTreated(final String ext) {
    return allExtensionsTreated.contains(ext);
  }

  //
  // Getter
  //
  public void setFilesToNotCompare(final String file) {
    filesToNotCompare.add(file);
  }

  public void setFilesToNotCompare(final Collection<String> files) {
    filesToNotCompare.addAll(files);
  }

  public void setFilesToNotCompare(final String... files) {
    for (String file : files)
      filesToNotCompare.add(file);
  }

  //
  // Constructor
  //

  /**
   * @param useSerialization use bloom filter serialized on one file
   * @throws EoulsanException
   */
  public ComparatorDirectories(final boolean useSerialization,
      final boolean checkingFilename) throws EoulsanException {

    this.useSerialization = useSerialization;
    this.checkingFilename = checkingFilename;
    this.report = new StringBuilder();

    // Build map type files can been compare
    typeComparatorFiles.add(new FastqCompareFiles());
    typeComparatorFiles.add(new SAMCompareFiles("PG"));
    typeComparatorFiles.add(new TextCompareFiles());
    typeComparatorFiles.add(new LogCompareFiles());

    for (CompareFiles comp : typeComparatorFiles) {
      allExtensionsTreated.addAll(comp.getExtensionReaded());
    }

    LOGGER.config("Comparator param: use serialization file "
        + useSerialization);
    LOGGER.config("Comparator type files \n"
        + Joiner.on("\n\t").join(typeComparatorFiles));

  }

  //
  // Internal class
  //

  class ComparatorPairFile {

    private DataFile dataFileA;
    private DataFile dataFileB;

    private CompareFiles compareFile = null;
    // private final boolean useSerialization;

    private boolean result;

    public boolean compare() throws IOException, EoulsanException {

      // Extension file not recognize in any comparator file
      if (!isComparable()) {
        LOGGER.warning("true \t true \t NA\t" + dataFileA.getName());

        return false;
      }

      // Launch compare
      final Stopwatch timer = Stopwatch.createStarted();
      result =
          this.compareFile.compareFiles(getPathFileA(), getPathFileB(),
              useSerialization);

      LOGGER.info("true\ttrue\t"
          + result + "\t" + dataFileA.getName() + "\tin "
          + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

      timer.stop();

      return result;
    }

    //
    // Getter
    //

    /**
     * @return True if the pair file can be compared, else false
     */
    public boolean isComparable() {
      return this.compareFile != null;
    }

    public String getPathFileA() {
      return this.dataFileA.toFile().getAbsolutePath();
    }

    public String getPathFileB() {
      return this.dataFileB.toFile().getAbsolutePath();
    }

    public File getFileA() {
      return dataFileA.toFile();
    }

    public File getFileB() {
      return dataFileB.toFile();
    }

    public DataFile getDataFileA() {
      return dataFileA;
    }

    public void setDataFileA(DataFile dataFileA) {
      this.dataFileA = dataFileA;
    }

    public DataFile getDataFileB() {
      return dataFileB;
    }

    public void setDataFileB(DataFile dataFileB) {
      this.dataFileB = dataFileB;
    }

    @Override
    public String toString() {
      return "result: "
          + result + " " + compareFile.getName() + "\t"
          + this.dataFileA.getName() + " (" + getLengthFileA() + ") "
          + " vs \t" + this.dataFileB.getName() + " (" + getLengthFileB()
          + ") ";
    }

    public String getLengthFileA() {
      return StringUtils.sizeToHumanReadable(new File(getPathFileA()).length());
    }

    public String getLengthFileB() {
      return StringUtils.sizeToHumanReadable(new File(getPathFileB()).length());
    }

    //
    // Constructor
    //

    public ComparatorPairFile(final DataFile dataFileA, final DataFile dataFileB)
        throws EoulsanException {

      this.dataFileA = dataFileA;
      this.dataFileB = dataFileB;

      if (checkingFilename) {
        if (!this.dataFileA.getName().equals(this.dataFileB.getName()))
          throw new EoulsanException("Two files have not same filename.");
      }

      final String extensionFile = this.dataFileA.getExtension();

      // Set Comparator file according to extension file
      for (CompareFiles compareFiles : typeComparatorFiles) {

        if (compareFiles.getExtensionReaded().contains(extensionFile))
          this.compareFile = compareFiles;
      }

    }

    public ComparatorPairFile(final String pathFileA, final String pathFileB)
        throws EoulsanException {
      this(new DataFile(pathFileA), new DataFile(pathFileB));
    }

  }
}
