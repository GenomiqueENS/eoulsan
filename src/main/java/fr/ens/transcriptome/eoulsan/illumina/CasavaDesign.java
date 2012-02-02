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

package fr.ens.transcriptome.eoulsan.illumina;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class handle a Casava design object.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class CasavaDesign implements Iterable<CasavaSample> {

  private List<CasavaSample> samples = Utils.newArrayList();

  public void addSample(final CasavaSample sample) {

    this.samples.add(sample);
  }

  @Override
  public Iterator<CasavaSample> iterator() {

    return Collections.unmodifiableList(this.samples).iterator();
  }

  /**
   * Get all the samples of a lane.
   * @param laneId the lane of the samples
   * @return a list of the samples in the lane in the same order as the Casava
   *         design. Return null if the laneId < 1.
   */
  public List<CasavaSample> getSampleInLane(final int laneId) {

    if (laneId < 1)
      return null;

    final List<CasavaSample> result = new ArrayList<CasavaSample>();

    for (CasavaSample s : this.samples) {
      if (s.getLane() == laneId)
        result.add(s);
    }

    return result;
  }

  
  /**
   * Get the number of samples in the design.
   * @return the number of samples in the design
   */
  public int size() {
    
    return this.samples.size();
  }
  
  @Override
  public String toString() {

    return this.getClass().getSimpleName() + "{samples=" + this.samples + "}";
  }

}
