package fr.ens.biologie.genomique.eoulsan.modules.expression;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.bio.GenomeDescription;
import fr.ens.biologie.genomique.kenetre.bio.expressioncounter.ExpressionCounter;
import fr.ens.biologie.genomique.kenetre.util.ReporterIncrementer;

/**
 * This class define some glue methods between Eoulsan and Kenetre.
 * @since 2.6
 * @author Laurent Jourdren
 */
public class ExpressionCounterUtils {

  /**
   * Initialize the counter.
   * @param counter the counter
   * @param genomeDesc genome description
   * @param annotationFile annotation file
   * @param gtfFormat true if the input format is in GTF format
   * @throws KenetreException if an error occurs while initialize the counter
   * @throws IOException if an error occurs while reading input files
   */
  public static void init(final ExpressionCounter counter,
      final GenomeDescription genomeDesc, final DataFile annotationFile,
      final boolean gtfFormat) throws IOException, KenetreException {

    requireNonNull(counter);
    requireNonNull(annotationFile);

    counter.init(genomeDesc, annotationFile.open(), gtfFormat);
  }

  /**
   * Initialize the counter.
   * @param counter the counter
   * @param genomeDescFile genome description file
   * @param annotationFile annotation file
   * @param gtfFormat true if the input format is in GTF format
   * @throws KenetreException if an error occurs while initialize the counter
   * @throws IOException if an error occurs while reading input files
   */
  public static void init(final ExpressionCounter counter,
      final DataFile genomeDescFile, final DataFile annotationFile,
      final boolean gtfFormat) throws IOException, KenetreException {

    requireNonNull(counter);
    requireNonNull(annotationFile);

    counter.init(genomeDescFile.open(), annotationFile.open(), gtfFormat);
  }

  /**
   * Count the the features.
   * @param counter the counter
   * @param samFile SAM file
   * @param reporter the reporter
   * @param counterGroup the counter group of the reporter
   * @return a map with the counts
   * @throws KenetreException if an error occurs while counting
   * @throws IOException if an error occurs while reading the input file
   * @throws KenetreException if an errors occurs while counting
   */
  public static Map<String, Integer> count(final ExpressionCounter counter,
      final DataFile samFile, final ReporterIncrementer reporter,
      final String counterGroup) throws IOException, KenetreException {

    requireNonNull(counter);
    requireNonNull(samFile);

    return counter.count(samFile.open(), reporter, counterGroup);
  }

  //
  // Constructor
  //

  private ExpressionCounterUtils() {
    throw new IllegalStateException();
  }

}
