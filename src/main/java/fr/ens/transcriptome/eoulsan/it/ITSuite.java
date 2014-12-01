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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.createSymbolicLink;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;

/**
 * This singleton class survey the execution of a test suite (count the number
 * of finished tests and manage the symbolic links in output directory).
 * @author Sandrine Perrin
 * @since 2.0
 */
public class ITSuite {

  private static final String RUNNING_LINKNAME = "running";
  private static final String SUCCEEDED_LINKNAME = "succeeded";
  private static final String FAILED_LINKNAME = "failed";
  private static final String LATEST_LINKNAME = "latest";

  // Singleton
  private static ITSuite itSuite;

  private final Stopwatch globalTimer = Stopwatch.createStarted();

  private boolean debugEnabled = false;
  private int failCount = 0;
  private int successCount = 0;
  private int testRunningCount = 0;
  private boolean isFirstTest = true;

  private final int testsCount;

  //
  // Singleton methods
  //

  /**
   * Initialize an instance of ITSuite.
   * @param testsCount tests count for the execution
   * @return the instance of ITSuite object
   */
  public static ITSuite getInstance(final int testsCount) {

    if (itSuite == null) {
      itSuite = new ITSuite(testsCount);

      return itSuite;
    }

    throw new EoulsanRuntimeException(
        "Cannot create an instance of ITSuite because an instance has already been created.");
  }

  /**
   * Get the instance of ITSuite object.
   * @return the instance of ITSuite object
   */
  public static ITSuite getInstance() {

    if (itSuite != null) {
      return itSuite;
    }

    throw new EoulsanRuntimeException(
        "Cannot get an instance of ITSuite class because no instance has been created.");
  }

  /**
   * Create useful symbolic test to the latest and running test in output test
   * directory.
   * @param directory the source path directory
   */
  private void createSymbolicLinkToTest(final File directory) {

    // Path to the running link
    File runningDirLink = new File(directory.getParentFile(), RUNNING_LINKNAME);

    // Path to the latest link
    File latestDirLink = new File(directory.getParentFile(), LATEST_LINKNAME);

    // Path to the last succeeded test link
    File succeededDirLink =
        new File(directory.getParentFile(), SUCCEEDED_LINKNAME);

    // Path to the last failed test link
    File failedDirLink = new File(directory.getParentFile(), FAILED_LINKNAME);

    // Remove old running test link
    runningDirLink.delete();

    // Create running test link
    if (this.testRunningCount == 0) {
      createSymbolicLink(directory, runningDirLink);

    } else {
      // Replace latest by running test link

      // Remove old link
      latestDirLink.delete();

      // Recreate the latest link
      createSymbolicLink(directory, latestDirLink);

      if (this.failCount == 0) {
        // Update succeed link
        succeededDirLink.delete();
        createSymbolicLink(directory, succeededDirLink);
      } else {
        // Update failed link
        failedDirLink.delete();
        createSymbolicLink(directory, failedDirLink);
      }
    }
  }

  /**
   * Update counter of tests running. If it is the first, create symbolics link.
   * @param directory directory where create symbolic link.
   */
  public void startTest(final File directory) {

    if (this.isFirstTest) {
      createSymbolicLinkToTest(directory);
      isFirstTest = false;
    }

    // Count test running
    this.testRunningCount++;
  }

  /**
   * Update counter of tests running. If it is the last, update symbolics link
   * and close logger.
   * @param directory directory where update symbolic link.
   */
  public void endTest(final File directory, final ITResult itResult) {

    // Update counter
    if (itResult.isSuccess()) {
      this.successCount++;
    } else {
      this.failCount++;
    }

    // For latest
    if (this.testRunningCount == this.testsCount) {
      createSymbolicLinkToTest(directory);
      closeLogger();
    }

  }

  /**
   * Close log file, add a summary on tests execution and update symbolic link
   * in output test directory.
   */
  private void closeLogger() {

    // Add summary of tests execution
    getLogger().info(
        "Summary tests execution: "
            + successCount + " succeeded tests and " + failCount
            + " failed tests.");

    // Add suffix to log global filename
    getLogger().fine(
        "End of configuration of "
            + testsCount
            + " integration tests in "
            + toTimeHumanReadable(this.globalTimer
                .elapsed(TimeUnit.MILLISECONDS)));

    this.globalTimer.stop();
  }

  //
  // Getter and setter
  //

  /**
   * Get the true if debug mode settings otherwise false.
   * @return true if debug mode settings otherwise false.
   */
  public boolean isDebugEnabled() {
    return debugEnabled;
  }

  /**
   * Set the debug mode, true if it is demand otherwise false.
   * @param debugEnabled true if it is demand otherwise false.
   */
  public void setDebugEnabled(boolean debugEnabled) {
    this.debugEnabled = debugEnabled;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   * @param testsCount tests count to run
   */
  private ITSuite(final int testsCount) {
    this.testsCount = testsCount;
  }

}
