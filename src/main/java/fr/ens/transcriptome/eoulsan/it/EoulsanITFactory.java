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

package fr.ens.transcriptome.eoulsan.it;

import java.io.File;

import org.testng.annotations.Factory;

/**
 * This class define a factory that create integrative tests for Eoulsan.
 * @author Laurent Jourdren
 * @since 1.3
 */
public class EoulsanITFactory {

  // System property that contains the path to the tests configuration
  public static final String TESTS_PATH_SYSTEM_PROPERTY =
      "fr.ens.transcriptome.validation.tests.path";

  private final File testConfigurationFile;

  /**
   * Create the integrative tests.
   * @return an array with the integrative tests to execute
   */
  @Factory
  public Object[] createInstances() {

    // If no test configuration path defined, do nothing
    if (this.testConfigurationFile == null)
      return new Object[0];

    // For the demo, create 10 integrative tests
    final int testNumber = 10;
    Object[] result = new Object[testNumber];

    for (int i = 0; i < testNumber; i++) {
      result[i] = new EoulsanIT(i * 10);
    }

    return result;
  }

  //
  // Constructors
  //

  /**
   * This constructor is called by maven. The tests configuration file must be
   * define in a Java system property.
   */
  public EoulsanITFactory() {

    final String testPathValue = System.getProperty(TESTS_PATH_SYSTEM_PROPERTY);

    if (testPathValue != null)
      this.testConfigurationFile = new File(testPathValue);
    else
      this.testConfigurationFile = null;
  }

}
