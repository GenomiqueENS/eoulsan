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

package fr.ens.biologie.genomique.eoulsan;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.logging.FileHandler;
import java.util.logging.Handler;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 * Main class in local mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class MainCLI extends Main {

  private static final String LAUNCH_MODE_NAME = "local";

  /**
   * Create options for command line
   * @return an Options object
   */
  @Override
  @SuppressWarnings("static-access")
  protected Options makeOptions() {

    // create Options object
    final Options options = super.makeOptions();

    // eoulsan.sh shell script options
    options.addOption(OptionBuilder.withArgName("path").hasArg()
        .withDescription("JAVA_HOME path").create('j'));

    options.addOption(OptionBuilder.withArgName("size").hasArg()
        .withDescription("maximal memory usage for JVM in MB (4096 by default)")
        .create('m'));

    options.addOption(OptionBuilder.withArgName("args").hasArg()
        .withDescription("JVM arguments (-server by default)").create('J'));

    options.addOption(OptionBuilder.withArgName("path").hasArg()
        .withDescription("JVM working directory").create('w'));

    options.addOption(OptionBuilder.withArgName("classpath").hasArg()
        .withDescription("additional classpath for eoulsan plugins")
        .create('p'));

    return options;
  }

  @Override
  protected void initializeRuntime(final Settings settings) {

    LocalEoulsanRuntime.newEoulsanRuntime(settings);
  }

  @Override
  protected String getHelpEoulsanCommand() {

    return Globals.APP_NAME_LOWER_CASE;
  }

  @Override
  protected Handler getLogHandler(final URI logFile) throws IOException {

    if (logFile == null) {
      throw new NullPointerException("The log file is null");
    }

    final File file = new File(logFile);
    final File parentFile = file.getParentFile();

    // Create parent directory if necessary
    if (parentFile != null && !parentFile.exists()) {
      if (!parentFile.mkdirs()) {
        throw new IOException("Unable to create directory "
            + parentFile + " for log file:" + logFile);
      }
    }

    return new FileHandler(file.getAbsolutePath());
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param args command line arguments
   */
  protected MainCLI(final String[] args) {

    super(LAUNCH_MODE_NAME, args);
  }

}
