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

import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;

/**
 * This class is a demo that show how launching integration tests without Maven.
 * @author Laurent Jourdren
 */
public class EoulsanITDemo {

  public static void main(String[] args) {

    // Define a listener that print information about the results of the
    // integration tests
    TestListenerAdapter tla = new TestListenerAdapter() {

      @Override
      public void onTestSuccess(final ITestResult tr) {

        super.onTestSuccess(tr);
        System.out.println(tr);
      }

      @Override
      public void onTestFailure(final ITestResult tr) {

        super.onTestFailure(tr);
        System.err.println(tr);
        System.err.println(tr.getThrowable().getMessage());
      }

    };

    // Set the path to the tests configuration file in a Java system property
    System.setProperty(EoulsanITFactory.TESTS_PATH_SYSTEM_PROPERTY, ".");

    // Create and configure TestNG
    TestNG testng = new TestNG();
    testng.setTestClasses(new Class[] { EoulsanITFactory.class });
    testng.addListener(tla);

    // Launch integration tests using TestNG
    testng.run();
  }

}
