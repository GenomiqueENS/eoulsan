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

package fr.ens.transcriptome.eoulsan.bio.expressioncounters;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
//import org.apache.commons.io;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.steps.expression.HTSeqCount;

/**
 * This class test the class
 * fr.ens.transcriptome.eoulsan.steps.expression.HTSeqCount.java. 
 * @author Claire Wallon
 */
public class HTSeqCountTest {

  @Test
  public void testCountReadsInFeatures() throws EoulsanException, IOException,
      BadBioEntryException {

    InputStream isSE =
        this.getClass().getResourceAsStream("/mapper_results_SE.sam");
    InputStream isPE =
        this.getClass().getResourceAsStream("/mapper_results_PE.sam");

    String line;
    String fields[];
    BufferedReader br;

    File dirData = new File("/home/wallon/Bureau/DATA");
    File dirTest = new File("/home/wallon/Bureau/TEST_HTSEQ/JUNIT");

    File samFileSE = new File(dirTest, "mapper_results_SE.sam");
    File samFilePE = new File(dirTest, "mapper_results_PE.sam");

    File annotationFileSE = new File(dirData, "annotation.gff");
    File outputFileSE_union = new File(dirTest, "SE-union-java");
    File outputFileSE_nonempty = new File(dirTest, "SE-nonempty-java");
    File outputFileSE_strict = new File(dirTest, "SE-strict-java");
    File outputFileSE_union_yes = new File(dirTest, "SE-union-java-yes");
    File outputFileSE_nonempty_yes = new File(dirTest, "SE-nonempty-java-yes");
    File outputFileSE_strict_yes = new File(dirTest, "SE-strict-java-yes");
    File outputFileSE_union_reverse =
        new File(dirTest, "SE-union-java-reverse");
    File outputFileSE_nonempty_reverse =
        new File(dirTest, "SE-nonempty-java-reverse");
    File outputFileSE_strict_reverse =
        new File(dirTest, "SE-strict-java-reverse");

    File annotationFilePE = new File(dirData, "mouse.gff");
    File outputFilePE_union = new File(dirTest, "PE-union-java");
    File outputFilePE_nonempty = new File(dirTest, "PE-nonempty-java");
    File outputFilePE_strict = new File(dirTest, "PE-strict-java");
    File outputFilePE_union_yes = new File(dirTest, "PE-union-java-yes");
    File outputFilePE_nonempty_yes = new File(dirTest, "PE-nonempty-java-yes");
    File outputFilePE_strict_yes = new File(dirTest, "PE-strict-java-yes");
    File outputFilePE_union_reverse =
        new File(dirTest, "PE-union-java-reverse");
    File outputFilePE_nonempty_reverse =
        new File(dirTest, "PE-nonempty-java-reverse");
    File outputFilePE_strict_reverse =
        new File(dirTest, "PE-strict-java-reverse");

    // All results from HTSeqCount are compared with the HTSeq-count (pyhton
    // implementation) results

    // HTSeqCount.countReadsInFeatures(samFileSE, annotationFileSE,
    // outputFileSE_union, "no", "union", "exon", "ID", false, 0, null);
    //
    // br = new BufferedReader(new FileReader(outputFileSE_union));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("exon:ENSMUST00000022242:9")
    // || fields[0].equals("no_feature") || fields[0].equals("not_aligned"))
    // assertEquals(fields[1], "1");
    // else if (fields[0].equals("ambiguous"))
    // assertEquals(fields[1], "3");
    // else if (fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "16");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFileSE, annotationFileSE,
    // outputFileSE_nonempty, "no", "intersection-nonempty", "exon", "ID",
    // false, 0, null);
    //
    // br = new BufferedReader(new FileReader(outputFileSE_nonempty));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("exon:ENSMUST00000022242:9")
    // || fields[0].equals("no_feature") || fields[0].equals("not_aligned"))
    // assertEquals(fields[1], "1");
    // else if (fields[0].equals("ambiguous"))
    // assertEquals(fields[1], "3");
    // else if (fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "16");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFileSE, annotationFileSE,
    // outputFileSE_strict, "no", "intersection-strict", "exon", "ID", false,
    // 0, null);
    //
    // br = new BufferedReader(new FileReader(outputFileSE_strict));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("exon:ENSMUST00000022242:9")
    // || fields[0].equals("ambiguous") || fields[0].equals("not_aligned"))
    // assertEquals(fields[1], "1");
    // else if (fields[0].equals("no_feature"))
    // assertEquals(fields[1], "3");
    // else if (fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "16");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFileSE, annotationFileSE,
    // outputFileSE_union_yes, "yes", "union", "exon", "ID", false, 0, null);
    //
    // br = new BufferedReader(new FileReader(outputFileSE_union_yes));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("ambiguous") || fields[0].equals("not_aligned"))
    // assertEquals(fields[1], "1");
    // else if (fields[0].equals("no_feature"))
    // assertEquals(fields[1], "4");
    // else if (fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "16");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFileSE, annotationFileSE,
    // outputFileSE_nonempty_yes, "yes", "intersection-nonempty", "exon",
    // "ID", false, 0, null);
    //
    // br = new BufferedReader(new FileReader(outputFileSE_nonempty_yes));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("ambiguous") || fields[0].equals("not_aligned"))
    // assertEquals(fields[1], "1");
    // else if (fields[0].equals("no_feature"))
    // assertEquals(fields[1], "4");
    // else if (fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "16");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFileSE, annotationFileSE,
    // outputFileSE_strict_yes, "yes", "intersection-strict", "exon", "ID",
    // false, 0, null);
    //
    // br = new BufferedReader(new FileReader(outputFileSE_strict_yes));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("ambiguous") || fields[0].equals("not_aligned"))
    // assertEquals(fields[1], "1");
    // else if (fields[0].equals("no_feature"))
    // assertEquals(fields[1], "4");
    // else if (fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "16");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFileSE, annotationFileSE,
    // outputFileSE_union_reverse, "reverse", "union", "exon", "ID", false, 0,
    // null);
    //
    // br = new BufferedReader(new FileReader(outputFileSE_union_reverse));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("exon:ENSMUST00000022242:9")
    // || fields[0].equals("not_aligned"))
    // assertEquals(fields[1], "1");
    // else if (fields[0].equals("no_feature") || fields[0].equals("ambiguous"))
    // assertEquals(fields[1], "2");
    // else if (fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "16");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFileSE, annotationFileSE,
    // outputFileSE_nonempty_reverse, "reverse", "intersection-nonempty",
    // "exon", "ID", false, 0, null);
    //
    // br = new BufferedReader(new FileReader(outputFileSE_nonempty_reverse));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("exon:ENSMUST00000022242:9")
    // || fields[0].equals("not_aligned"))
    // assertEquals(fields[1], "1");
    // else if (fields[0].equals("no_feature") || fields[0].equals("ambiguous"))
    // assertEquals(fields[1], "2");
    // else if (fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "16");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFileSE, annotationFileSE,
    // outputFileSE_strict_reverse, "reverse", "intersection-strict", "exon",
    // "ID", false, 0, null);
    //
    // br = new BufferedReader(new FileReader(outputFileSE_strict_reverse));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("exon:ENSMUST00000022242:9")
    // || fields[0].equals("not_aligned"))
    // assertEquals(fields[1], "1");
    // else if (fields[0].equals("no_feature"))
    // assertEquals(fields[1], "4");
    // else if (fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "16");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFilePE, annotationFilePE,
    // outputFilePE_union, "no", "union", "exon", "ID", false, 0, null);
    //
    // br = new BufferedReader(new FileReader(outputFilePE_union));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("exon:ENSMUST00000062356:2")
    // || fields[0].equals("no_feature"))
    // assertEquals(fields[1], "1");
    // else if (fields[0].equals("ambiguous")
    // || fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "7");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFilePE, annotationFilePE,
    // outputFilePE_nonempty, "no", "intersection-nonempty", "exon", "ID",
    // false, 0, null);
    //
    // br = new BufferedReader(new FileReader(outputFilePE_nonempty));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("exon:ENSMUST00000062356:2"))
    // assertEquals(fields[1], "1");
    // else if (fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "7");
    // else if (fields[0].equals("no_feature") || fields[0].equals("ambiguous"))
    // assertEquals(fields[1], "4");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFilePE, annotationFilePE,
    // outputFilePE_strict, "no", "intersection-strict", "exon", "ID", false,
    // 0, null);
    //
    // br = new BufferedReader(new FileReader(outputFilePE_strict));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("exon:ENSMUST00000062356:2")
    // || fields[0].equals("ambiguous"))
    // assertEquals(fields[1], "1");
    // else if (fields[0].equals("no_feature")
    // || fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "7");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFilePE, annotationFilePE,
    // outputFilePE_union_yes, "yes", "union", "exon", "ID", false, 0, null);
    //
    // br = new BufferedReader(new FileReader(outputFilePE_union_yes));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("exon:ENSMUST00000062356:2"))
    // assertEquals(fields[1], "1");
    // else if (fields[0].equals("ambiguous"))
    // assertEquals(fields[1], "3");
    // else if (fields[0].equals("no_feature"))
    // assertEquals(fields[1], "5");
    // else if (fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "7");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFilePE, annotationFilePE,
    // outputFilePE_nonempty_yes, "yes", "intersection-nonempty", "exon",
    // "ID", false, 0, null);
    //
    // br = new BufferedReader(new FileReader(outputFilePE_nonempty_yes));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("exon:ENSMUST00000062356:2"))
    // assertEquals(fields[1], "1");
    // else if (fields[0].equals("ambiguous"))
    // assertEquals(fields[1], "2");
    // else if (fields[0].equals("no_feature"))
    // assertEquals(fields[1], "6");
    // else if (fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "7");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFilePE, annotationFilePE,
    // outputFilePE_strict_yes, "yes", "intersection-strict", "exon", "ID",
    // false, 0, null);
    //
    // br = new BufferedReader(new FileReader(outputFilePE_strict_yes));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("exon:ENSMUST00000062356:2")
    // || fields[0].equals("ambiguous"))
    // assertEquals(fields[1], "1");
    // else if (fields[0].equals("no_feature")
    // || fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "7");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFilePE, annotationFilePE,
    // outputFilePE_union_reverse, "reverse", "union", "exon", "ID", false, 0,
    // null);
    //
    // br = new BufferedReader(new FileReader(outputFilePE_union_reverse));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("ambiguous"))
    // assertEquals(fields[1], "4");
    // else if (fields[0].equals("no_feature"))
    // assertEquals(fields[1], "5");
    // else if (fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "7");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFilePE, annotationFilePE,
    // outputFilePE_nonempty_reverse, "reverse", "intersection-nonempty",
    // "exon", "ID", false, 0, null);
    //
    // br = new BufferedReader(new FileReader(outputFilePE_nonempty_reverse));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("ambiguous"))
    // assertEquals(fields[1], "2");
    // else if (fields[0].equals("no_feature")
    // || fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "7");
    // else
    // assertEquals(fields[1], "0");
    // }
    //
    // HTSeqCount.countReadsInFeatures(samFilePE, annotationFilePE,
    // outputFilePE_strict_reverse, "reverse", "intersection-strict", "exon",
    // "ID", false, 0, null);
    //
    // br = new BufferedReader(new FileReader(outputFilePE_strict_reverse));
    // while ((line = br.readLine()) != null) {
    // fields = line.split("\t");
    // if (fields[0].equals("no_feature"))
    // assertEquals(fields[1], "9");
    // else if (fields[0].equals("alignment_not_unique"))
    // assertEquals(fields[1], "7");
    // else
    // assertEquals(fields[1], "0");
    // }

  }
}
