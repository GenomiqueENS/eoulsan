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

import fr.ens.biologie.genomique.eoulsan.Main;

/**
 * This class define a Dummy cluster scheduler.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DummyTaskScheduler extends BpipeTaskScheduler {

  private static final String COMMAND_WRAPPER_SCRIPT = "bpipe-dummy";

  private final Path commandWrapperFile;

  @Override
  public String getSchedulerName() {

    return "dummy";
  }

  @Override
  protected File getBpipeCommandWrapper() {

    return this.commandWrapperFile.toFile();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   */
  public DummyTaskScheduler() {

    final Path binDir =
        Main.getInstance().getEoulsanDirectory().toPath().resolve("bin");

    this.commandWrapperFile = binDir.resolve(COMMAND_WRAPPER_SCRIPT);
  }

}
