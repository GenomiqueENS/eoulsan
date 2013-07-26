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
 * of the Institut de Biologie de l'√âcole Normale Sup√©rieure and
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

package fr.ens.transcriptome.eoulsan.core.workflow;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeDebug;
import fr.ens.transcriptome.eoulsan.core.Command;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.SimpleContext;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.DesignFactory;
import fr.ens.transcriptome.eoulsan.design.DesignUtils;
import fr.ens.transcriptome.eoulsan.design.SampleMetadata;

public class NewWorkflowDemo {

  private static final String TEST_DIR = "/home/jourdren/tmp/workflow-test/";

  private static Command createCommand() throws EoulsanException {

    Command c = new Command();
    c.setAuthor("Laurent Jourdren");
    c.setDescription("The description");
    c.setName("Worflow name");

    Set<Parameter> ps1 = new LinkedHashSet<Parameter>();
    ps1.add(new Parameter("illuminaid", ""));
    ps1.add(new Parameter("lengthThreshold", "11"));
    ps1.add(new Parameter("qualityThreshold", "12"));
    c.addStep("filterreads", "filterreads", ps1, false);

    Set<Parameter> ps2 = new LinkedHashSet<Parameter>();
    ps2.add(new Parameter("mapper", "bowtie"));
    ps2.add(new Parameter("mapperarguments", "--best -k 2"));
    c.addStep("mapreads", "mapreads", ps2, false);

    Set<Parameter> ps3 = new LinkedHashSet<Parameter>();
    ps3.add(new Parameter("removeunmapped", ""));
    ps3.add(new Parameter("removemultimatches", ""));
    c.addStep("filtersam", "filtersam", ps3, false);

    Set<Parameter> ps4 = new LinkedHashSet<Parameter>();
    ps4.add(new Parameter("counter", "htseq-count"));
    ps4.add(new Parameter("genomictype", "exon"));
    ps4.add(new Parameter("attributeid", "PARENT"));
    ps4.add(new Parameter("stranded", "no"));
    ps4.add(new Parameter("overlapmode", "union"));
    c.addStep("expression", "expression", ps4, false);

    return c;
  }

  private static Design createDesign() {

    final Design d = DesignFactory.createEmptyDesign();

    d.addSample("s1");
    d.addSample("s2");

    d.addMetadataField(SampleMetadata.READS_FIELD);
    d.getSample("s1").getMetadata()
        .setReads(Collections.singletonList(TEST_DIR + "s1.fq.bz2"));
    d.getSample("s2").getMetadata()
        .setReads(Collections.singletonList(TEST_DIR + "s2.fq.bz2"));

    d.addMetadataField(SampleMetadata.GENOME_FIELD);
    d.getSample("s1").getMetadata().setGenome(TEST_DIR + "genome.fasta.bz2");
    d.getSample("s2").getMetadata().setGenome(TEST_DIR + "genome.fasta.bz2");

    d.addMetadataField(SampleMetadata.ANNOTATION_FIELD);
    d.getSample("s1").getMetadata().setAnnotation(TEST_DIR + "annotation.gff.bz2");
    d.getSample("s2").getMetadata().setAnnotation(TEST_DIR + "annotation.gff.bz2");

    return d;
  }

  public static final void main(final String[] args) throws EoulsanException,
      IOException {

    EoulsanRuntimeDebug.initDebugEoulsanRuntime();

    Command command = createCommand();
    Design design = createDesign();
    Context context = new SimpleContext();

    DesignUtils.showDesign(design);

    CommandWorkflow nwf = new CommandWorkflow(command, null, null, design, context);

    //nwf.show();
    
    
    

  }

}
