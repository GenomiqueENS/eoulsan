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

import static fr.ens.transcriptome.eoulsan.util.StringUtils.isNullOrEmpty;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.util.Utils;

public final class CasavaDesignUtil {

  public static boolean checkCasavaDesign(final CasavaDesign design)
      throws EoulsanException {

    if (design == null)
      return false;

    String fcid = null;
    boolean first = true;

    final Map<Integer, Set<String>> indexes = Utils.newHashMap();
    final Set<String> sampleIds = Utils.newHashSet();

    for (CasavaSample sample : design) {

      // Check if all the fields are not empty
      checkFCID(sample.getFlowCellId());

      // Check if all the samples had the same flow cell id
      if (first) {
        fcid = sample.getFlowCellId();
        first = false;
      } else {

        if (!fcid.equals(sample.getFlowCellId()))
          throw new EoulsanException("Two differents flow cell id found: "
              + fcid + " and " + sample.getFlowCellId() + ".");
      }

      // Check the lane number
      if (sample.getLane() < 1 || sample.getLane() > 8)
        throw new EoulsanException("Invalid lane number found: "
            + sample.getLane() + ".");

      // Check if the sample is null or empty
      checkSampleId(sample.getSampleId(), sampleIds);

      // Check sample reference
      if (isNullOrEmpty(sample.getSampleRef()))
        throw new EoulsanException("Found a null or empty sample reference.");

      // Check index
      checkIndex(sample.getIndex());

      // Check the description
      if (isNullOrEmpty(sample.getDescription()))
        throw new EoulsanException("Found a null or empty description.");

      // Check recipe
      if (isNullOrEmpty(sample.getRecipe()))
        throw new EoulsanException("Found a null or empty recipe.");

      // Check operator
      if (isNullOrEmpty(sample.getOperator()))
        throw new EoulsanException("Found a null or empty operator.");

      // Check sample project
      checkSampleProject(sample.getSampleProject());

      final String index = sample.getIndex();
      final int lane = sample.getLane();

      // check if a lane has not two or more same indexes
      if (indexes.containsKey(lane)) {

        if (indexes.get(lane).contains(index))
          return false;

      } else
        indexes.put(lane, new HashSet<String>());

      indexes.get(lane).add(index);
    }

    return true;
  }

  private static void checkFCID(final String fcid) throws EoulsanException {

    if (isNullOrEmpty(fcid))
      throw new EoulsanException("Flow cell id is null or empty.");

    for (int i = 0; i < fcid.length(); i++) {

      final int c = fcid.codePointAt(i);
      if (!(Character.isLetterOrDigit(c)))
        throw new EoulsanException(
            "Invalid flow cell id, only letters or digits are allowed : "
                + fcid + ".");
    }

  }

  private static void checkSampleId(final String sampleId,
      final Set<String> sampleIds) throws EoulsanException {

    // Check if null of empty
    if (isNullOrEmpty(sampleId))
      throw new EoulsanException("Found a null or empty sample id.");

    // Check for forbidden characters
    for (int i = 0; i < sampleId.length(); i++) {

      final int c = sampleId.codePointAt(i);
      if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-'))
        throw new EoulsanException(
            "Invalid sample id, only letters, digits, '-' or '_' characters are allowed : "
                + sampleId + ".");
    }

    // Check if the sample has been already defined
    if (sampleIds.contains(sampleIds))
      throw new EoulsanException("The sample id \""
          + sampleId + "\" has been define more than one time.");
    sampleIds.add(sampleId);
  }

  private static void checkIndex(final String index) throws EoulsanException {

    if (index == null)
      return;

    for (int i = 0; i < index.length(); i++)
      switch (index.codePointAt(i)) {

      case 'A':
      case 'a':
      case 'T':
      case 't':
      case 'G':
      case 'g':
      case 'C':
      case 'c':
        break;

      default:
        throw new EoulsanException("Invalid index found: " + index + ".");
      }
  }

  private static void checkSampleProject(final String sampleProject)
      throws EoulsanException {

    if (isNullOrEmpty(sampleProject))
      throw new EoulsanException("Found a null or sample project.");

    for (int i = 0; i < sampleProject.length(); i++)
      if (!Character.isLetterOrDigit(sampleProject.codePointAt(i)))
        throw new EoulsanException(
            "Invalid sample project, only letters or digits are allowed: "
                + sampleProject + ".");

  }
}
