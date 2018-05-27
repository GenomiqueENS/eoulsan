package fr.ens.biologie.genomique.eoulsan.modules.multiqc;

import java.io.File;
import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;

/**
 * This interface define a preprocessor for MultiQC input data.
 * @since 2.2
 * @author Laurent Jourdren
 */
public interface InputPreprocessor {

  /**
   * Get the name of the report.
   * @return the name of the report
   */
  String getReportName();

  /**
   * Get the DataFormat handled by the preprocessor.
   * @return the DataFormat handled by the preprocessor
   */
  DataFormat getDataFormat();

  /**
   * Preprocess data.
   * @param context Step context
   * @param data data to preprocess
   * @param multiQCInputDirectory MultiQC input directory
   * @throws IOException if an error occurs while preprocessing the data
   */
  void preprocess(TaskContext context, Data data, File multiQCInputDirectory)
      throws IOException;

}