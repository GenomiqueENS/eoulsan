package fr.ens.biologie.genomique.eoulsan.modules.diffana;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.design.*;

import java.util.*;

import static fr.ens.biologie.genomique.eoulsan.design.DesignUtils.*;
import static fr.ens.biologie.genomique.eoulsan.design.SampleMetadata.CONDITION_KEY;
import static fr.ens.biologie.genomique.eoulsan.design.SampleMetadata.REP_TECH_GROUP_KEY;
import static fr.ens.biologie.genomique.eoulsan.design.SampleMetadata.REFERENCE_KEY;
import static java.util.Objects.requireNonNull;

/**
 * This class is made to check the design.txt file before running DESeq2.
 * @author Charlotte Berthelier
 * @since 2.4
 */

public class DESeq2Checker {

    /**
     * Check experiment design.
     * @param experiment experiment to check
     * @throws EoulsanException if the experiment design is not correct
     */
    static boolean checkExperimentDesign(Experiment experiment) throws EoulsanException {
        return checkExperimentDesign (experiment,true);
    }


    /**
     * Check experiment design.
     * @param experiment experiment to check
     * @param throwsException if true throw an exception
     * @throws EoulsanException if the experiment design is not correct
     */
    static boolean checkExperimentDesign(Experiment experiment, boolean throwsException) throws EoulsanException {

        requireNonNull(experiment, "Experiment argument cannot be null");
        final ExperimentMetadata emd = experiment.getMetadata();
        // Check the comparison string, if exists it is a complex design
        if (emd.containsComparisons()) {
            // Check if the comparison value is correct
            for (String c : emd.getComparisons().split(";")) {
                String[] splitC = c.split(":");
                // Check if there is not more than one value per comparison
                if (splitC.length != 2) {
                    return error("Error in "
                            + experiment.getName()
                            + " experiment, comparison cannot have more than 1 value: " + c, throwsException);
                }

                // Get the name of all Metadata keys
                String[] metakeys = DesignUtils.getExperimentSampleAllMetadataKeys(experiment).toArray(new String[0]);
                // Get each condition in the comparison string
                String[] listConditionsInComparisonString = splitC[1].split("(%)|(_vs_)");
                // Get every sample value for each Metadata key, and get every possible condition by merging the strings
                ArrayList<String> possibleConditions = new ArrayList<String>();
                for (String key : metakeys) {
                    for (ExperimentSample es : experiment.getExperimentSamples()) {
                        String value = DesignUtils.getMetadata(es, key);
                        possibleConditions.add(key+value);
                    }
                }

                // Check if each condition exists
                for (String condi : listConditionsInComparisonString){
                    Boolean exist = false;
                    for (int i=0;i<possibleConditions.size();i++){
                        if (possibleConditions.get(i).equals(condi)){
                            exist = true;
                        }
                    }
                    if (exist == false){
                        return error("Error in "
                                + experiment.getName()
                                + " experiment, one comparison does not exist: " + c, throwsException);
                    }
                }
            }
        }

        // Check if the column Condition is missing for the experiment
        if (!getExperimentSampleAllMetadataKeys(experiment)
                .contains(CONDITION_KEY)
                && !getAllSamplesMetadataKeys(experiment.getDesign()).contains(CONDITION_KEY)) {
            return error("Condition column missing for experiment: "
                    + experiment.getName(), throwsException);
        }

        // Check if the column RepTechGroup is missing for the experiment
        if (!getExperimentSampleAllMetadataKeys(experiment)
                .contains(REP_TECH_GROUP_KEY)
                && !getAllSamplesMetadataKeys(experiment.getDesign()).contains(REP_TECH_GROUP_KEY)) {
            return error("RepTechGroup column missing for experiment: "
                    + experiment.getName(), throwsException);
        }

        /* Check if there is no numeric character at the begin of a row in the column Condition
        for a complex design model */
        if (getExperimentSampleAllMetadataKeys(experiment).contains(CONDITION_KEY)) {
            for (ExperimentSample es : experiment.getExperimentSamples()) {
                String s = DesignUtils.getMetadata(es, CONDITION_KEY);
                // Condition column contains an invalid numeric character as first character
                if ( Character.isDigit(s.charAt(0)) && emd.getComparisons() != null){
                    return error("One or more Condition rows start with a numeric character : "
                            + experiment.getName(), throwsException);
                }
            }
        }

        // Check if there is no "-" in the column Condition when the contrast mode is activate
        if (getExperimentSampleAllMetadataKeys(experiment).contains(CONDITION_KEY)) {
            for (ExperimentSample es : experiment.getExperimentSamples()) {
                String s = DesignUtils.getMetadata(es, CONDITION_KEY);
                if (s.contains("-") && emd.isContrast()){
                    return error("There is a - character in the column Condition : "
                            + experiment.getName(), throwsException);
                }
            }
        }

        // Verify consistency between the values in the columns Reference and Condition for non complex mode
        // If mode is not complex
        if (!emd.containsComparisons()) {
            // If Exp.exp1.Condition and Exp.exp1.Reference exist
            if (getExperimentSampleAllMetadataKeys(experiment).contains(REFERENCE_KEY) && getExperimentSampleAllMetadataKeys(experiment).contains(CONDITION_KEY)){
                Map<String, String> lhm = new LinkedHashMap<>();
                // Get all values of the Condition and Reference columns
                for (ExperimentSample es : experiment.getExperimentSamples()) {
                    String conditions = DesignUtils.getMetadata(es, CONDITION_KEY);
                    String references = DesignUtils.getMetadata(es, REFERENCE_KEY);
                    // If one condition is associated with more than one reference, error
                    ArrayList<String> possibleConditionsReferences = new ArrayList<String>();
                    possibleConditionsReferences.add(conditions+references);
                    // if condition already in dictionnary check if value is equals to reference and if not error
                    for (String key : lhm.keySet()) {
                        String value = lhm.get(key);
                        if (key.equals(conditions) && !value.equals(references)){
                            return error("There is an inconsistency between the conditions and the references : "
                                    + experiment.getName(), throwsException);
                        }
                    }
                    lhm.put(conditions,references);
                }
            }
        }

        // Verify consistency between "Exp.exp1" in the header of the file and the ExperimentSample columns name
        // TO DO

        return true;
    }

        private static boolean error (String message, boolean throwsException) throws EoulsanException {

        if (throwsException){
            throw new EoulsanException(message);
        }

        return false;
    }

}
