package fr.ens.biologie.genomique.eoulsan.modules.diffana;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.design.*;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;

import static fr.ens.biologie.genomique.eoulsan.design.DesignFactory.createEmptyDesign;
import static fr.ens.biologie.genomique.eoulsan.modules.diffana.DESeq2Checker.checkExperimentDesign;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 * This class is made to check the design.txt file before running DESeq2.
 * @author Charlotte Berthelier
 * @since 2.4
 */

public class DESeq2CheckerTest {

    @Test
    public void testCheckExperimentDesign() throws EoulsanException {

        /* Create test experiments */


        /* Test if the column Condition is missing for the experiment */

        // Working example
        Design testCondiColumn = createEmptyDesign();
        Experiment e1 = testCondiColumn.addExperiment("exp1");
        List<String> h1 = asList("RepTechGroup", "Condition");
        e1.addSample(addSample(testCondiColumn, h1, "S1", "296-a","KO"));
        assertTrue("Condition column missing for experiment",
                checkExperimentDesign(e1, false));
        // Example containing a mistake: the column Condition is missing
        Design testCondiColumnBis = createEmptyDesign();
        Experiment e1bis = testCondiColumnBis.addExperiment("exp1");
        List<String> h1bis = asList("RepTechGroup");
        e1bis.addSample(addSample(testCondiColumnBis, h1bis, "S1", "296-a"));
        assertFalse("Condition column missing for experiment",
                checkExperimentDesign(e1bis, false));


        /* Test if the column RepTechGroup is missing for the experiment */

        // Working example
        Design testReptechgroupColumn = createEmptyDesign();
        Experiment e2 = testReptechgroupColumn.addExperiment("exp1");
        List<String> h2 = asList("RepTechGroup", "Condition");
        e2.addSample(addSample(testReptechgroupColumn, h2, "S1", "296-a","KO"));
        assertTrue("RepTechGroup column missing for experiment",
                checkExperimentDesign(e2, false));
        // Example containing a mistake: the column RepTechGroup is missing
        Design testReptechgroupColumnBis = createEmptyDesign();
        Experiment e2bis = testReptechgroupColumnBis.addExperiment("exp1");
        List<String> h2bis = asList("Condition");
        e2bis.addSample(addSample(testReptechgroupColumnBis, h2bis, "S1", "KO"));
        assertFalse("RepTechGroup column missing for experiment",
                checkExperimentDesign(e2bis, false));


        /* Test if the comparison string is correct */

        // Working example
        Design testCompString = createEmptyDesign();
        Experiment e3 = testCompString.addExperiment("exp1");
        e3.getMetadata().setComparisons("HT29:LigneeHT29%CultureD3_vs_LigneeHT29%CultureD2;" +
                "D3:CultureD3%Etatvieille_vs_CultureD3%Etatjeune");
        List<String> h3 = asList("RepTechGroup", "Condition");
        e3.addSample(addSample(testCompString, h3, "S1", "296-a","KO"));
        e3.addSample(addSample(testCompString, h3, "S2", "298-c","KO"));
        addExperimentSample(testCompString, asList("Lignee"),"S1","exp1","HT29");
        addExperimentSample(testCompString, asList("Lignee"),"S2","exp1","LS513");
        addExperimentSample(testCompString, asList("Culture"),"S1","exp1","D3");
        addExperimentSample(testCompString, asList("Culture"),"S2","exp1","D2");
        addExperimentSample(testCompString, asList("Etat"),"S1","exp1","vieille");
        addExperimentSample(testCompString, asList("Etat"),"S2","exp1","jeune");
        assertTrue("The comparison string is not correct", checkExperimentDesign(e3, false));
        // Example containing a mistake
        Design testCompStringBis = createEmptyDesign();
        Experiment e3bis = testCompStringBis.addExperiment("exp1");
        e3bis.getMetadata().setComparisons("HT29:LigneeHT29%CultureD3_vs_LigneeHT29%CultureD1");
        List<String> h3bis = asList("RepTechGroup", "Condition");
        e3bis.addSample(addSample(testCompStringBis, h3bis, "S1", "296-a","KO"));
        e3bis.addSample(addSample(testCompStringBis, h3bis, "S2", "298-c","KO"));
        addExperimentSample(testCompStringBis, asList("Lignee"),"S1","exp1","HT29");
        addExperimentSample(testCompStringBis, asList("Lignee"),"S2","exp1","LS513");
        addExperimentSample(testCompStringBis, asList("Culture"),"S1","exp1","D3");
        addExperimentSample(testCompStringBis, asList("Culture"),"S2","exp1","D2");
        assertFalse("The comparison string is not correct", checkExperimentDesign(e3bis, false));


        /* Test if there is no "-" in the column Condition when the contrast mode is activate */

        // Working example
        Design testDashInConditon = createEmptyDesign();
        Experiment e4 = testDashInConditon.addExperiment("exp1");
        e4.getMetadata().setContrast(true);
        List<String> h4 = asList("RepTechGroup", "Condition");
        e4.addSample(addSample(testDashInConditon, h4, "S1", "296-a","KO"));
        assertTrue("There is a dash in the condition column while the contrast mode is activate",
                checkExperimentDesign(e4, false));
        // Example containing a mistake: "KO-" as Condition for S1 when the contrast mode is activate
        Design testDashInConditonBis = createEmptyDesign();
        Experiment e4bis = testDashInConditonBis.addExperiment("exp1");
        e4bis.getMetadata().setContrast(true);
        List<String> h4bis = asList("RepTechGroup", "Condition");
        e4bis.addSample(addSample(testDashInConditonBis, h4bis, "S1", "296-a","KO-"));
        assertFalse("There is a dash in the condition column while the contrast mode is activate",
                checkExperimentDesign(e4bis, false));


        /* Test if there is no numeric character at the begin of a row in the column Condition
        for a complex design model */

        // Working example
        Design testCondiIfNumber = createEmptyDesign();
        Experiment e5 = testCondiIfNumber.addExperiment("exp1");
        e5.getMetadata().setComparisons("HT29:LigneeHT29%CultureD3_vs_LigneeHT29%CultureD2");
        List<String> h5 = asList("RepTechGroup", "Condition");
        e5.addSample(addSample(testCondiIfNumber, h5, "S1", "296-a","KO"));
        e5.addSample(addSample(testCondiIfNumber, h5, "S2", "298-c","KO"));
        addExperimentSample(testCondiIfNumber, asList("Lignee"),"S1","exp1","HT29");
        addExperimentSample(testCondiIfNumber, asList("Lignee"),"S2","exp1","LS513");
        addExperimentSample(testCondiIfNumber, asList("Culture"),"S1","exp1","D3");
        addExperimentSample(testCondiIfNumber, asList("Culture"),"S2","exp1","D2");
        assertTrue("There is a numeric character at the begin of a row in the column Condition " +
                "for a complex design model", checkExperimentDesign(e5, false));
        // Example containing a mistake: "1KO" as Condition for S1 with a complex model
        Design testCondiIfNumberBis = createEmptyDesign();
        Experiment e5bis = testCondiIfNumberBis.addExperiment("exp1");
        e5bis.getMetadata().setComparisons("HT29:LigneeHT29%CultureD3_vs_LigneeHT29%CultureD2");
        List<String> h5bis = asList("RepTechGroup", "Condition");
        e5bis.addSample(addSample(testCondiIfNumberBis, h5bis, "S1", "296-a","1KO"));
        e5bis.addSample(addSample(testCondiIfNumberBis, h5bis, "S2", "298-c","KO"));
        addExperimentSample(testCondiIfNumberBis, asList("Lignee"),"S1","exp1","HT29");
        addExperimentSample(testCondiIfNumberBis, asList("Lignee"),"S2","exp1","LS513");
        addExperimentSample(testCondiIfNumberBis, asList("Culture"),"S1","exp1","D3");
        addExperimentSample(testCondiIfNumberBis, asList("Culture"),"S2","exp1","D2");
        assertFalse("There is a numeric character at the begin of a row in the column Condition " +
                "for a complex design model", checkExperimentDesign(e5bis, false));


        /* Test consistency between the values in the columns Reference and Condition for non complex mode */

        // Working example
        Design testConsistency = createEmptyDesign();
        Experiment e6 = testConsistency.addExperiment("exp1");
        List<String> h6 = asList("RepTechGroup");
        e6.addSample(addSample(testConsistency, h6, "S1", "296-a"));
        e6.addSample(addSample(testConsistency, h6, "S2", "298-c"));
        e6.addSample(addSample(testConsistency, h6, "S3", "294-b"));
        addExperimentSample(testConsistency, asList("Condition"),"S1","exp1","KO");
        addExperimentSample(testConsistency, asList("Condition"),"S2","exp1","WT");
        addExperimentSample(testConsistency, asList("Condition"),"S3","exp1","WT");
        addExperimentSample(testConsistency, asList("Reference"),"S1","exp1","false");
        addExperimentSample(testConsistency, asList("Reference"),"S2","exp1","true");
        addExperimentSample(testConsistency, asList("Reference"),"S3","exp1","true");
        assertTrue("There is inconstancy between the values in the columns Reference and Condition",
                checkExperimentDesign(e6, false));
        // Example containing a mistake: KO-false WT-true and KO-true
        Design testConsistencyBis = createEmptyDesign();
        Experiment e6bis = testConsistencyBis.addExperiment("exp1");
        List<String> h6bis = asList("RepTechGroup");
        e6bis.addSample(addSample(testConsistencyBis, h6bis, "S1", "296-a"));
        e6bis.addSample(addSample(testConsistencyBis, h6bis, "S2", "298-c"));
        e6bis.addSample(addSample(testConsistencyBis, h6bis, "S3", "294-b"));
        addExperimentSample(testConsistencyBis, asList("Condition"),"S1","exp1","KO");
        addExperimentSample(testConsistencyBis, asList("Condition"),"S2","exp1","WT");
        addExperimentSample(testConsistencyBis, asList("Condition"),"S3","exp1","KO");
        addExperimentSample(testConsistencyBis, asList("Reference"),"S1","exp1","false");
        addExperimentSample(testConsistencyBis, asList("Reference"),"S2","exp1","true");
        addExperimentSample(testConsistencyBis, asList("Reference"),"S3","exp1","true");
        assertFalse("There is inconstancy between the values in the columns Reference and Condition",
                checkExperimentDesign(e6bis, false));


        /* Test to check if there is no combination column-data that equals to a column name */

        // Working example
        Design testCombinationUnique = createEmptyDesign();
        Experiment e7 = testCombinationUnique.addExperiment("exp1");
        List<String> h7 = asList("RepTechGroup", "Condition");
        e7.addSample(addSample(testCombinationUnique, h7, "S1", "296-a","KO"));
        e7.addSample(addSample(testCombinationUnique, h7, "S2", "298-c","KO"));
        addExperimentSample(testCombinationUnique, asList("Toto"),"S1","exp1","D3");
        addExperimentSample(testCombinationUnique, asList("Toto"),"S2","exp1","D2");
        addExperimentSample(testCombinationUnique, asList("To"),"S1","exp1","ta");
        addExperimentSample(testCombinationUnique, asList("To"),"S2","exp1","tu");
        assertTrue("Combinations column-data are not unique",
                checkExperimentDesign(e7, false));
        // Example containing a mistake: column "Toto" == column "To"+ data "to"
        Design testCombinationUniqueBis = createEmptyDesign();
        Experiment e7bis = testCombinationUniqueBis.addExperiment("exp1");
        List<String> h7bis = asList("RepTechGroup", "Condition");
        e7bis.addSample(addSample(testCombinationUniqueBis, h7bis, "S1", "296-a","KO"));
        e7bis.addSample(addSample(testCombinationUniqueBis, h7bis, "S2", "298-c","KO"));
        addExperimentSample(testCombinationUniqueBis, asList("Toto"),"S1","exp1","D3");
        addExperimentSample(testCombinationUniqueBis, asList("Toto"),"S2","exp1","D2");
        addExperimentSample(testCombinationUniqueBis, asList("To"),"S1","exp1","to");
        addExperimentSample(testCombinationUniqueBis, asList("To"),"S2","exp1","ta");
        assertFalse("Combinations column-data are not unique",
                checkExperimentDesign(e7bis, false));


        /* Test if there is no empty cell in the experiment */

        // Working example
        Design testEmptyCell = createEmptyDesign();
        Experiment e8 = testEmptyCell.addExperiment("exp1");
        List<String> h8 = asList("RepTechGroup", "Condition");
        e8.addSample(addSample(testEmptyCell, h8, "S1", "296-a","KO"));
        e8.addSample(addSample(testEmptyCell, h8, "S2", "298-c","KO"));
        assertTrue("There is empty cell in the experiment",checkExperimentDesign(e8, false));
        // Example containing a mistake: empty cell in the Condition column for S1
        Design testEmptyCellBis = createEmptyDesign();
        Experiment e8bis = testEmptyCellBis.addExperiment("exp1");
        List<String> h8bis = asList("RepTechGroup", "Condition");
        e8bis.addSample(addSample(testEmptyCellBis, h8bis, "S1", "296-a"," "));
        e8bis.addSample(addSample(testEmptyCellBis, h8bis, "S2", "298-c","KO"));
        assertFalse("There is empty cell in the experiment",checkExperimentDesign(e8bis, false));
    }

    private static Sample addSample(Design d, List<String> header, String sampleId, String... values){
        d.addSample(sampleId);
        Sample s = d.getSample(sampleId);
        for (int i=0; i<header.size();i++){
            s.getMetadata().set(header.get(i), values[i]);
        }
        return s;
    }

    private static ExperimentSample addExperimentSample(Design d, List<String> header, String sampleId,
                                                        String experimentId, String... values){
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