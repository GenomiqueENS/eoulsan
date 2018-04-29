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

package fr.ens.biologie.genomique.eoulsan.bio.expressioncounters;

import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.HTSeqCounter.ATTRIBUTE_ID_PARAMETER_NAME;
import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.HTSeqCounter.GENOMIC_TYPE_PARAMETER_NAME;
import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.HTSeqCounter.OVERLAP_MODE_PARAMETER_NAME;
import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.HTSeqCounter.REMOVE_AMBIGUOUS_CASES_PARAMETER_NAME;
import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.HTSeqCounter.REMOVE_NON_UNIQUE_ALIGNMENTS_PARAMETER_NAME;
import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.HTSeqCounter.REMOVE_SECONDARY_ALIGNMENTS_PARAMETER_NAME;
import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.HTSeqCounter.STRANDED_PARAMETER_NAME;
import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.OverlapMode.INTERSECTION_NONEMPTY;
import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.OverlapMode.INTERSECTION_STRICT;
import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.OverlapMode.UNION;
import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.StrandUsage.NO;
import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.StrandUsage.REVERSE;
import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.StrandUsage.YES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.BadBioEntryException;
import fr.ens.biologie.genomique.eoulsan.bio.GFFEntry;
import fr.ens.biologie.genomique.eoulsan.bio.GenomeDescription;
import fr.ens.biologie.genomique.eoulsan.bio.io.GTFReader;
import fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounterCounter;
import fr.ens.biologie.genomique.eoulsan.util.LocalReporter;

/**
 * This class test the HTSeqCount class.
 * @author Laurent Jourdren
 */
public class HTSeqCountTest {

  private static final String HTSEQ_RESSOURCE_DIR = "/htseq-count";
  private static final String GTF_RESSOURCE =
      HTSEQ_RESSOURCE_DIR + "/Saccharomyces_cerevisiae.SGD1.01.56.gtf";
  private static final String SAM_RESSOURCE =
      HTSEQ_RESSOURCE_DIR + "/yeast_RNASeq_excerpt_withNH.sam";
  private static final String COUNTER_GROUP = "expression";

  private GenomeDescription genomeDescription;

  @Before
  public void init() throws IOException {

    // Load genome description before the first test
    if (this.genomeDescription == null) {
      this.genomeDescription = loadGenomeDescription();
    }
  }

  @Test
  public void testCountWithNH()
      throws EoulsanException, IOException, BadBioEntryException {

    // htseq-count -m intersection-nonempty --nonunique none
    HTSeqCounter counter = new HTSeqCounter();
    counter.setParameter(OVERLAP_MODE_PARAMETER_NAME,
        INTERSECTION_NONEMPTY.getName());
    counter.setParameter(GENOMIC_TYPE_PARAMETER_NAME, "exon");
    counter.setParameter(ATTRIBUTE_ID_PARAMETER_NAME, "gene_id");
    counter.setParameter(STRANDED_PARAMETER_NAME, YES.getName());

    compareCounts(counter, "/yeast_RNASeq_excerpt_withNH_counts.tsv");
  }

  @Test
  public void testCountWithNHUnion()
      throws EoulsanException, IOException, BadBioEntryException {

    // htseq-count -m intersection-nonempty --nonunique none
    HTSeqCounter counter = new HTSeqCounter();
    counter.setParameter(OVERLAP_MODE_PARAMETER_NAME, UNION.getName());
    counter.setParameter(GENOMIC_TYPE_PARAMETER_NAME, "exon");
    counter.setParameter(ATTRIBUTE_ID_PARAMETER_NAME, "gene_id");
    counter.setParameter(STRANDED_PARAMETER_NAME, YES.getName());

    compareCounts(counter, "/yeast_RNASeq_excerpt_withNH_counts_union.tsv");
  }

  @Test
  public void testCountWithNHIntersectionStrict()
      throws EoulsanException, IOException, BadBioEntryException {

    // htseq-count -m intersection-nonempty --nonunique none
    HTSeqCounter counter = new HTSeqCounter();
    counter.setParameter(OVERLAP_MODE_PARAMETER_NAME,
        INTERSECTION_STRICT.getName());
    counter.setParameter(GENOMIC_TYPE_PARAMETER_NAME, "exon");
    counter.setParameter(ATTRIBUTE_ID_PARAMETER_NAME, "gene_id");
    counter.setParameter(STRANDED_PARAMETER_NAME, YES.getName());

    compareCounts(counter,
        "/yeast_RNASeq_excerpt_withNH_counts_intersection-strict.tsv");
  }

  @Test
  public void testCountWithNHNo()
      throws EoulsanException, IOException, BadBioEntryException {

    // htseq-count -m intersection-nonempty --nonunique none
    HTSeqCounter counter = new HTSeqCounter();
    counter.setParameter(OVERLAP_MODE_PARAMETER_NAME,
        INTERSECTION_NONEMPTY.getName());
    counter.setParameter(GENOMIC_TYPE_PARAMETER_NAME, "exon");
    counter.setParameter(ATTRIBUTE_ID_PARAMETER_NAME, "gene_id");
    counter.setParameter(STRANDED_PARAMETER_NAME, NO.getName());

    compareCounts(counter, "/yeast_RNASeq_excerpt_withNH_counts_no.tsv");
  }

  @Test
  public void testCountWithNHReverse()
      throws EoulsanException, IOException, BadBioEntryException {

    // htseq-count -m intersection-nonempty --nonunique none
    HTSeqCounter counter = new HTSeqCounter();
    counter.setParameter(OVERLAP_MODE_PARAMETER_NAME,
        INTERSECTION_NONEMPTY.getName());
    counter.setParameter(GENOMIC_TYPE_PARAMETER_NAME, "exon");
    counter.setParameter(ATTRIBUTE_ID_PARAMETER_NAME, "gene_id");
    counter.setParameter(STRANDED_PARAMETER_NAME, REVERSE.getName());

    compareCounts(counter, "/yeast_RNASeq_excerpt_withNH_counts_reverse.tsv");
  }

  @Test
  public void testCountNonUnique()
      throws EoulsanException, IOException, BadBioEntryException {

    // htseq-count -m intersection-nonempty --nonunique all
    HTSeqCounter counter = new HTSeqCounter();
    counter.setParameter(OVERLAP_MODE_PARAMETER_NAME,
        INTERSECTION_NONEMPTY.getName());
    counter.setParameter(GENOMIC_TYPE_PARAMETER_NAME, "exon");
    counter.setParameter(ATTRIBUTE_ID_PARAMETER_NAME, "gene_id");
    counter.setParameter(STRANDED_PARAMETER_NAME, StrandUsage.YES.getName());
    counter.setParameter(REMOVE_NON_UNIQUE_ALIGNMENTS_PARAMETER_NAME, "false");
    counter.setParameter(REMOVE_AMBIGUOUS_CASES_PARAMETER_NAME, "false");

    compareCounts(counter, "/yeast_RNASeq_excerpt_withNH_counts_nonunique.tsv");
  }

  @Test
  public void testCountIgnoreSecondary()
      throws EoulsanException, IOException, BadBioEntryException {

    // htseq-count -m intersection-nonempty --nonunique none
    // --secondary-alignments ignore
    HTSeqCounter counter = new HTSeqCounter();
    counter.setParameter(OVERLAP_MODE_PARAMETER_NAME,
        INTERSECTION_NONEMPTY.getName());
    counter.setParameter(GENOMIC_TYPE_PARAMETER_NAME, "exon");
    counter.setParameter(ATTRIBUTE_ID_PARAMETER_NAME, "gene_id");
    counter.setParameter(STRANDED_PARAMETER_NAME, StrandUsage.YES.getName());
    counter.setParameter(REMOVE_SECONDARY_ALIGNMENTS_PARAMETER_NAME, "true");

    compareCounts(counter,
        "/yeast_RNASeq_excerpt_withNH_counts_ignore_secondary.tsv");
  }

  //
  // Utility methods
  //

  /**
   * Compare counts.
   * @param counter counter
   * @param expectedRessource expected counts resources
   * @throws IOException if an error occurs while reading the expected counts
   * @throws EoulsanException if an error occurs while counting
   */
  private void compareCounts(HTSeqCounter counter,
      final String expectedRessource) throws IOException, EoulsanException {

    try (GTFReader reader =
        new GTFReader(this.getClass().getResourceAsStream(GTF_RESSOURCE))) {
      counter.init(this.genomeDescription, reader);
    }

    LocalReporter reporter = new LocalReporter();
    Map<String, Integer> counts = null;
    try (InputStream in = this.getClass().getResourceAsStream(SAM_RESSOURCE)) {
      counts = counter.count(in, reporter, COUNTER_GROUP);
    }

    counter.addZeroCountFeatures(counts);

    compareCounts(counts, reporter, HTSEQ_RESSOURCE_DIR + expectedRessource);
  }

  /**
   * Compare counts with values in a ressource file.
   * @param counts the count to compare
   * @param ressource the ressource with the excepted values
   * @throws IOException if an error occurs while reading the expected values
   */
  private void compareCounts(final Map<String, Integer> counts,
      final LocalReporter reporter, final String ressource) throws IOException {

    assertNotNull(counts);
    assertNotNull(ressource);

    int entryCounts = 0;

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
        this.getClass().getResourceAsStream(ressource)))) {

      String line;
      while ((line = reader.readLine()) != null) {

        line = line.trim();
        if (line.isEmpty()) {
          continue;
        }

        String[] fields = line.split("\t");
        String key = fields[0];
        int value = Integer.parseInt(fields[1]);

        if (line.startsWith("__")) {

          ExpressionCounterCounter c =
              ExpressionCounterCounter.getCounterFromHTSeqCountName(key);

          if (c != null) {
            assertEquals(value == 0 ? -1 : value,
                reporter.getCounterValue(COUNTER_GROUP, c.counterName()));
          }

          continue;
        }

        assertTrue(counts.containsKey(key));
        assertEquals(value, counts.get(key).intValue());

        entryCounts++;
      }
      assertEquals(entryCounts, counts.size());
    }

  }

  /**
   * Load genome description from GTF ressource.
   * @return a GenomeDescription object
   * @throws IOException if an error occurs while reading the ressource
   */
  private GenomeDescription loadGenomeDescription() throws IOException {

    try (GTFReader reader =
        new GTFReader(this.getClass().getResourceAsStream(GTF_RESSOURCE))) {
      return createGenomeDescriptionFromAnnotation(reader);
    }
  }

  /**
   * Create a GenomeDescription object from an annotation stream.
   * @param annotation annotation stream
   * @return a GenomeDescription
   */
  private static GenomeDescription createGenomeDescriptionFromAnnotation(
      Iterable<GFFEntry> annotation) {

    Map<String, Integer> chromosomeSizes = new HashMap<>();

    for (GFFEntry e : annotation) {

      String chromosome = e.getSeqId();
      Integer max = Integer.valueOf(Math.max(e.getStart(), e.getEnd()));

      if (!chromosomeSizes.containsKey(chromosome)) {
        chromosomeSizes.put(chromosome, max);
      } else if (max.compareTo(chromosomeSizes.get(chromosome)) < 0) {
        chromosomeSizes.put(chromosome, max);
      }
    }

    GenomeDescription result = new GenomeDescription();

    for (Map.Entry<String, Integer> e : chromosomeSizes.entrySet()) {

      result.addSequence(e.getKey(), e.getValue());
    }

    return result;
  }

}
