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
import java.util.logging.Logger;

import net.sf.samtools.SAMRecord;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;

/**
 * This class define a filter based on the quality of an alignment (SAM format).
 * @author Claire Wallon
 */
public class QualityReadAlignmentsFilter extends AbstractReadAlignmentsFilter {
  
  public static final String FILTER_NAME = "mappingQuality";
  private int qualityThreshold = -1;
  
  private final List<SAMRecord> result = new ArrayList<SAMRecord>();
  
  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  @Override
  public String getName() {
    return "mappingQuality";
  }

  @Override
  public String getDescription() {
    return "Quality score of the alignment filter";
  }
  
  @Override
  public void filterReadAlignments(List<SAMRecord> records) {
    
    if (records == null)
      return;
    
    for (SAMRecord r : records) {
      // storage in 'result' of records that do not pass the quality filter
      if (r.getMappingQuality() < this.qualityThreshold) {
        this.result.add(r);
      }
    }
    
    // all records stored in 'result' are removed from 'records' 
    records.removeAll(result);
    result.clear();
  }
  
  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {
    
    if (key == null || value == null)
      return;

    if ("threshold".equals(key.trim())) {

      try {
        this.qualityThreshold = Integer.parseInt(value.trim());
      } catch (NumberFormatException e) {
        return;
      }

      if (this.qualityThreshold < 0.0)
        throw new EoulsanException("Invalid qualityThreshold: "
            + qualityThreshold);
    } else

      throw new EoulsanException("Unknown parameter for "
          + getName() + " read filter: " + key);
  }
  
  @Override
  public void init() {

    if (this.qualityThreshold < 0.0)
      throw new IllegalArgumentException("Quality threshold is not set for "
          + getName() + " read alignments filter.");
  }
  
  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public QualityReadAlignmentsFilter() {
    
  }

}
