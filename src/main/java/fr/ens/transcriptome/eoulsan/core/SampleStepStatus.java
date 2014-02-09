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

import java.util.Map;

import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class define a status when process a sample in a step.
 * @since 1.3
 * @author Laurent Jourdren
 */
public class SampleStepStatus {

  private final StepStatus status;
  private final Sample sample;

  /**
   * Get sample message.
   * @return the message for the sample
   */
  public String getMessage() {

    return this.status.getSampleMessage(this.sample);
  }

  /**
   * Get the sample counters.
   * @return the sample counters as a map
   */
  public Map<String, Long> getCounters() {

    return this.status.getSampleCounters(this.sample);
  }

  /**
   * Set the sample message.
   * @param message the message to set
   */
  public void setMessage(String message) {

    this.status.setSampleMessage(this.sample, message);
  }

  /**
   * Set the sample counters
   * @param reporter the reporter
   * @param counterGroup counter group to use with the reporter
   * @param sampleCounterHeader header for the sample (optional)
   */
  public void setCounters(Reporter reporter, String counterGroup,
      String sampleCounterHeader) {

    this.status.setSampleCounters(this.sample, reporter, counterGroup,
        sampleCounterHeader);
  }

  /**
   * Get the progress of a sample processing.
   * @return the progress of the processing of the sample as percent (between
   *         0.0 and 1.0)
   */
  public double getProgress() {

    return this.status.getSampleProgress(this.sample);
  }

  /**
   * Set the progress of the processing of a sample by the step.
   * @param min minimal value of the progress
   * @param max maximal value of the progress
   * @param value current value of the progress
   */
  public void setProgress(int min, int max, int value) {

    this.status.setSampleProgress(this.sample, min, max, value);
  }

  /**
   * Set the progress of the processing of a sample by the step.
   * @param progress value of the progress. This value must be greater or equals
   *          to 0 and lower or equals to 1.0
   */
  public void setProgress(double progress) {

    this.status.setSampleProgress(this.sample, progress);
  }

  //
  // Constructor
  //

  public SampleStepStatus(final StepStatus status, final Sample sample) {

    if (status == null)
      throw new NullPointerException("status is null");

    if (sample == null)
      throw new NullPointerException("sample is null");

    this.status = status;
    this.sample = sample;
  }
}
