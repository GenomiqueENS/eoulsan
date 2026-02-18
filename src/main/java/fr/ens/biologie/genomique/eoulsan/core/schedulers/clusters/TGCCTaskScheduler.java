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

package fr.ens.biologie.genomique.eoulsan.core.schedulers.clusters;

import java.io.File;
import java.nio.file.Path;

/**
 * This class define a TGCC cluster scheduler. This scheduler is based on the
 * Bpipe slurm script.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TGCCTaskScheduler extends BpipeTaskScheduler {

  public static final String SCHEDULER_NAME = "tgcc";
  private static final String BPIPE_SCRIPT_PATH =
      "/ccc/cont007/home/fg/fg/products/bpipe-0.9.8.6/bin/bpipe-slurm.sh";

  @Override
  public String getSchedulerName() {

    return SCHEDULER_NAME;
  }

  @Override
  protected File getBpipeCommandWrapper() {

    return Path.of(BPIPE_SCRIPT_PATH).toFile();
  }

}
