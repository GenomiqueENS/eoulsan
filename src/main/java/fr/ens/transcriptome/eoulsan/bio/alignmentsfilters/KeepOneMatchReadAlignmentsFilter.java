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

import java.util.List;

import fr.ens.transcriptome.eoulsan.EoulsanException;

import net.sf.samtools.SAMRecord;

/**
 * This alignments filter keep only one alignment for a read. This filter is
 * useful to count the number of reads that can match on the genome.
 * @since 1.1
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
public class KeepOneMatchReadAlignmentsFilter extends
    AbstractReadAlignmentsFilter {
  
  public static final String FILTER_NAME = "keeponematch";
  // TODO Claire : Why keep is not used ?
  private boolean keep = false;

  @Override
  public String getName() {
    return "keeponematch";
  }

  @Override
  public String getDescription() {
    return "After this filter only one alignment is keeped by read";
  }

  @Override
  public void filterReadAlignments(final List<SAMRecord> records) {
    
    final SAMRecord first, second;
    
    if (records == null)
      return;
    
    // single-end mode
    if (!records.get(0).getReadPairedFlag()) {
      if (records.size() < 2)
        return;
      
      first = records.get(0);
      records.clear();
      records.add(first);
    }
    
    // paired-end mode
    else {
      if (records.size() < 4)
        return;
      
      first = records.get(0);
      second = records.get(1);
      records.clear();
      records.add(first);
      records.add(second);
    }
  }
  
  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    if (key == null || value == null)
      return;

    if ("keep".equals(key.trim())) {

      try {
        this.keep = Boolean.parseBoolean(value.trim());
      } catch (NumberFormatException e) {
        return;
      }

    } else

      throw new EoulsanException("Unknown parameter for "
          + getName() + " alignments filter: " + key);
  }

}
