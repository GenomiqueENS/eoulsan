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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import fr.ens.biologie.genomique.eoulsan.util.ServiceNameLoader;

/**
 * This class define a service to retrieve a ReadFilter.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class ReadFilterService extends ServiceNameLoader<ReadFilter> {

  private static ReadFilterService service;

  //
  // Static method
  //

  /**
   * Retrieve the singleton static instance of an ReadFilterService.
   * @return A ActionService instance
   */
  public static synchronized ReadFilterService getInstance() {

    if (service == null) {
      service = new ReadFilterService();
    }

    return service;
  }

  //
  // Protected methods
  //

  @Override
  protected boolean accept(final Class<?> clazz) {

    return true;
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
  private ReadFilterService() {
    super(ReadFilter.class);
  }

}
