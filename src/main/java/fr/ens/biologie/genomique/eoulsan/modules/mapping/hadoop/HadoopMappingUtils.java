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

package fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;

/**
 * This class contains utility methods for the Hadoop mapping package classes.
 *
 * @author Laurent Jourdren
 * @since 1.2
 */
public class HadoopMappingUtils {

  private static final String PARAM_KEYS_LIST_SUFFIX = "*.list";

  /**
   * Add the parameters to an Hadoop job configuration.
   *
   * @param parameters parameters to add.
   * @param prefix prefix for the parameters
   * @param jobConf job configuration
   */
  static void addParametersToJobConf(
      final Map<String, String> parameters, final String prefix, final Configuration jobConf) {

    if (parameters == null || jobConf == null || prefix == null) {
      return;
    }

    // Add the parameter to the job configuration
    for (Map.Entry<String, String> e : parameters.entrySet()) {

      final String key = prefix + e.getKey();
      final String value = e.getValue();

      jobConf.set(key, value);
    }

    // Set the key with the list of parameters keys as a string
    jobConf.set(prefix + PARAM_KEYS_LIST_SUFFIX, Joiner.on(',').join(parameters.keySet()));
  }

  /**
   * Retrieve parameters from a job configuration
   *
   * @param jobConf job configuration
   * @param prefix prefix for the parameters
   * @return a ordered map (LinkedHashMap) with the parameters
   */
  static Map<String, String> jobConfToParameters(final Configuration jobConf, final String prefix) {

    final Map<String, String> result = new LinkedHashMap<>();

    if (jobConf == null || prefix == null) {
      return result;
    }

    // Get the list of parameters
    final String keys = jobConf.get(prefix + PARAM_KEYS_LIST_SUFFIX);
    if (keys == null) {
      return result;
    }

    // Fill the result map with the parameters keys and values
    for (String key : Splitter.on(',').omitEmptyStrings().trimResults().split(keys)) {

      final String value = jobConf.get(prefix + key);

      result.put(key, value == null ? "" : value);
    }

    return result;
  }

  /** Private Constructor. */
  private HadoopMappingUtils() {
    throw new IllegalStateException();
  }
}
