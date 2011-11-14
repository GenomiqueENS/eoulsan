/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.illumina;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import fr.ens.transcriptome.eoulsan.util.Utils;

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

  @Override
  public String toString() {

    return this.getClass().getSimpleName() + "{samples=" + this.samples + "}";
  }

}
