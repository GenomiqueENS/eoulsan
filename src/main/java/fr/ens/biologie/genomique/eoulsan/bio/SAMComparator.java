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

package fr.ens.biologie.genomique.eoulsan.bio;

import java.io.Serializable;
import java.util.Comparator;

import htsjdk.samtools.SAMRecord;

/**
 * This class is a Comparator for SAM records.
 * @since 1.2
 * @author Claire Wallon
 */
public class SAMComparator implements Comparator<SAMRecord>, Serializable {

  private static final long serialVersionUID = -3948514532737476599L;

  @Override
  public int compare(final SAMRecord r0, final SAMRecord r1) {

    if (r0 == null) {
      throw new NullPointerException("r0 argument is null in SAMComparator");
    }

    if (r1 == null) {
      throw new NullPointerException("r1 argument is null in SAMComparator");
    }

    int comp;

    // Compare the two read names
    comp = r0.getReadName().compareTo(r1.getReadName());

    if (comp == 0) {
      // Compare the mapping quality scores
      comp = r0.getMappingQuality() - r1.getMappingQuality();

      if (comp == 0) {
        // Compare the reference names (chromosomes)
        comp = r0.getReferenceName().compareTo(r1.getReferenceName());

        if (comp == 0) {

          // paired-end mode
          if (r0.getReadPairedFlag()) {

            // the two alignments to compare are a pair
            if (r0.getAlignmentStart() == r1.getMateAlignmentStart()
                && r1.getAlignmentStart() == r0.getMateAlignmentStart()) {
              comp = r0.getFlags() - r1.getFlags();
            }

            // the two alignments to compare are part of different pairs
            else {

              if (r0.getFirstOfPairFlag() && r1.getFirstOfPairFlag()) {
                comp = r0.getAlignmentStart() - r1.getAlignmentStart();
                if (comp == 0) {
                  comp =
                      r0.getMateAlignmentStart() - r1.getMateAlignmentStart();
                }
              }

              else if (!r0.getFirstOfPairFlag() && !r1.getFirstOfPairFlag()) {
                comp = r0.getMateAlignmentStart() - r1.getMateAlignmentStart();
                if (comp == 0) {
                  comp = r0.getAlignmentStart() - r1.getAlignmentStart();
                }
              }

              else if (r0.getFirstOfPairFlag() && !r1.getFirstOfPairFlag()) {
                comp = r0.getAlignmentStart() - r1.getMateAlignmentStart();
                if (comp == 0) {
                  comp = r0.getMateAlignmentStart() - r1.getAlignmentStart();
                }
              }

              else {
                comp = r0.getMateAlignmentStart() - r1.getAlignmentStart();
                if (comp == 0) {
                  comp = r0.getAlignmentStart() - r1.getMateAlignmentStart();
                }
              }

              if (comp == 0) {
                // Compare the CIGAR code
                comp = r0.getCigarString().compareTo(r1.getCigarString());
              }

            }
          }

          // single-end mode
          else {

            // Compare the 1-based leftmost mapping position
            comp = r0.getAlignmentStart() - r1.getAlignmentStart();

            if (comp == 0) {
              // Compare the end position of the alignment
              comp = r0.getAlignmentEnd() - r1.getAlignmentEnd();

              if (comp == 0) {
                // Compare the CIGAR code
                comp = r0.getCigarString().compareTo(r1.getCigarString());
              }
            }
          }

        }
      }
    }

    return comp;
  }

}
