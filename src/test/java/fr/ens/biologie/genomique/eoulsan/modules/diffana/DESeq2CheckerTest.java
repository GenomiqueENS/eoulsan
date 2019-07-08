package fr.ens.biologie.genomique.eoulsan.modules.diffana;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.design.*;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * This class is made to check the design.txt file before running DESeq2.
 * @author Charlotte Berthelier
 * @since 2.4
 */

public class DESeq2CheckerTest {

    @Test
    public void testCheckExperimentDesign() throws EoulsanException {

        Design d = DesignFactory.createEmptyDesign();
        Experiment e1 = d.addExperiment("e1");
        List<String> h = Arrays.asList("RepTechGroup", "Condition", "Reference");
        e1.addSample(addSample(d, h, "S1", "296-a","C1", "True"));
        e1.addSample(addSample(d, h, "S2", "298-c","C2", "False"));

        DesignUtils.showDesign(d);
        assertTrue(DESeq2Checker.checkExperimentDesign(e1, false));

        Design d2 = DesignFactory.createEmptyDesign();
        Experiment e2 = d2.addExperiment("e1");
        List<String> h2 = Arrays.asList("RepTechGroup","Conditionbis", "Reference");
        e2.addSample(addSample(d2, h2, "S3","296-a", "C3", "True"));
        e2.addSample(addSample(d2, h2, "S4", "296-a", "C4", "False"));

        DesignUtils.showDesign(d2);
        assertFalse(DESeq2Checker.checkExperimentDesign(e2, false));

        e2.getExperimentSample(d2.getSample("S3")).getMetadata().set("Conditionter","C2");
        assertFalse(DESeq2Checker.checkExperimentDesign(e2, false));

        e2.getExperimentSample(d2.getSample("S3")).getMetadata().set("Condition","C2");
        assertTrue(DESeq2Checker.checkExperimentDesign(e2, false));

    }

    private static Sample addSample(Design d, List<String> header, String sampleId, String... values){
        d.addSample(sampleId);
        Sample s = d.getSample(sampleId);
        for (int i=0; i<header.size();i++){
            s.getMetadata().set(header.get(i), values[i]);

        }
        return s;
    }


}