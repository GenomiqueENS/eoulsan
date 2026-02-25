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

package fr.ens.biologie.genomique.eoulsan.design.io;

import static org.junit.Assert.assertEquals;

import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.DesignFactory;
import fr.ens.biologie.genomique.eoulsan.design.DesignMetadata;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentMetadata;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentSampleMetadata;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.design.SampleMetadata;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import org.junit.Test;

public class Eoulsan2ReaderWriterTest {

  @Test
  public void test() throws IOException {

    // create a design test
    Design design = DesignFactory.createEmptyDesign();
    DesignMetadata designMetadata = design.getMetadata();

    // design metadata
    designMetadata.setGenomeFile("mm10.fasta");
    designMetadata.setGffFile("mm10.gff");
    designMetadata.setAdditionalAnnotationFile("additional_mm10.txt");

    // create experiments
    design.addExperiment("1");
    design.addExperiment("2");
    Experiment exp1 = design.getExperiment("1");
    Experiment exp2 = design.getExperiment("2");

    // experiment metadata
    exp1.setName("exp1");
    exp2.setName("exp2");

    // experimentMetadata
    ExperimentMetadata exp1MD = exp1.getMetadata();
    ExperimentMetadata exp2MD = exp2.getMetadata();

    exp1MD.setSkip(false);
    exp1MD.setReference("sample1");
    exp1MD.setModel("~type+day+type:day");
    exp2MD.setSkip(false);
    exp2MD.setReference("false");
    exp1MD.setModel("~Condition");

    // add samples
    design.addSample("1");
    design.addSample("2");
    Sample sample1 = design.getSample("1");
    Sample sample2 = design.getSample("2");

    sample1.setName("sample1");
    sample2.setName("sample2");

    // sample metadata
    SampleMetadata sample1MD = sample1.getMetadata();
    SampleMetadata sample2MD = sample2.getMetadata();

    sample1MD.setReads(Collections.singletonList("read_sample1.fasta"));
    sample2MD.setReads(Collections.singletonList("read_sample2.fasta"));

    sample1MD.setDate("06.10.2015");
    sample2MD.setDate("06.10.2015");

    // experiment sample metadata
    exp1.addSample(sample1);
    exp1.addSample(sample2);
    exp2.addSample(sample1);
    exp2.addSample(sample2);

    ExperimentSampleMetadata exp1Sample1MD = exp1.getExperimentSample(sample1).getMetadata();
    ExperimentSampleMetadata exp1Sample2MD = exp1.getExperimentSample(sample2).getMetadata();
    ExperimentSampleMetadata exp2Sample1MD = exp2.getExperimentSample(sample1).getMetadata();
    ExperimentSampleMetadata exp2Sample2MD = exp2.getExperimentSample(sample2).getMetadata();

    exp1Sample1MD.set("type", "WT");
    exp1Sample2MD.set("type", "KO");
    exp1Sample1MD.set("day", "1");
    exp1Sample2MD.set("day", "2");
    exp2Sample1MD.setCondition("non-treated");
    exp2Sample2MD.setCondition("treated");

    // Write design

    File outFile = File.createTempFile("design-", ".txt");

    new Eoulsan2DesignWriter(outFile).write(design);

    // Read the design generated
    Design design2 = new Eoulsan2DesignReader(outFile).read();
    design2.setName(design.getName());

    // Test if equal
    assertEquals(design, design2);
    outFile.delete();
  }

  @Test
  public void test2() throws IOException {

    // Read a design file
    final InputStream is = this.getClass().getResourceAsStream("/design-v2.txt");
    Design design = new Eoulsan2DesignReader(is).read();

    // Rewrite the the read design
    File outFile = File.createTempFile("design-", ".txt");
    new Eoulsan2DesignWriter(outFile).write(design);

    // Read the design generated
    Design design2 = new Eoulsan2DesignReader(outFile).read();
    design2.setName(design.getName());

    // Test if equal
    assertEquals(design, design2);
    outFile.delete();
  }
}
