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

package net.sf.samtools;

import java.util.Comparator;

/**
 * This class is a Comparator for SAM records
 * @since 1.2
 * @author Claire Wallon
 */
public class SAMComparator implements Comparator<SAMRecord> {

  @Override
  public int compare(SAMRecord r0, SAMRecord r1) {

    int comp;

    // Compare the two read names
    comp = r0.getReadName().compareTo(r1.getReadName());

    // Compare the reference names (chromosomes)
    if (comp == 0) {
      comp = r0.getReferenceName().compareTo(r1.getReferenceName());

      // For paired-end reads, compare the start position of the first alignment
      // of the pair
      if (comp == 0 && r0.getReadPairedFlag()) {
        Integer start0 = new Integer(r0.getAlignmentStart());
        Integer start1 = new Integer(r1.getMateAlignmentStart());
        comp = start0.compareTo(start1);
      }

      // Compare the alignment flags
      if (comp == 0) {
        Integer flag0 = new Integer(r0.getFlags());
        Integer flag1 = new Integer(r1.getFlags());
        comp = flag0.compareTo(flag1);
      }
    }

    return comp;
  }

}
