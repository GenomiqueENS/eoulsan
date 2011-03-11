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

package fr.ens.transcriptome.eoulsan.steps;

import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.ProcessSample.ProcessSampleException;

/**
 * This class allow to process samples of a step with multithreading.
 * @author Laurent Jourdren
 */
public class ProcessSampleExecutor {

  /**
   * Process all the sample of a design.
   * @param context The Eouslan context of execution
   * @param design Design of to process
   * @param ps ProcessSample Object
   * @return a StepResult object
   */
  public static final StepResult processAllSamples(final Context context,
      final Design design, ProcessSample ps) {

    return processAllSamples(System.currentTimeMillis(), context, design, ps);
  }

  /**
   * Process all the sample of a design.
   * @param startTime the time of the start of step
   * @param context The Eouslan context of execution
   * @param design Design of to process
   * @param ps ProcessSample Object
   * @return a StepResult object
   */
  public static final StepResult processAllSamples(final long startTime,
      final Context context, final Design design, ProcessSample ps) {

    final StringBuilder log = new StringBuilder();

    try {

      // Process all the samples
      for (Sample sample : design.getSamples())
        log.append(ps.processSample(context, sample));

    } catch (ProcessSampleException e) {
      new StepResult(context, e.getException(), e.getMessage());
    }

    return new StepResult(context, startTime, log.toString());
  }

}
