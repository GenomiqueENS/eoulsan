package fr.ens.biologie.genomique.eoulsan.util.r;

import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.data.DataFile;

/**
 * This interface define how to prepare, launch and retrieve data of a R
 * analysis.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface RExecutor {

  /**
   * Get the name of the RExecutor.
   * @return the name of the RExecutor
   */
  String getName();

  /**
   * Open the connection.
   * @throws IOException if an error occurs while opening the connection
   */
  void openConnection() throws IOException;

  /**
   * Close the connection.
   * @throws IOException if an error occurs while closing the connection
   */
  void closeClonnection() throws IOException;

  /**
   * Put a file.
   * @param inputFile the file to put
   * @param outputFilename the output filename
   * @throws IOException if an exception occurs while putting a file
   */
  void putInputFile(DataFile inputFile, String outputFilename)
      throws IOException;

  /**
   * Execute a R script.
   * @param rScript the source of the script to execute
   * @param sweave true if the script is a Sweave script
   * @param sweaveOutput Sweave output file
   * @param saveRscript true to keep the R script
   * @param description description of the R script
   * @throws IOException if an error occurs while executing the script
   */
  void executeRScript(String rScript, boolean sweave, String sweaveOutput,
      boolean saveRscript, String description) throws IOException;

  /**
   * Remove input files.
   * @throws IOException if an error occurs while removing the files
   */
  void removeInputFiles() throws IOException;

  /**
   * Get the output files of the analysis
   * @throws IOException if an error occurs while getting the output files
   */
  void getOutputFiles() throws IOException;

}
