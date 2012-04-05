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

package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMRecord;

/**
 * This alignment filter remove all the unmapped alignments.
 * @since 1.1
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
public class RemoveUnmappedReadAlignmentsFilter extends
    AbstractReadAlignmentsFilter {

  private final List<SAMRecord> result = new ArrayList<SAMRecord>();

  @Override
  public String getName() {

    return "removeunmapped";
  }

  @Override
  public String getDescription() {

    return "Remove all the unmapped alignments";
  }

  @Override
  public void filterReadAlignments(final List<SAMRecord> records) {

    if (records == null)
      return;

    // single-end mode
    if (!records.get(0).getReadPairedFlag()) {
      for (SAMRecord r : records)
        
        // storage in 'result' of records that do not pass the filter
        if (r.getReadUnmappedFlag())
          this.result.add(r);
    }
    
    // paired-end mode
    else {
      for (int counterRecord = 0; counterRecord < records.size()-1; 
          counterRecord += 2) {
        
        // storage in 'result' of records that do not pass the filter
        if (records.get(counterRecord).getReadUnmappedFlag() ||
            records.get(counterRecord+1).getReadUnmappedFlag()) {
          
          // records are stored 2 by 2 because of the paired-end mode
          this.result.add(records.get(counterRecord));
          this.result.add(records.get(counterRecord+1));
        }
      }
    }
    
    // all records that do not pass the filter are removed
    records.removeAll(result);
    result.clear();
  }

}
