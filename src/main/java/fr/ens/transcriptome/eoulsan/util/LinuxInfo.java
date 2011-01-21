package fr.ens.transcriptome.eoulsan.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.Globals;

/**
 * This class define a linux info file parser
 * @author Laurent Jourdren
 */
public abstract class LinuxInfo {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private Map<String, String> map = Maps.newHashMap();

  protected abstract File getInfoFile();

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

      LOGGER
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
