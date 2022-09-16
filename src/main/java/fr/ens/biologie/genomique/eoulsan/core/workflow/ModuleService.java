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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.annotations.ExecutionMode;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.kenetre.util.ServiceNameLoader;

/**
 * This class allow to get a Step object from a class in the classpath.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class ModuleService extends ServiceNameLoader<Module> {

  //
  // Protected methods
  //

  @Override
  protected boolean accept(final Class<?> clazz) {

    if (EoulsanRuntime.getRuntime().getMode().isHadoopMode()) {
      return true;
    }

    return ExecutionMode.accept(clazz, false);
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

  ModuleService() {

    super(Module.class);
  }

}
