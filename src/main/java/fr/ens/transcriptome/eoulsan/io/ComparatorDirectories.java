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

  private boolean useSerialization = false;
  private boolean checkingFilename = false;

  private final Collection<String> allExtensionsTreated = Sets.newHashSet();
  private final Set<CompareFiles> typeComparatorFiles = Sets.newHashSet();

  private final Set<String> filesToNotCompare = Sets.newHashSet();
  private final Multimap<Boolean, ComparatorDirectories.ComparatorPairFile> resultComparaison =
      HashMultimap.create();

  private final StringBuilder report;
  private int numberComparaison = 0;

  /**
   * @param dataSetA
   * @param dataSetB
   * @throws EoulsanException
   * @throws IOException
   */
  public void compareDataSet(final DataSetAnalysis dataSetA,
      final DataSetAnalysis dataSetB) throws EoulsanException, IOException {

    final DataSetAnalysis dataSetExpected;
    final DataSetAnalysis dataSetTested;

    if (dataSetA.isExpected()) {
      dataSetExpected = dataSetA;
      dataSetTested = dataSetB;

    } else if (dataSetB.isExpected()) {
      dataSetExpected = dataSetB;
      dataSetTested = dataSetA;

    } else {
      dataSetExpected = dataSetA;
      dataSetTested = dataSetB;
    }

    parsingDataSet(dataSetExpected, dataSetTested);

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

    LOGGER.info("Start comparison between to result analysis");
    final Stopwatch timer = Stopwatch.createStarted();

    // Build pair files with same names
    for (Map.Entry<String, DataFile> entry : expected.getFileByName()
        .entrySet()) {
      DataFile dfExpected = entry.getValue();

      // Search file with same in test directory
      DataFile dfTested = tested.getFileByName().get(entry.getKey());

      if (isComparable(dfExpected, dfTested)) {

        execute(dfExpected, dfTested);

      } else {
        // None comparison
        LOGGER.info(entry.getKey()
            + " \t " + (dfExpected == null) + " \t " + (dfTested == null)
            + " \tNA");
      }
    }

    // Case file present only in tested directory
    Set<DataFile> filesOnlyInTestDir = Sets.newHashSet(tested.getAllFiles());
    filesOnlyInTestDir.removeAll(expected.getAllFiles());

    if (filesOnlyInTestDir.size() > 0) {
      for (DataFile df : filesOnlyInTestDir) {
        // None comparison
        LOGGER.info(df.getName() + "\t false \t true \tNA");
      }
    }

    timer.stop();
    LOGGER.info("All comparison in  "
        + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

    // TODO
    // bilan comparison
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

    if (isExtensionTreated(dfExpected.getExtension()))
      return false;

    // Check files must be skip
    if (filesToNotCompare.contains(pathFileA)
        || filesToNotCompare.contains(pathFileB))
      return false;

    return true;
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
        this.numberComparaison++;
      }
    } catch (IOException io) {
      LOGGER.severe("Compare pair file fail! " + io.getMessage());
    }
  }

  // ----------------------------------------to delete

  private void collectFiles(final Map<String, ComparatorPairFile> map,
      final Set<DataFile> files) {

    for (DataFile df : files) {
      if (map.containsKey(df.getName())) {
        map.get(df.getName()).setDataFileB(df);
      } else {
        ComparatorPairFile comp = new ComparatorPairFile(df.getName());
        map.put(df.getName(), comp);
        comp.setDataFileA(df);
      }
    }

  }

  private void parsingDataSet_byMap(final DataSetAnalysis expected,
      final DataSetAnalysis tested) throws EoulsanException {

    LOGGER.info("Start comparison between to result analysis");
    final Stopwatch timer = Stopwatch.createStarted();

    Map<String, Collection<DataFile>> mapExpected =
        expected.getAllFilesInAnalysis();
    Map<String, Collection<DataFile>> mapTested =
        tested.getAllFilesInAnalysis();

    // Compare size between map which contains all files include in directory
    if (mapExpected.size() != mapTested.size()) {
      String diffExtension = diffExtension(mapExpected, mapTested);
      loggerReport("Not same count extension, difference is "
          + (mapExpected.size() - mapTested.size()) + " " + diffExtension);
    }

    // Compare per extension type
    for (Map.Entry<String, Collection<DataFile>> entry : mapExpected.entrySet()) {

      String extension = entry.getKey();

      // Check extension include in set extension treated
      if (allExtensionsTreated.contains(extension)) {

        Collection<DataFile> filesExpected = entry.getValue();
        Collection<DataFile> filesTested = mapExpected.get(extension);

        LOGGER.info("Comparison files with extension " + extension);

        // Compare two lists size
        if (filesExpected.size() != filesTested.size())
          loggerReport("Not the same size for list files expected and tested");

        for (DataFile fileExpected : filesExpected) {
          DataFile fileTested = tested.getDataFileSameName(fileExpected);

          if (fileTested == null)
            loggerReport("File "
                + fileExpected.getName() + " is missing in test analysis.");
          else
            // Launch comparison
            execute(fileExpected, fileTested);
        }
      }
    }

    timer.stop();
    loggerReport("All comparison in  "
        + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));
  }

  /**
   * 
   */
  public void computeReport() {

    report.append("\nComparator param: use serialization file "
        + useSerialization);
    report.append("\nComparator type files \n" + typeComparatorFiles);

    report.append("\nnumber pair files compared " + this.numberComparaison);

    report.append("\n\tnumber pair files idem "
        + this.resultComparaison.get(true).size());
    report.append("\n\tnumber pair files different "
        + this.resultComparaison.get(false).size());

    report.append(" detail false :");

    boolean first = true;
    for (ComparatorDirectories.ComparatorPairFile comp : resultComparaison
        .get(false)) {

      if (first) {
        report.append("\ndir "
            + comp.getFileA().getParent() + " vs "
            + comp.getFileB().getParent());
        first = false;
      }
      report.append("\n\t" + comp.toString());
    }
    report.append("\n\t ");

    LOGGER.info("final report \n " + report.toString());

  }

  /**
   * @param first
   * @param second
   * @return
   */
  private String diffExtension(final Map<String, Collection<DataFile>> first,
      final Map<String, Collection<DataFile>> second) {

    StringBuilder s = new StringBuilder();
    s.append("[");

    // Extension only present in first
    Set<String> diffFirst = Sets.newHashSet(first.keySet());
    diffFirst.removeAll(second.keySet());
    s.append(diffFirst.toString());

    s.append(" ; ");

    // Extension only present in first
    Set<String> diffSecond = Sets.newHashSet(second.keySet());
    diffSecond.removeAll(first.keySet());
    s.append(diffSecond.toString());

    s.append("]");

    return s.toString();
  }

  /**
   * Add new entry in logger and save message for final report
   * @param messag
   */
  private void loggerReport(final String messag) {
    LOGGER.warning(messag);

    report.append("\n");
    report.append(messag);
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
        LOGGER.info(dataFileA.getName() + "\t true \t true \t NA");

        return false;
      }

      // Launch compare
      final Stopwatch timer = Stopwatch.createStarted();
      result =
          this.compareFile.compareFiles(getPathFileA(), getPathFileB(),
              useSerialization);

      LOGGER.info(dataFileA.getName()
          + "\t true \t true \t" + result + "\tin "
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
