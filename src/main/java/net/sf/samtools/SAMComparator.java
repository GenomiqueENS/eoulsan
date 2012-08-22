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
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;

/**
 * This class is a Comparator for SAM records.
 * @since 1.2
 * @author Claire Wallon
 */
public class SAMComparator implements Comparator<SAMRecord> {
  
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  @Override
  public int compare(SAMRecord r0, SAMRecord r1) {

    int comp;

    // Compare the two read names
    comp = r0.getReadName().compareTo(r1.getReadName());

    if (comp == 0) {
      // Compare the mapping quality scores
//      Integer score0 = new Integer(r0.getMappingQuality());
//      Integer score1 = new Integer(r1.getMappingQuality());
      
      // A VERIFIER 
      comp = r0.getMappingQuality()-r1.getMappingQuality();

      if (comp == 0) {
        // Compare the reference names (chromosomes)
        comp = r0.getReferenceName().compareTo(r1.getReferenceName());

        if (comp == 0) {

          // paired-end mode
          if (r0.getReadPairedFlag()) {

            // the two alignments to compare are a pair
            if (r0.getAlignmentStart() == r1.getMateAlignmentStart()
                && r1.getAlignmentStart() == r0.getMateAlignmentStart()) {
              Integer flag0 = new Integer(r0.getFlags());
              Integer flag1 = new Integer(r1.getFlags());
              comp = flag0.compareTo(flag1);
            }

            // the two alignments to compare are part of different pairs
            else {

              if (r0.getFirstOfPairFlag() && r1.getFirstOfPairFlag()) {
                Integer start0 = new Integer(r0.getAlignmentStart());
                Integer start1 = new Integer(r1.getAlignmentStart());
                comp = start0.compareTo(start1);
                if (comp == 0) {
                  Integer mateStart0 = new Integer(r0.getMateAlignmentStart());
                  Integer mateStart1 = new Integer(r1.getMateAlignmentStart());
                  comp = mateStart0.compareTo(mateStart1);
                }
              }

              else if (!r0.getFirstOfPairFlag() && !r1.getFirstOfPairFlag()) {
                Integer mateStart0 = new Integer(r0.getMateAlignmentStart());
                Integer mateStart1 = new Integer(r1.getMateAlignmentStart());
                comp = mateStart0.compareTo(mateStart1);
                if (comp == 0) {
                  Integer start0 = new Integer(r0.getAlignmentStart());
                  Integer start1 = new Integer(r1.getAlignmentStart());
                  comp = start0.compareTo(start1);
                }
              }

              else if (r0.getFirstOfPairFlag() && !r1.getFirstOfPairFlag()) {
                Integer start0 = new Integer(r0.getAlignmentStart());
                Integer mateStart1 = new Integer(r1.getMateAlignmentStart());
                comp = start0.compareTo(mateStart1);
                if (comp == 0) {
                  Integer mateStart0 = new Integer(r0.getMateAlignmentStart());
                  Integer start1 = new Integer(r1.getAlignmentStart());
                  comp = mateStart0.compareTo(start1);
                }
              }

              else {
                Integer mateStart0 = new Integer(r0.getMateAlignmentStart());
                Integer start1 = new Integer(r1.getAlignmentStart());
                comp = mateStart0.compareTo(start1);
                if (comp == 0) {
                  Integer start0 = new Integer(r0.getAlignmentStart());
                  Integer mateStart1 = new Integer(r1.getMateAlignmentStart());
                  comp = start0.compareTo(mateStart1);
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
            Integer start0 = new Integer(r0.getAlignmentStart());
            Integer start1 = new Integer(r1.getAlignmentStart());
            comp = start0.compareTo(start1);

            if (comp == 0) {
              // Compare the end position of the alignment
              Integer end0 = new Integer(r0.getAlignmentEnd());
              Integer end1 = new Integer(r1.getAlignmentEnd());
              comp = end0.compareTo(end1);

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
