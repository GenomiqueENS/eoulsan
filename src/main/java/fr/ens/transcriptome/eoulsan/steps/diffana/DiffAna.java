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

package fr.ens.transcriptome.eoulsan.steps.diffana;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.r.RSConnectionNewImpl;

/**
 * This class create and launch a R script to compute differential analysis.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Vivien Deshaies
 */
public class DiffAna extends Normalization {

  @Override
  public String writeScript(final List<Sample> experimentSamplesList) {

    final Map<String, List<Integer>> conditionsMap = Maps.newHashMap();

    final List<Integer> rSampleIds = Lists.newArrayList();
    final List<String> rSampleNames = Lists.newArrayList();
    final List<String> rCondNames = Lists.newArrayList();
    final List<String> rRepTechGroup = Lists.newArrayList();
    int i = 0;
    // Create Rnw script stringbuilder
    final StringBuilder sb = new StringBuilder();

    for (Sample s : experimentSamplesList) {

      if (!s.getMetadata().isConditionField())
        throw new EoulsanException("No condition field found in design file.");

      final String condition = s.getMetadata().getCondition().trim();

      if ("".equals(condition))
        throw new EoulsanException("No value for condition in sample: "
            + s.getName() + " (" + s.getId() + ")");

      final String repTechGroup = s.getMetadata().getRepTechGroup().trim();

      if (!"".equals(repTechGroup)) {
        rRepTechGroup.add(repTechGroup);
      }

      final List<Integer> index;
      if (!conditionsMap.containsKey(condition)) {
        index = Lists.newArrayList();
        conditionsMap.put(condition, index);
      } else {
        index = conditionsMap.get(condition);
      }
      index.add(i);

      rSampleIds.add(s.getId());
      rSampleNames.add(s.getName());
      rCondNames.add(condition);

      i++;
    }

    // Add dispersion estimation part
//    if (isBiologicalReplicates(conditionsMap, rCondNames, rRepTechGroup)) {
//      sb.append(readStaticScript(DISPERSION_ESTIMATION_WITH_REPLICATES));
//    } else {
//      sb.append(readStaticScript(DISPERSION_ESTIMATION_WITHOUT_REPLICATES));
//    }
//
//    if (isReference(experimentSamplesList)) {
//      sb.append(readStaticScript(KINETIC_ANADIFF));
//    } else {
//      sb.append(readStaticScript(NOT_KINETIC_ANADIFF));
//    }

    return null;
  }

  /**
   * Determine if there is biological replicates in an experiment
   * @param conditionsMap
   * @param rCondNames
   * @param rRepTechGroup
   * @return a boolean
   */
  private boolean isBiologicalReplicates(
      Map<String, List<Integer>> conditionsMap, List<String> rCondNames,
      List<String> rRepTechGroup) {

    for (String condition : rCondNames) {
      List<Integer> condPos = conditionsMap.get(condition);

      for (int i = 0; i < condPos.size() - 1; i++) {
        int pos1 = condPos.get(i);
        int pos2 = condPos.get(i + 1);
        if (!rRepTechGroup.get(pos1).equals(rRepTechGroup.get(pos2))) {
          return true;
        }
      }
    }
    return false;
  }

  /*
   * Constructor
   */

  /**
   * Public constructor
   * @param design
   * @param expressionFilesDirectory
   * @param expressionFilesPrefix
   * @param expressionFilesSuffix
   * @param outPath
   * @param rServerName
   */
  public DiffAna(Design design, File expressionFilesDirectory,
      String expressionFilesPrefix, String expressionFilesSuffix, File outPath,
      String rServerName) {
    super(design, expressionFilesDirectory, expressionFilesPrefix,
        expressionFilesSuffix, outPath, rServerName);
  }

}
