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

package fr.ens.transcriptome.eoulsan;

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.StreamHandler;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * Main class in Hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class MainHadoop extends Main {

  private static final String LAUNCH_MODE_NAME = "hadoop";

  @Override
  protected void initializeRuntime(final Settings settings) {

    HadoopEoulsanRuntime.newEoulsanRuntime(settings);
  }

  @Override
  protected String getHelpEoulsanCommand() {

    return "hadoop jar " + Globals.APP_NAME_LOWER_CASE + ".jar";
  }

  @Override
  protected Handler getLogHandler(final String logFile) throws IOException {

    final Configuration conf =
        ((HadoopEoulsanRuntime) EoulsanRuntime.getRuntime()).getConfiguration();

    final Path loggerPath = new Path(logFile);

    final FileSystem loggerFs = loggerPath.getFileSystem(conf);

    return new StreamHandler(loggerFs.create(loggerPath), Globals.LOG_FORMATTER);
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param args command line arguments
   */
  protected MainHadoop(final String[] args) {

    super(LAUNCH_MODE_NAME, args);
  }

}
