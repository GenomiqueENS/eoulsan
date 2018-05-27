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

package fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters;

import java.util.List;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import htsjdk.samtools.SAMRecord;

/**
 * This interface define a filter for alignments.
 * @since 1.1
 * @author Laurent Jourdren
 */
public interface ReadAlignmentsFilter {

  /**
   * Filter a list of alignments from a list of alignment of one unique read.
   * All the read id in the records are the same. The input list is modified
   * after the call of the method.
   * @param records the list of alignments with the same read name
   */
  void filterReadAlignments(List<SAMRecord> records);

  /**
   * Get the name of the filter.
   * @return the name of the filter
   */
  String getName();

  /**
   * Get the description of the filter.
   * @return the description of the filter
   */
  String getDescription();

  /**
   * Set a parameter of the filter.
   * @param key name of the parameter to set
   * @param value value of the parameter to set
   * @throws EoulsanException if the parameter is invalid
   */
  void setParameter(String key, String value) throws EoulsanException;

  /**
   * Initialize the filter.
   * @throws EoulsanException an error occurs while initialize the filter
   */
  void init() throws EoulsanException;

}
