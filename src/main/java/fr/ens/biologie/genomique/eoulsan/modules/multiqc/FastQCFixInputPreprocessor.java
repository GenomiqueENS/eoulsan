package fr.ens.biologie.genomique.eoulsan.modules.multiqc;

/**
 * This class define a preprocessor for FastQC reports. It just exist for
 * compatibility with a jar patch.
 * @since 2.7
 * @author Laurent Jourdren
 */
public class FastQCFixInputPreprocessor extends FastQCInputPreprocessor {

  @Override
  public String getReportName() {
    return "fastqcfix";
  }

}
