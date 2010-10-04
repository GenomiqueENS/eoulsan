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

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;

public class SimpleExecutorInfo implements ExecutorInfo {

  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private String basePathname;
  private String logPathname;
  private String outputPathname;
  private String executionName;
  private String designPathname;
  private String paramPathname;

  //
  // Getters
  //

  /**
   * Get the base path
   * @return Returns the basePath
   */
  @Override
  public String getBasePathname() {
    return this.basePathname;
  }

  /**
   * Get the log path
   * @return Returns the log Path
   */
  @Override
  public String getLogPathname() {
    return this.logPathname;
  }

  /**
   * Get the output path
   * @return Returns the output Path
   */
  @Override
  public String getOutputPathname() {
    return this.outputPathname;
  }

  /**
   * Get the execution name.
   * @return the execution name
   */
  @Override
  public String getExecutionName() {
    return this.executionName;
  }
  
  /**
   * Get the design path.
   * @return the design path
   */
  @Override
  public String getDesignPathname() {
    return this.designPathname;
  }
  
  /**
   * Get the parameter path.
   * @return the parameter path
   */
  @Override
  public String getParameterPathname(){
    return this.paramPathname;
  }

  //
  // Setters
  //

  /**
   * Set the base path
   * @param basePath The basePath to set
   */
  public void setBasePathname(final String basePath) {

    logger.info("Base path: " + basePath);
    this.basePathname = basePath;
  }

  /**
   * Set the log path
   * @param logPath The log Path to set
   */
  public void setLogPathname(final String logPath) {

    logger.info("Log path: " + logPath);
    this.logPathname = logPath;
  }

  /**
   * Set the output path
   * @param logPathname The log Path to set
   */
  public void setOutputPathname(final String outputPath) {

    logger.info("Output path: " + outputPath);
    this.outputPathname = outputPath;
  }
  
  /**
   * Set the design path
   * @param designPathname The log Path to set
   */
  public void setDesignPathname(final String designPathname) {

    logger.info("Design path: " + designPathname);
    this.designPathname = designPathname;
  }
  
  /**
   * Set the parameter path
   * @param paramPathname The log Path to set
   */
  public void setParameterPathname(final String paramPathname) {

    logger.info("Parameter path: " + paramPathname);
    this.paramPathname = paramPathname;
  }

  //
  // Other methods
  //

  private void createExecutionName() {

    final Calendar cal = Calendar.getInstance(Locale.ENGLISH);
    cal.setTime(new Date(System.currentTimeMillis()));

    this.executionName =
        Globals.APP_NAME_LOWER_CASE
            + "-"
            + String.format("%04d%02d%02d%02d%02d%02d", cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal
                    .get(Calendar.SECOND));
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public SimpleExecutorInfo() {

    createExecutionName();
  }

}
