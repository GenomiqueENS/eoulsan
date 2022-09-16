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

package fr.ens.biologie.genomique.eoulsan.util;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fr.ens.biologie.genomique.kenetre.io.FileUtils;

/**
 * This class define a linux info file parser.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class LinuxInfo {

  private final Map<String, String> map = new HashMap<>();

  /**
   * Get the file to parse.
   * @return the file to parse
   */
  public abstract File getInfoFile();

  protected void parse() {

    try {

      final BufferedReader br = FileUtils.createBufferedReader(getInfoFile());

      String line = null;

      while ((line = br.readLine()) != null) {

        String[] fields = line.split(":");

        if (fields.length > 1) {
          this.map.put(fields[0].trim(), fields[1].trim());
        }

      }

      br.close();
    } catch (IOException e) {

      getLogger()
          .warning("unable to parse " + getInfoFile() + ": " + e.getMessage());
    }
  }

  /**
   * Get the value for a key
   * @param key key
   * @return the value for the key
   */
  public String get(final String key) {

    return this.map.get(key);
  }

}
