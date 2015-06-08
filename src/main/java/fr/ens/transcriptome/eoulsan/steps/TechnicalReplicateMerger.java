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

package fr.ens.transcriptome.eoulsan.steps;

import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.annotations.ReuseStepInstance;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.design.SampleMetadata;

/**
 * This class define a merger step for technical replicates.
 * @author Laurent Jourdren
 * @since 2.0
 */
@HadoopCompatible
@ReuseStepInstance
public class TechnicalReplicateMerger extends MergerStep {

  public static final String STEP_NAME = "technicalreplicatemerger";

  //
  // Protected method
  //

  @Override
  protected String getMapKey(final Data data) {

    return data.getMetadata().get(SampleMetadata.REP_TECH_GROUP_KEY);
  }

  @Override
  protected boolean checkForPartDuplicates() {

    return false;
  }

  //
  // Step methods
  //

  @Override
  public String getName() {

    return STEP_NAME;
  }

}
