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

package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import java.util.Iterator;
import java.util.ServiceLoader;

public class SequenceReadsMapperService {

  private static SequenceReadsMapperService service;
  private final ServiceLoader<SequenceReadsMapper> loader;

  //
  // Static method
  //

  /**
   * Retrieve the singleton static instance of StepService.
   * @return A StepService instance
   */
  public static synchronized SequenceReadsMapperService getInstance() {

    if (service == null) {
      service = new SequenceReadsMapperService();
    }

    return service;
  }

  //
  // Instance methods
  //

  /**
   * Get SequenceReadsMapper.
   * @param mapperName name of the mapper to get
   * @return a SequenceReadsMapper
   */
  public SequenceReadsMapper getMapper(final String mapperName) {

    if (mapperName == null) {
      return null;
    }

    final String mapperNameLower = mapperName.toLowerCase();

    final Iterator<SequenceReadsMapper> it = this.loader.iterator();

    while (it.hasNext()) {

      final SequenceReadsMapper mapper = it.next();

      if (mapperNameLower.equals(mapper.getMapperName().toLowerCase())) {
        return mapper;
      }
    }

    return null;
  }

  //
  // Constructor
  //

  /**
   * Private protocol.
   */
  private SequenceReadsMapperService() {

    loader = ServiceLoader.load(SequenceReadsMapper.class);
  }

}
