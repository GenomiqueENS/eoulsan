package fr.ens.biologie.genomique.eoulsan.modules.diffana;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.design.*;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;

import static fr.ens.biologie.genomique.eoulsan.modules.diffana.DESeq2Checker.checkExperimentDesign;
import static org.junit.Assert.*;

/**
 * This class is made to check the design.txt file before running DESeq2.
 * @author Charlotte Berthelier
 * @since 2.4
 */

public class DESeq2CheckerTest {

    @Test
    public void testCheckExperimentDesign() throws EoulsanException {

        // Create a test experiment
        Design d = DesignFactory.createEmptyDesign();
        Experiment e1 = d.addExperiment("exp1");

        e1.getMetadata().setSkip(false);
        e1.getMetadata().setContrast(true);
        e1.getMetadata().setBuildContrast(true);
        e1.getMetadata().setModel("~Lignee+Culture+Lignee:Culture");
        //e1.getMetadata().setComparisons("HT29:LigneeHT29%CultureD3_vs_LigneeHT29%CultureD2");
        //e1.getMetadata().setComparisons("HT29:LigneeHT29%CultureD3_vs_LigneeHT29%CultureD2;LS513:LigneeLS513%CultureD3_vs_LigneeLS513%CultureD2;D3:CultureD3%LigneeHT29_vs_CultureD3%LigneeLS513");

        List<String> h = Arrays.asList("RepTechGroup");
        e1.addSample(addSample(d, h, "S1", "296-a"));
        e1.addSample(addSample(d, h, "S2", "298-c"));
        e1.addSample(addSample(d, h, "S3", "294-b"));

        addExperimentSample(d,Arrays.asList("Condition"),"S1","exp1","KO");
        addExperimentSample(d,Arrays.asList("Condition"),"S2","exp1","WT");
        addExperimentSample(d,Arrays.asList("Condition"),"S3","exp1","WT");
        addExperimentSample(d,Arrays.asList("Reference"),"S1","exp1","false");
        addExperimentSample(d,Arrays.asList("Reference"),"S2","exp1","true");
        addExperimentSample(d,Arrays.asList("Reference"),"S3","exp1","true");
        addExperimentSample(d,Arrays.asList("Lignee"),"S1","exp1","HT29");
        addExperimentSample(d,Arrays.asList("Lignee"),"S2","exp1","LS513");
        addExperimentSample(d,Arrays.asList("Lignee"),"S3","exp1","HT29");
        addExperimentSample(d,Arrays.asList("Culture"),"S1","exp1","D3");
        addExperimentSample(d,Arrays.asList("Culture"),"S2","exp1","D2");
        addExperimentSample(d,Arrays.asList("Culture"),"S3","exp1","D2");

        // Run the checkExperimentDesign function for the experiment e1
        assertTrue(checkExperimentDesign(e1, false));

    }

    private static Sample addSample(Design d, List<String> header, String sampleId, String... values){
        d.addSample(sampleId);
        Sample s = d.getSample(sampleId);
        for (int i=0; i<header.size();i++){
            s.getMetadata().set(header.get(i), values[i]);
        }
        return s;
    }

    private static ExperimentSample addExperimentSample(Design d, List<String> header, String sampleId, String experimentId, String... values){
        Sample s = d.getSample(sampleId);
        if (!d.containsExperiment(experimentId)) {
            d.addExperiment(experimentId);
        }
        ExperimentSample es = d.getExperiment(experimentId).getExperimentSample(s);
        for (int i=0; i<header.size();i++){
            es.getMetadata().set(header.get(i), values[i]);

        }
        return es;
    }

}