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

package fr.ens.biologie.genomique.eoulsan.checkers;

import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;

/**
 * This interface define a checker.
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface Checker {

  /**
   * Get the name of the checker.
   * @return the name of the checker
   */
  String getName();

  /**
   * Test if the Checker is a design checker
   */
  boolean isDesignChecker();

  /**
   * Get format related to the checker.
   * @return a DataFormat object
   */
  DataFormat getFormat();

  /**
   * Set the parameters of the checker to configure the checker.
   * @param stepParameters parameters of the step
   * @throws EoulsanException if a parameter is invalid
   */
  void configure(Set<Parameter> stepParameters) throws EoulsanException;

  /**
   * Launch the check.
   * @param data data to check
   * @param checkInfo object that contains data shared between the checkers
   * @throws EoulsanException if an error occurs while executing step
   */
  boolean check(Data data, CheckStore checkInfo) throws EoulsanException;

  /**
   * Get the list of Checker required to run before this checker.
   * @return a list of DataFormat that are checked by the required checkers
   */
  Set<DataFormat> getCheckersRequired();

}
