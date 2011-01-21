/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.AbstractEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * This interface define a context.
 * @author Laurent Jourdren
 */
public interface Context {

  /**
   * Get the command name.
   * @return the command name
   */
  String getCommandName();

  /**
   * Get the job name.
   * @return the job name
   */
  String getJobName();

  /**
   * Get the UUID of the job.
   * @return the job UUID
   */
  String getJobUUID();

  /**
   * Get the job description.
   * @return the job description
   */
  String getJobDescription();

  /**
   * Get the job environment.
   * @return the job environment
   */
  String getJobEnvironment();

  /**
   * Get command description.
   * @return the command description
   */
  String getCommandDescription();

  /**
   * Get the command author.
   * @return the command author
   */
  String getCommandAuthor();

  /**
   * Get the base path.
   * @return Returns the basePath
   */
  String getBasePathname();

  /**
   * Get the log path.
   * @return Returns the log Path
   */
  String getLogPathname();

  /**
   * Get the output path.
   * @return Returns the output Path
   */
  String getOutputPathname();

  /**
   * Get the execution name.
   * @return the execution name
   */
  String getExecutionName();

  /**
   * Get the design path.
   * @return the design path
   */
  String getDesignPathname();

  /**
   * Get the parameter path.
   * @return the parameter path
   */
  String getParameterPathname();

  /**
   * Get the application jar path.
   * @return Returns the jar path
   */
  String getJarPathname();

  /**
   * Add executor information to log.
   */
  void logInfo();

  /**
   * Get EoulsanRuntime.
   * @return the EoulsanRuntime
   */
  AbstractEoulsanRuntime getRuntime();

  /**
   * Get the logger.
   * @return the logger
   */
  Logger getLogger();

  /**
   * Get the workflow description
   * @return the workflow description
   */
  WorkflowDescription getWorkflow();

  /**
   * Get the pathname for a DataType and a Sample.
   * @param df the DataFormat of the source
   * @param sample the sample for the source
   * @return a String with the pathname
   */
  String getDataFilename(DataFormat df, Sample sample);

  /**
   * Get the DataFile for a DataType and a Sample.
   * @param df the DataFormat of the source
   * @param sample the sample for the source
   * @return a String with the pathname
   */
  DataFile getDataFile(DataFormat df, Sample sample);

  /**
   * Create an InputStream to load data.
   * @param ds the DataFormat of the data to load
   * @param sample the sample
   * @return an InputStream corresponding to DataType and Sample
   */
  InputStream getInputStream(DataFormat df, Sample sample) throws IOException;

  /**
   * Create a raw InputStream (without decompression of input data) to load
   * data.
   * @param dt the DataFormat of the data to load
   * @param sample the sample
   * @return an InputStream corresponding to DataType and Sample
   */
  InputStream getRawInputStream(DataFormat df, Sample sample)
      throws IOException;

  /**
   * Create an OutputStream to load data.
   * @param dt the DataFormat of the data to write
   * @param sample the sample
   * @return an InputStream corresponding to DataType and Sample
   */
  OutputStream getOutputStream(DataFormat df, Sample sample) throws IOException;
}
