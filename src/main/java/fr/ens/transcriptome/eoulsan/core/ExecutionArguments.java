/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.core;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import fr.ens.transcriptome.eoulsan.Globals;

public class ExecutionArguments {

  private String basePathname;
  private String designPathname;
  private String paramPathname;
  private String jobDescription = "";
  private String jobEnvironment = "";
  private String outputPathname;
  private String logPathname;
  private final String jobId;
  private final String jobUUID = UUID.randomUUID().toString();
  private final long creationTime;

  //
  // Getters
  //

  public final String getBasePathname() {
    return this.basePathname;
  }

  public final String getLogPathname() {
    return this.logPathname;
  }

  public final String getOutputPathname() {
    return this.outputPathname;
  }

  public final String getDesignPathname() {
    return this.designPathname;
  }

  public final String getParameterPathname() {
    return this.paramPathname;
  }

  public final String getJobDescription() {
    return this.jobDescription == null ? "" : this.jobDescription.trim();
  }

  public final String getJobEnvironment() {
    return this.jobEnvironment == null ? "" : this.jobEnvironment.trim();
  }

  public final String getJobId() {
    return this.jobId;
  }

  public final String getJobUUID() {
    return this.jobUUID;
  }

  public final long getCreationTime() {
    return this.creationTime;
  }

  //
  // Setters
  //

  /**
   * Set the base path
   * @param basePath The basePath to set
   */
  public final void setBasePathname(final String basePath) {

    if (basePath == null)
      return;

    this.basePathname = basePath.trim();
  }

  /**
   * Set the log path
   * @param logPath The log path to set
   */
  public final void setLogPathname(final String logPath) {

    if (logPath == null)
      return;

    this.logPathname = logPath.trim();
  }

  /**
   * Set the output path
   * @param outputPath The output path to set
   */
  public final void setOutputPathname(final String outputPath) {

    if (outputPath == null)
      return;

    this.outputPathname = outputPath.trim();
  }

  /**
   * Set the design path
   * @param designPathname The design path to set
   */
  public final void setDesignPathname(final String designPathname) {

    if (designPathname == null)
      return;

    this.designPathname = designPathname.trim();
  }

  /**
   * Set the parameter path
   * @param paramPathname The parameter path to set
   */
  public final void setParameterPathname(final String paramPathname) {

    if (paramPathname == null)
      return;

    this.paramPathname = paramPathname.trim();
  }

  /**
   * Set job description.
   * @param jobDescription job description
   */
  public final void setJobDescription(final String jobDescription) {

    if (jobDescription == null)
      return;

    this.jobDescription = jobDescription.trim();
  }

  /**
   * Set job environment.
   * @param jobEnvironment job environment
   */
  public final void setJobEnvironment(final String jobEnvironment) {

    if (jobEnvironment == null)
      return;

    this.jobEnvironment = jobEnvironment.trim();
  }

  //
  // Constructor
  //

  public ExecutionArguments() {

    this(System.currentTimeMillis());
  }

  public ExecutionArguments(final long millisSinceEpoch) {

    final Calendar cal = Calendar.getInstance(Locale.ENGLISH);
    cal.setTime(new Date(millisSinceEpoch));

    final String creationDate =
        String.format("%04d%02d%02d-%02d%02d%02d", cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND));

    this.creationTime = millisSinceEpoch;
    this.jobId = Globals.APP_NAME_LOWER_CASE + "-" + creationDate;
  }

  public ExecutionArguments(final File parameterFile, final File designFile) {

    this();

    // Set the base path
    setBasePathname(designFile.getAbsoluteFile().getParentFile()
        .getAbsolutePath());

    // Set the design path
    setDesignPathname(designFile.getAbsolutePath());

    // Set the parameter path
    setParameterPathname(parameterFile.getAbsolutePath());

    final File logDir =
        new File(designFile.getAbsoluteFile().getParent().toString()
            + "/" + getJobId());

    final File outputDir =
        new File(designFile.getAbsoluteFile().getParent().toString()
            + "/" + getJobId());

    // Set the output path
    setOutputPathname(outputDir.getAbsolutePath());

    // Set the log path
    setLogPathname(logDir.getAbsolutePath());
  }

}
