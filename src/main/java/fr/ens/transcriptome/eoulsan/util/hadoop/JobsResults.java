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

package fr.ens.transcriptome.eoulsan.util.hadoop;

import java.util.ArrayList;
import java.util.List;

import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.steps.StepResult;

/**
 * This class define the result of a Hadoop MapReduce Job. It contains the log
 * and the list of the failed jobs.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class JobsResults {

  private String log;
  private List<String> failedJobname = new ArrayList<String>();
  

  /**
   * Get the log
   * @return Returns the log
   */
  public String getLog() {
    return this.log;
  }

  /**
   * Get the number of failled Jobs
   * @return Returns the failedJobs
   */
  public int getFailedJobs() {
    return this.failedJobname.size();
  }

  /**
   * Test if all the jobs has been successful
   * @return true if all the jobs are successful
   */
  public boolean isSuccessful() {

    return this.failedJobname.size() == 0;
  }

  /**
   * Set the log.
   * @param log log to set
   */
  protected void setLog(final String log) {

    this.log = log;
  }

  /**
   * Add the name of a failed job
   * @param jobName The name of the job to add
   */
  protected void addFailedJob(final String jobName) {

    this.failedJobname.add(jobName);
  }

  /**
   * Get Step result.
   * @param context context of the step
   * @param startTime the time of the start of the step
   * @return a new Step result object
   */
  public StepResult getStepResult(final Context context, final long startTime) {

    if (isSuccessful())
      return new StepResult(context, startTime, getLog());

    return new StepResult(context, false, startTime, getLog(), getFailedJobs()
        + " jobs failed");
  }

}
