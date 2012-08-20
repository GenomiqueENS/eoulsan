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
 * This class define a read filter that check if the two reads of a pair came
 * from the same cluster. <b>Warning:</b> this class has not been update for Illumina id
 * generated with Casava 1.8.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class PairCheckReadFilter extends AbstractReadFilter {

  @Override
  public boolean accept(final ReadSequence read1, final ReadSequence read2) {

    if (read1 == null)
      return false;

    if (read2 == null)
      return false;

    final String id1 = read1.getName();
    final String id2 = read2.getName();

    if (id1 == null)
      return false;

    if (id2 == null)
      return false;

    if (id1.equals(id2))
      return false;

    final int len1 = id1.length();
    final int len2 = id2.length();

    if (len1 != len2)
      return false;

    if (!id1.endsWith("/1"))
      return false;

    if (!id2.endsWith("/2"))
      return false;

    final int len = len1 - 2;
    final String prefix1 = id1.substring(0, len);
    final String prefix2 = id2.substring(0, len);

    return prefix1.equals(prefix2);
  }

  @Override
  public boolean accept(ReadSequence read) {

    if (read == null)
      return false;

    return true;
  }

  @Override
  public String getName() {

    return "paircheck";
  }

  @Override
  public String getDescription() {
    return "Pair check read filter";
  }

  @Override
  public void setParameter(String key, String value) {
    // this filter has no parameter
  }
  
  @Override
  public String toString() {

    return this.getClass().getSimpleName() + "{}";
  }

}
