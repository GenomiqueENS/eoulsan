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

import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.annotations.EoulsanMode;
import fr.ens.transcriptome.eoulsan.util.ServiceNameLoader;

/**
 * This class allow to get a Step object.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class StepService extends ServiceNameLoader<Step> {

  /** Logger. */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  private static StepService service;

  //
  // Static method
  //

  /**
   * Retrieve the singleton static instance of StepService.
   * @return A StepService instance
   */
  public static synchronized StepService getInstance() {

    if (service == null) {
      service = new StepService();
    }

    return service;
  }

  //
  // Protected methods
  //

  @Override
  protected boolean accept(final Class<?> clazz) {

    return EoulsanMode
        .accept(clazz, EoulsanRuntime.getRuntime().isHadoopMode());
  }

  @Override
  protected String getMethodName() {

    return "getName";
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */

  private StepService() {

    super(Step.class);

    for (String stepName : getServiceClasses().keySet()) {

      LOGGER.config("found step: " + stepName + " (" + stepName + ")");
    }
  }

}
