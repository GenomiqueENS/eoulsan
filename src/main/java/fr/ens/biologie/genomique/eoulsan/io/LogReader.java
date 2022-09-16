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

package fr.ens.biologie.genomique.eoulsan.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.kenetre.io.FileUtils;
import fr.ens.biologie.genomique.kenetre.util.LocalReporter;
import fr.ens.biologie.genomique.kenetre.util.Reporter;

/**
 * This class define a log reader that store log data in a Reporter object
 * @since 1.0
 * @author Laurent Jourdren
 */
public class LogReader {

  /* Default Charset. */
  private static final Charset CHARSET =
      Charset.forName(Globals.DEFAULT_FILE_ENCODING);

  private final BufferedReader reader;

  /**
   * Read a log file.
   * @return a reporter object
   * @throws IOException if an error occurs while reading data
   */
  public Reporter read() throws IOException {

    final LocalReporter result = new LocalReporter();

    String line = null;
    String counterGroup = null;

    while ((line = this.reader.readLine()) != null) {

      final String tLine = line.trim();

      if ("".equals(tLine)
          || tLine.startsWith("Start time:") || tLine.startsWith("End time:")
          || tLine.startsWith("Duration:")) {
        continue;
      }

      if (line.startsWith("\t")) {

        if (counterGroup == null) {
          continue;
        }

        final int separatorIndex = tLine.indexOf('=');
        if (separatorIndex == -1) {
          continue;
        }
        final String counter = tLine.substring(0, separatorIndex);

        try {
          final int value =
              Integer.parseInt(tLine.substring(separatorIndex + 1));

          result.setCounter(counterGroup, counter, value);
        } catch (NumberFormatException e) {
          continue;
        }
      } else {
        counterGroup = line;
      }

    }

    this.reader.close();

    return result;
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public LogReader(final InputStream is) {

    if (is == null) {
      throw new NullPointerException("InputStream is null");
    }

    this.reader = new BufferedReader(new InputStreamReader(is, CHARSET));
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public LogReader(final File file) throws FileNotFoundException {

    if (file == null) {
      throw new NullPointerException("File is null");
    }

    if (!file.isFile()) {
      throw new FileNotFoundException(
          "File not found: " + file.getAbsolutePath());
    }

    this.reader = FileUtils.createBufferedReader(file);
  }

}
