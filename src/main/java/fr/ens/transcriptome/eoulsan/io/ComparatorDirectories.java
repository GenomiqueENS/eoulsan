package fr.ens.transcriptome.eoulsan.io;

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlElementDecl.GLOBAL;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.io.FastqCompareFiles;
import fr.ens.transcriptome.eoulsan.bio.io.SAMCompareFiles;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * @author sperrin
 */
public class Comparator {

  /** LOGGER */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private boolean useSerialization = false;
  private final Stopwatch timer = Stopwatch.createUnstarted();

  private final Collection<String> allExtensionsTreated = Sets.newHashSet();
  private final Set<CompareFiles> typeComparatorFiles = Sets.newHashSet();

  private final Multimap<Boolean, Comparator.ComparatorPairFile> resultComparaison =
      HashMultimap.create();

  private int numberComparaison = 0;

  // TODO add in constructor
  public void configure() {
    // TODO
    // retrieve DataGameAnalysis _expected
    // path directory expected
  }

  public void collect(final String pathFileA, final String pathFileB)
      throws Exception {

    // TODO
    // set fileA from DGA expected, include in directory source
    // set fileB from DGA tested

    // TODO use check...
    if (pathFileA == null
        || pathFileA.length() == 0 || pathFileB == null
        || pathFileB.length() == 0) {
      LOGGER.severe("Pathname(s) file invalid");
      throw new Exception("Pathname file invalid");
    }

    ComparatorPairFile comparePairFile =
        new ComparatorPairFile(pathFileA, pathFileB, this.useSerialization);

    try {
      // Check if extension file included in type list
      if (comparePairFile.isComparable()) {

        LOGGER.info("Add comparaison fileA "
            + pathFileA + " with fileB " + pathFileB);

        this.resultComparaison.put(comparePairFile.compare(), comparePairFile);
        this.numberComparaison++;
      }
    } catch (IOException io) {
      LOGGER.severe("Compare pair file fail! " + io.getMessage());
    }
  }

  public void execute() {

    timer.stop();
    buildFinalReport();
  }

  public String buildFinalReport() {
    final StringBuilder sb = new StringBuilder();

    sb.append("Comparator param: use serialization file " + useSerialization);
    sb.append("\nComparator type files \n" + typeComparatorFiles);

    sb.append("\n number paire files compare " + this.numberComparaison);
    sb.append("\n all comparaison in  "
        + toTimeHumanReadable(this.timer.elapsed(TimeUnit.MILLISECONDS)));

    sb.append("\n number paire files idem "
        + this.resultComparaison.get(true).size());

    sb.append("\n number paire files different "
        + this.resultComparaison.get(false).size());
    sb.append(" detail false :");

    boolean first = true;

    // for (Comparator.ComparatorPairFile comp : resultComparaison.get(false)) {
    // if (first) {
    // sb.append("\ndir "
    // + comp.getFileA().getParent() + " vs "
    // + comp.getFileB().getParent());
    // first = false;
    // }
    // sb.append("\n\t" + comp.toString());
    // // TODO for debug
    // sb.append("\ndiff " + comp.getPathFileA() + " " + comp.getPathFileB());
    // }
    sb.append("\n\t ");

    LOGGER.info("final report \n " + sb.toString());

    return sb.toString();
  }

  public boolean isExtensionTreated(final String ext) {
    return allExtensionsTreated.contains(ext);
  }

  //
  // Getter
  //

  //
  // Constructor
  //

  public Comparator(final boolean useSerialization,
      final String directorySourcePath, final boolean checkingFilename,
      final boolean initLogger) {

  }

  /**
   * @param useSerialization use bloom filter serialized on one file
   */
  public Comparator(final boolean useSerialization) {

    this.useSerialization = useSerialization;

    LOGGER.config("Comparator param: use serialization file "
        + useSerialization);

    // Build map type files can been compare
    typeComparatorFiles.add(new FastqCompareFiles());
    typeComparatorFiles.add(new SAMCompareFiles());
    typeComparatorFiles.add(new TextCompareFiles());

    for (CompareFiles comp : typeComparatorFiles) {
      allExtensionsTreated.addAll(comp.getExtensionReaded());
    }

    LOGGER.config("Comparator type files \n" + typeComparatorFiles);

    this.timer.start();

  }

  //
  // Internal class
  //

  class ComparatorPairFile {

    private final String pathFileA;
    private final String pathFileB;
    private final String filenameA;
    private final String filenameB;

    private CompareFiles compareFile = null;
    private final boolean useSerialization;

    private boolean result;

    public boolean compare() throws IOException {

      // Extension file not recognize in any comparator file
      if (!isComparable())
        return false;

      final Stopwatch timer = Stopwatch.createStarted();
      result =
          this.compareFile.compareFiles(pathFileA, pathFileB,
              this.useSerialization);

      LOGGER
          .info(toString()
              + "\tin "
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
      return this.pathFileA;
    }

    public String getPathFileB() {
      return this.pathFileB;
    }

    public File getFileA() {
      return new File(this.pathFileA);
    }

    public File getFileB() {
      return new File(this.pathFileB);
    }

    @Override
    public String toString() {
      return "result: "
          + result + " " + compareFile.getName() + "\t" + this.filenameA + " ("
          + getLengthFileA() + ") " + " vs \t" + this.filenameB + " ("
          + getLengthFileB() + ") ";
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

    public ComparatorPairFile(final String pathFileA, final String pathFileB,
        final boolean useSerialization) {

      this.pathFileA = pathFileA;
      this.pathFileB = pathFileB;
      this.useSerialization = useSerialization;

      this.filenameA = new File(pathFileA).getName();
      this.filenameB = new File(pathFileA).getName();

      // if (!filename.equals(new File(pathFileB).getName()))
      // throw new Exception("FileA are not the same name.");

      final String extensionFile =
          StringUtils.extension(StringUtils
              .filenameWithoutCompressionExtension(filenameA));

      // Set Comparator file according to extension file
      for (CompareFiles compareFiles : typeComparatorFiles) {

        if (compareFiles.getExtensionReaded().contains(extensionFile))
          this.compareFile = compareFiles;
      }

    }
  }
}
