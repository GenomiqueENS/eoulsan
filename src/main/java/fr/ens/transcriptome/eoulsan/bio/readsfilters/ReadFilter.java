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

import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

/**
 * This interface define a filter for reads.
 * @author jourdren
 */
public interface ReadFilter {

  /**
   * Tests if a specified read should be keep.
   * @param read read to test
   */
  boolean accept(ReadSequence read);

  /**
   * Tests if the specified reads should be keep.
   * @param read1 first read to test
   * @param read2 first read to test
   */
  boolean accept(ReadSequence read1, ReadSequence read2);

  /**
   * Get the name of the filter.
   * @return the name of the filter
   */
  String getName();

}
