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

package fr.ens.biologie.genomique.eoulsan.requirements;

import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.Progress;

/**
 * This interface define a requirement for an Eoulsan Step.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface Requirement {

  /**
   * The name of the requirement.
   * @return the name of the requirement
   */
  String getName();

  /**
   * Test if the requirement is optional.
   * @return true if he requirement is optional
   */
  boolean isOptional();

  /**
   * Test if the requirement is available.
   * @return true if he requirement is optional
   */
  boolean isAvailable();

  /**
   * Test if the requirement is installable.
   * @return true if the requirement is installable
   */
  boolean isInstallable();

  /**
   * Get the parameters of the requirement
   * @return a set of parameters
   */
  Set<Parameter> getParameters();

  /**
   * Configure the requirement.
   * @param parameters the parameters of the requirement
   * @throws EoulsanException if an error occurs while configuring the
   *           requirement
   */
  void configure(Set<Parameter> parameters) throws EoulsanException;

  /**
   * Install the requirement.
   * @param progress Progress object
   * @throws EoulsanException if the requirement cannot be installed
   */
  void install(Progress progress) throws EoulsanException;

}
