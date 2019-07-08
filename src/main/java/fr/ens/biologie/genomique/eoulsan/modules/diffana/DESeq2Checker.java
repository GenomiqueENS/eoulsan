package fr.ens.biologie.genomique.eoulsan.modules.diffana;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentMetadata;

import static fr.ens.biologie.genomique.eoulsan.design.DesignUtils.getAllSamplesMetadataKeys;
import static fr.ens.biologie.genomique.eoulsan.design.DesignUtils.getExperimentSampleAllMetadataKeys;
import static fr.ens.biologie.genomique.eoulsan.design.SampleMetadata.CONDITION_KEY;
import static fr.ens.biologie.genomique.eoulsan.design.SampleMetadata.REP_TECH_GROUP_KEY;
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
        return checkExperimentDesign (experiment, true);
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

        if (emd.containsComparisons()) {

            // Check if the comparison value is correct
            for (String c : emd.getComparisons().split(";")) {
                String[] splitC = c.split(":");
                if (splitC.length != 2) {
                    return error("Error in "
                            + experiment.getName()
                            + " experiment, comparison cannot have more than 1 value: " + c, throwsException);
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

        if (!getExperimentSampleAllMetadataKeys(experiment)
                .contains(REP_TECH_GROUP_KEY)
                && !getAllSamplesMetadataKeys(experiment.getDesign()).contains(REP_TECH_GROUP_KEY)) {
            return error("RepTechGroup column missing for experiment: "
                    + experiment.getName(), throwsException);
        }

        return true;
    }

    private static boolean error (String message, boolean throwsException) throws EoulsanException {

        if (throwsException){
            throw new EoulsanException(message);
        }

        return false;
    }
}
