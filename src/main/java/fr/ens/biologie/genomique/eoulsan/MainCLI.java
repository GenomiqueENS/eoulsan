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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Handler;

import org.apache.commons.cli.Option;
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
    options.addOption(Option.builder("j").argName("path").hasArg()
        .desc("JAVA_HOME path").get());

    options.addOption(Option.builder("m").argName("size").hasArg()
        .desc("maximal memory usage for JVM in MB (4096 by default)").get());

    options.addOption(Option.builder("J").argName("args").hasArg()
        .desc("JVM arguments (-server by default)").get());

    options.addOption(Option.builder("w").argName("path").hasArg()
        .desc("JVM working directory").get());

    options.addOption(Option.builder("p").argName("classpath").hasArg()
        .desc("additional classpath for eoulsan plugins").get());

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

    final Path file = Path.of(logFile);
    final Path parentFile = file.getParent();

    // Create parent directory if necessary
    if (parentFile != null && !Files.exists(parentFile)) {

      try {
        Files.createDirectories(parentFile);
      } catch (IOException e) {
        throw new IOException("Unable to create directory "
            + parentFile + " for log file:" + logFile);
      }
    }

    return new FileHandler(file.toAbsolutePath().toString());
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param args command line arguments
   */
  MainCLI(final String[] args) {

    super(LAUNCH_MODE_NAME, args);
  }

}
