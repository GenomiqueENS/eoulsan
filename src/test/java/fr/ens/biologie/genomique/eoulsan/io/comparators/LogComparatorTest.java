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
package fr.ens.biologie.genomique.eoulsan.io.comparators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class LogComparatorTest {

  final private Comparator comparator = new LogComparator();

  //
  // File expression.log
  //

  private final String expressionLogText =
      "Expression computation with htseq-count (2013_090b, filtered_mapper_results_2.sam, rproc1, exon, ID, stranded: NO, removeAmbiguousCases: false)\n"
          + "\tnumber of alignments with no feature=13868127\n"
          + "\treads eliminated=13868127\n"
          + "\ttotal number of alignments=14526150\n";

  @Test
  public void expressionLogIsSameTest() throws IOException {
    final InputStream isA1 =
        new ByteArrayInputStream(this.expressionLogText.getBytes());

    final InputStream isA2 =
        new ByteArrayInputStream(this.expressionLogText.getBytes());

    assertTrue("files are same", this.comparator.compareFiles(isA1, isA2));

  }

  @Test
  public void expressionLogIsDifferentTest() throws IOException {
    final InputStream isA =
        new ByteArrayInputStream(this.expressionLogText.getBytes());

    String newText = "";
    InputStream isB = null;

    // Change sample name
    newText = this.filterreadsLogText.replaceFirst("2013_090b", "2013_091b");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("filterreadsLog different, change sample name",
        this.comparator.compareFiles(isA, isB));

    // Change first value
    newText = this.expressionLogText.replaceFirst(
        "number of alignments with no feature=13868127",
        "number of alignments with no feature=13865127");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse(
        "expressionLog different change = number of alignments with no feature",
        this.comparator.compareFiles(isA, isB));

    // Change second value,
    newText = this.expressionLogText.replaceFirst("reads eliminated=13868127",
        "reads eliminated=13868126");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("expressionLog different change = reads eliminated",
        this.comparator.compareFiles(isA, isB));

    // Change third value
    newText = this.expressionLogText.replaceFirst(
        "total number of alignments=14526150",
        "total number of alignments=24526150");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("expressionLog different change = total number of alignments",
        this.comparator.compareFiles(isA, isB));
  }

  //
  // File filterreads.log
  //

  private final String filterreadsLogText =
      "Filter reads (2013_090b, 2013_090_GATCAG_L001_R2_001.fastq.bz2)\n"
          + "\tinput raw reads=31125782\n"
          + "\toutput accepted reads=27723097\n"
          + "\treads rejected by filters=3402685\n"
          + "\treads rejected by illuminaid filter=1577651\n"
          + "\treads rejected by quality filter=1818254\n"
          + "\treads rejected by trim filter=6780\n";

  @Test
  public void filterreadsLogIsSameTest() throws IOException {
    final InputStream isA1 =
        new ByteArrayInputStream(this.filterreadsLogText.getBytes());

    final InputStream isA2 =
        new ByteArrayInputStream(this.filterreadsLogText.getBytes());

    assertTrue("files are same", this.comparator.compareFiles(isA1, isA2));
  }

  @Test
  public void filterreadsLogIsDifferentTest() throws IOException {
    final InputStream isA =
        new ByteArrayInputStream(this.filterreadsLogText.getBytes());

    String newText = "";
    InputStream isB = null;

    // Change sample name
    newText = this.filterreadsLogText.replaceFirst("2013_090b", "2013_090a");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("filterreadsLog different, change sample name",
        this.comparator.compareFiles(isA, isB));

    // Change first value
    newText = this.filterreadsLogText.replaceFirst("input raw reads=31125782",
        "input raw reads=31125783");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("filterreadsLog different, change first value",
        this.comparator.compareFiles(isA, isB));

    // Change second value,
    newText = this.filterreadsLogText.replaceFirst(
        "output accepted reads=27723097", "output accepted reads=27723197");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("filterreadsLog different, change second value",
        this.comparator.compareFiles(isA, isB));

    // Change third value
    newText = this.filterreadsLogText.replaceFirst(
        "reads rejected by filters=3402685",
        "reads rejected by filters=3401685");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("filterreadsLog different, change third value",
        this.comparator.compareFiles(isA, isB));

    // Change 4th value
    newText = this.filterreadsLogText.replaceFirst(
        "reads rejected by illuminaid filter=1577651",
        "reads rejected by illuminaid filter=2577651");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("filterreadsLog different, change 4th value",
        this.comparator.compareFiles(isA, isB));

    // Change 5th value
    newText = this.filterreadsLogText.replaceFirst(
        "reads rejected by quality filter=1818254",
        "reads rejected by quality filter=1818253");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("filterreadsLog different, change 5th value",
        this.comparator.compareFiles(isA, isB));

    // Change 6th value
    newText = this.filterreadsLogText.replaceFirst(
        "reads rejected by trim filter=6780",
        "reads rejected by trim filter=678");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("filterreadsLog different, change 6th value",
        this.comparator.compareFiles(isA, isB));
  }

  //
  // File filtersam.log
  //

  private final String filtersamLogText =
      "Filter SAM file (2013_090b, /import/geri01/analysis/adaptanthrop_a2012/expression_exon_id/mapper_results_2.sam)\n"
          + "\talignments rejected by filters=18297031\n"
          + "\talignments rejected by removemultimatches filter=10200168\n"
          + "\talignments rejected by removeunmapped filter=8096863\n"
          + "\tinput alignments=32823181\n"
          + "\toutput filtered alignments=14526150\n";

  @Test
  public void filtersamLogIsSameTest() throws IOException {
    final InputStream isA1 =
        new ByteArrayInputStream(this.filtersamLogText.getBytes());

    final InputStream isA2 =
        new ByteArrayInputStream(this.filtersamLogText.getBytes());

    assertTrue("files are same", this.comparator.compareFiles(isA1, isA2));
  }

  @Test
  public void filtersamLogIsDifferentTest() throws IOException {
    final InputStream isA =
        new ByteArrayInputStream(this.filtersamLogText.getBytes());

    String newText = "";
    InputStream isB = null;

    // Change sample name
    newText = this.filterreadsLogText.replaceFirst("2013_090b", "2013_0090b");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("filterreadsLog different, change sample name",
        this.comparator.compareFiles(isA, isB));

    // Change first value
    newText = this.filtersamLogText.replaceFirst(
        "alignments rejected by filters=18297031",
        "alignments rejected by filters=18297032");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("filtersamLogText different, change first value",
        this.comparator.compareFiles(isA, isB));

    // Change second value
    newText = this.filtersamLogText.replaceFirst(
        "alignments rejected by removemultimatches filter=10200168",
        "alignments rejected by removemultimatches filter=10200167");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("filtersamLogText different, change second value",
        this.comparator.compareFiles(isA, isB));

    // Change third value
    newText = this.filtersamLogText.replaceFirst(
        "alignments rejected by removeunmapped filter=8096863",
        "alignments rejected by removeunmapped filter=80968631");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("filtersamLogText different, change third value",
        this.comparator.compareFiles(isA, isB));

    // Change 4th value
    newText = this.filtersamLogText.replaceFirst("input alignments=32823181",
        "input alignments=32823121");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("filtersamLogText different, change 4th value",
        this.comparator.compareFiles(isA, isB));

    // Change 5th value
    newText = this.filtersamLogText.replaceFirst(
        "output filtered alignments=14526150",
        "output filtered alignments=-4526150");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("filtersamLogText different, change 5th value",
        this.comparator.compareFiles(isA, isB));
  }

  //
  // File mapreads.log
  //

  private final String mapreadsLogText =
      "Mapping reads in fastq-sanger with Bowtie (2013_090b, filtered_reads_2a.fq)\n"
          + "\toutput mapping alignments=32823181\n";

  @Test
  public void mapreadsLogIsSameTest() throws IOException {
    final InputStream isA1 =
        new ByteArrayInputStream(this.mapreadsLogText.getBytes());

    final InputStream isA2 =
        new ByteArrayInputStream(this.mapreadsLogText.getBytes());

    assertTrue("files are same", this.comparator.compareFiles(isA1, isA2));
  }

  @Test
  public void mapreadsLogIsDifferentTest() throws IOException {
    final InputStream isA =
        new ByteArrayInputStream(this.mapreadsLogText.getBytes());

    String newText = "";
    InputStream isB = null;

    // Change sample name
    newText = this.filterreadsLogText.replaceFirst("2013_090b", "2014_090b");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("filterreadsLog different, change sample name",
        this.comparator.compareFiles(isA, isB));

    // Change first value
    newText =
        this.mapreadsLogText.replaceFirst("output mapping alignments=32823181",
            "output mapping alignments=3283181");
    isB = new ByteArrayInputStream(newText.getBytes());

    assertFalse("mapreadsLog different, change first value",
        this.comparator.compareFiles(isA, isB));
  }

}
