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

package fr.ens.transcriptome.eoulsan.bio.readsfilters;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * This class define a service to retrieve a ReadFilter.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class ReadFilterService {

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
  // Instance methods
  //

  /**
   * Get a ReadFilter object.
   * @param readFilterName name of the filter to get
   * @return an Action
   */
  public ReadFilter getReadFilter(final String readFilterName) {

    if (readFilterName == null) {
      return null;
    }

    final String actionNameLower = readFilterName.toLowerCase();

    final Iterator<ReadFilter> it =
        ServiceLoader.load(ReadFilter.class).iterator();

    while (it.hasNext()) {

      final ReadFilter filter = it.next();

      if (actionNameLower.equals(filter.getName().toLowerCase())) {
        return filter;
      }
    }

    return null;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private ReadFilterService() {
  }

}
