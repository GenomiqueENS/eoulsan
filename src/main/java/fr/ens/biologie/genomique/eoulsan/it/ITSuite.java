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
package fr.ens.biologie.genomique.eoulsan.it;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.LocalEoulsanRuntime.initEoulsanRuntimeForExternalApp;
import static fr.ens.biologie.genomique.kenetre.io.FileUtils.checkExistingDirectoryFile;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.toTimeHumanReadable;
import static java.nio.file.Files.createSymbolicLink;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import com.google.common.base.Stopwatch;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.util.ProcessUtils;

/**
 * This singleton class survey the execution of a test suite (count the number
 * of finished tests and manage the symbolic links in output directory).
 * @author Sandrine Perrin
 * @since 2.0
 */
public class ITSuite {

  private static final String RUNNING_LINK_NAME = "running";
  private static final String SUCCEEDED_LINK_NAME = "succeeded";
  private static final String FAILED_LINK_NAME = "failed";
  private static final String LATEST_LINK_NAME = "latest";

  private static final Formatter DATE_FORMATTER = new Formatter().format(
      Globals.DEFAULT_LOCALE, "%1$tY%1$tm%1$td_%1$tH%1$tM%1$tS", new Date());

  // Singleton
  private static ITSuite itSuite;

  private final Stopwatch globalTimer = Stopwatch.createStarted();

  private final Properties globalsConf;
  private final File applicationPath;
  private final File outputTestsDirectory;
  private final int testsCount;
  private final Map<String, File> testsToExecute;
  private final List<IT> testsInstance;

  private boolean debugEnabled = false;
  private int failCount = 0;
  private int successCount = 0;
  private int testRunningCount = 0;
  private int testSkippingCount = 0;
  private boolean isFirstTest = true;
  private final String loggerPath;
  private final File testsDataDirectory;
  private final String versionApplication;
  private final boolean generateAllExpectedDirectoryTest;
  private final boolean generateNewExpectedDirectoryTests;
  private final String actionType;

  //
  // Singleton methods
  //

  /**
   * Initialize an instance of ITSuite.
   * @param tests tests count for the execution
   * @param globalsConf the globals configuration
   * @param applicationPath the application path
   * @return the instance of ITSuite object
   * @throws EoulsanException if an error occurs when initialize integration
   *           test
   * @throws IOException if an error occurs with a test directories or
   *           configuration file
   */
  public static ITSuite getInstance(final Map<String, File> tests,
      final Properties globalsConf, final File applicationPath)
      throws IOException, EoulsanException {

    if (itSuite == null) {

      itSuite = new ITSuite(tests, globalsConf, applicationPath);

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
   * Creates the symbolic link, if possible create a relative link otherwise a
   * absolute link.
   * @param linkPath the link path
   * @param targetPath the target path
   * @return the path the relative link path
   * @throws IOException if a path is null or not exist.
   */
  public static Path createRelativeOrAbsoluteSymbolicLink(final Path linkPath,
      final Path targetPath) throws IOException {

    if (linkPath == null) {
      throw new IOException(
          "Can not be create relative symbolic link, link path is null.");
    }

    if (targetPath == null) {
      throw new IOException(
          "Can not be create relative symbolic link, target path is null.");
    }

    final Path basePath = linkPath.getParent();

    try {
      // Create relative path on target path
      Path pathRelative = basePath.relativize(targetPath);

      // Create symbolic link
      return createSymbolicLink(linkPath, pathRelative);

    } catch (IllegalArgumentException e) {

      // Not a Path that can be relativized against this path
      // Create a absolute symbolic link
      return createSymbolicLink(linkPath, targetPath);
    }
  }

  /**
   * Update counter of tests running. If it is the first, create symbolics link.
   */
  public void notifyStartTest() {

    if (this.isFirstTest) {
      createSymbolicLinkToTest();
      this.isFirstTest = false;
    }

    // Count test running
    this.testRunningCount++;
  }

  /**
   * Update counter of tests running. If it is the last, update symbolics link
   * and close logger.
   * @param itResult the it result
   */
  public void notifyEndTest(final ITResult itResult) {

    if (itResult.isNothingToDo()) {
      this.testSkippingCount++;
    } else {

      // Update counter
      if (itResult.isSuccess()) {
        this.successCount++;
      } else {
        this.failCount++;
      }
    }

    // For latest
    if (this.testRunningCount == this.testsCount) {
      createSymbolicLinkToTest();
      endLogger();
    }

  }

  /**
   * Execute command line shell to obtain the version name of application to
   * test. If fail, it return UNKNOWN.
   * @param commandLine command line shell
   * @param applicationPath application path to test
   * @return version name of application to test
   */
  public String retrieveVersionApplication(final String commandLine,
      final File applicationPath) {

    String version = "UNKNOWN";

    if (commandLine == null || commandLine.trim().length() == 0) {
      // None command line to retrieve version application set in configuration
      // file
      return version;
    }

    try {
      // Execute command
      final String output = ProcessUtils.execToString(commandLine);

      if (output != null && output.trim().length() > 0) {
        // Retrieve version
        version = output.trim();
      }

    } catch (final IOException e) {
    }

    return version;
  }

  /**
   * Create useful symbolic test to the latest and running test in output test
   * directory.
   */
  private void createSymbolicLinkToTest() {

    // Remove old running test link
    removeOldLink(RUNNING_LINK_NAME);

    // Create running test link
    if (this.testRunningCount == 0) {

      createNewLink(RUNNING_LINK_NAME);

    } else {

      // Replace latest by running test link
      removeOldLinkAndCreateANewOne(LATEST_LINK_NAME);

      if (this.failCount == 0) {

        // Update succeed link
        removeOldLinkAndCreateANewOne(SUCCEEDED_LINK_NAME);
      } else {

        // Update failed link
        removeOldLinkAndCreateANewOne(FAILED_LINK_NAME);
      }
    }
  }

  /**
   * Removes the old link and create a new one.
   * @param linkName the link name
   */
  private void removeOldLinkAndCreateANewOne(final String linkName) {

    // Remove old link
    removeOldLink(linkName);

    // Recreate link
    createNewLink(linkName);
  }

  /**
   * Removes the old link.
   * @param linkName the link name
   */
  private void removeOldLink(final String linkName) {

    final Path outputTestsPath =
        this.outputTestsDirectory.getParentFile().toPath();
    final Path linkPath = new File(outputTestsPath.toFile(), linkName).toPath();

    // Remove old link
    try {
      Files.delete(linkPath);
    } catch (IOException e) {
      getLogger().warning(
          "Unable to delete old " + linkName + " directory link: " + linkPath);
    }
  }

  /**
   * Creates the new link.
   * @param linkName the link name
   */
  private void createNewLink(final String linkName) {

    final Path outputTestsPath =
        this.outputTestsDirectory.getParentFile().toPath();

    // Create the link
    final Path linkPath = new File(outputTestsPath.toFile(), linkName).toPath();
    try {

      createRelativeOrAbsoluteSymbolicLink(linkPath,
          this.outputTestsDirectory.toPath());

    } catch (IOException e) {
      getLogger().warning(
          "Unable to create " + linkName + " directory link: " + linkPath);
    }

  }

  /**
   * Check validate test, exit configuration file at the root.
   * @param tests the tests
   * @return all tests can be run
   * @throws EoulsanException if none tests valid found
   */
  private Map<String, File> checkValidateTest(final Map<String, File> tests)
      throws EoulsanException {

    // Keep test with test.conf file exit at the root directory
    final Map<String, File> validTests = new HashMap<>();

    for (final Map.Entry<String, File> entry : tests.entrySet()) {

      // Check test.conf file exit
      if (new File(entry.getValue(), ITFactory.TEST_CONFIGURATION_FILENAME)
          .exists()) {

        // Keep test
        validTests.put(entry.getKey(), entry.getValue());
      }
    }

    // Check tests found not empty
    if (validTests.isEmpty()) {
      throw new EoulsanException("None test valide in directory "
          + this.testsDataDirectory.getAbsolutePath());
    }

    return Collections.unmodifiableMap(validTests);
  }

  /**
   * Initialize the integration test instances.
   * @return the list of integration test instances.
   * @throws IOException Signals that an I/O exception has occurred, when create
   *           integration test instances.
   * @throws EoulsanException the Eoulsan exception if an error occurs when
   *           create integration test instances, or none instance has been
   *           created.
   */
  private List<IT> initIT() throws IOException, EoulsanException {

    final List<IT> tests = new ArrayList<>();

    // Extract sorted tests name
    final Set<String> testsName = new TreeSet<>(this.testsToExecute.keySet());

    // Parse selected tests
    for (final String testName : testsName) {

      // Create instance
      final IT processIT = new IT(this, this.globalsConf, this.applicationPath,
          this.testsDataDirectory, this.outputTestsDirectory, testName);

      // Add tests
      tests.add(processIT);
    }

    if (tests.isEmpty()) {
      throw new EoulsanException("None integration test instance create.");
    }

    return Collections.unmodifiableList(tests);
  }

  /**
   * Initialization factory with principal needed directories.
   * @throws IOException if a source file doesn't exist
   */
  private void init() throws IOException {

    // Init logger
    initLogger();

    // Set source directory for tests to execute
    checkExistingDirectoryFile(this.testsDataDirectory, "tests data directory");

    getLogger().config(
        "Tests data directory: " + this.testsDataDirectory.getAbsolutePath());

    // Set output directory
    checkExistingDirectoryFile(this.outputTestsDirectory.getParentFile(),
        "output data parent directory");

    // Set directory contain all tests to execute
    getLogger().config("Output tests directory: "
        + this.outputTestsDirectory.getAbsolutePath());

    // Create output test directory
    if (!this.outputTestsDirectory.mkdir()) {
      throw new IOException("Cannot create output tests directory "
          + this.outputTestsDirectory.getAbsolutePath());
    }

    getLogger().config("Action " + this.actionType);

    final File loggerFile = new File(this.loggerPath);
    if (loggerFile.exists()) {
      // Create a symbolic link in output test directory
      createSymbolicLink(
          new File(this.outputTestsDirectory, loggerFile.getName()).toPath(),
          loggerFile.toPath());
    }

  }

  /**
   * Initialize logger.
   * @throws IOException if an error occurs while create logger
   */
  private void initLogger() throws IOException {

    // Remove default logger
    getLogger().setLevel(Level.OFF);

    // Remove default Handler
    getLogger().removeHandler(getLogger().getParent().getHandlers()[0]);

    try {
      initEoulsanRuntimeForExternalApp();
    } catch (final EoulsanException ee) {
      ee.printStackTrace();
    }

    Handler fh = null;
    try {
      fh = new FileHandler(this.loggerPath);

    } catch (final Exception e) {
      throw new IOException(e);
    }

    fh.setFormatter(Globals.LOG_FORMATTER);

    getLogger().setLevel(Level.ALL);
    // Remove output console
    getLogger().setUseParentHandlers(false);
    getLogger().addHandler(fh);
    getLogger().info(Globals.WELCOME_MSG);

  }

  /**
   * Close log file, add a summary on tests execution and update symbolic link
   * in output test directory.
   */
  private void endLogger() {

    getLogger().info("End of execution for "
        + this.testRunningCount + " integration tests in "
        + toTimeHumanReadable(this.globalTimer.elapsed(TimeUnit.MILLISECONDS)));

    // Add summary of tests execution
    getLogger().info("RUN : "
        + this.successCount + " succeeded, " + this.failCount + " failed, "
        + this.testSkippingCount + " skipped. "
        + (this.failCount == 0 ? "All tests are OK." : ""));

    this.globalTimer.stop();
  }

  //
  // Getter and setter
  //

  /**
   * Get the true if debug mode settings otherwise false.
   * @return true if debug mode settings otherwise false.
   */
  public boolean isDebugModeEnabled() {
    return this.debugEnabled;
  }

  /**
   * Set the debug mode, true if it is demand otherwise false.
   * @param debugEnabled true if it is demand otherwise false.
   */
  public void setDebugModeEnabled(final boolean debugEnabled) {
    this.debugEnabled = debugEnabled;
  }

  /**
   * Checks if is generate all expected directory test.
   * @return true, if is generate all expected directory test
   */
  public boolean isGenerateAllExpectedDirectoryTest() {
    return this.generateAllExpectedDirectoryTest;
  }

  /**
   * Checks if is generate new expected directory tests.
   * @return true, if is generate new expected directory tests
   */
  public boolean isGenerateNewExpectedDirectoryTests() {
    return this.generateNewExpectedDirectoryTests;
  }

  public String getActionType() {
    return this.actionType;
  }

  /**
   * Gets the tests data directory.
   * @return the tests data directory
   */
  public File getTestsDataDirectory() {
    return this.testsDataDirectory;
  }

  /**
   * Gets the tests to execute.
   * @return the tests to execute
   */
  public Map<String, File> getTestsToExecute() {
    return this.testsToExecute;
  }

  /**
   * Gets the tests instance.
   * @return the tests instance
   */
  public List<IT> getTestsInstance() {
    return this.testsInstance;
  }

  /**
   * Gets the tests instance to array.
   * @return the tests instance to array
   */
  public Object[] getTestsInstanceToArray() {
    return this.testsInstance.toArray(new Object[this.testsCount]);
  }

  /**
   * Gets the count test.
   * @return the count test
   */
  public int getCountTest() {
    return this.testsCount;
  }

  /**
   * Gets the output test directory path.
   * @return the output test directory path
   */
  public String getOutputTestDirectoryPath() {
    return this.outputTestsDirectory.getAbsolutePath();
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   * @param tests tests count to run
   * @param globalsConf the globals conf
   * @param applicationPath the application path
   * @throws IOException if an error occurs with a test directory or
   *           configuration file
   * @throws EoulsanException if an error occurs when initialize integration
   *           test
   */
  private ITSuite(final Map<String, File> tests, final Properties globalsConf,
      final File applicationPath) throws IOException, EoulsanException {

    checkExistingDirectoryFile(applicationPath, "application path");

    this.globalsConf = globalsConf;
    this.applicationPath = applicationPath;

    // Set test data source directory
    this.testsDataDirectory = new File(
        this.globalsConf.getProperty(ITFactory.TESTS_DIRECTORY_CONF_KEY));

    // Retrieve application version test
    this.versionApplication = retrieveVersionApplication(
        this.globalsConf
            .getProperty(ITFactory.COMMAND_TO_GET_APPLICATION_VERSION_CONF_KEY),
        this.applicationPath);

    // Set logger path
    this.loggerPath =
        this.globalsConf.getProperty(ITFactory.LOG_DIRECTORY_CONF_KEY)
            + "/" + this.versionApplication + "_" + DATE_FORMATTER.toString()
            + ".log";

    // Set test data output directory
    this.outputTestsDirectory = new File(
        this.globalsConf
            .getProperty(ITFactory.OUTPUT_ANALYSIS_DIRECTORY_CONF_KEY),
        this.versionApplication + "_" + DATE_FORMATTER.toString());

    this.generateAllExpectedDirectoryTest = Boolean.parseBoolean(
        globalsConf.getProperty(ITFactory.GENERATE_ALL_EXPECTED_DATA_CONF_KEY));

    this.generateNewExpectedDirectoryTests = Boolean.parseBoolean(
        globalsConf.getProperty(ITFactory.GENERATE_NEW_EXPECTED_DATA_CONF_KEY));

    // Set action required
    this.actionType = (this.generateAllExpectedDirectoryTest
        || this.generateNewExpectedDirectoryTests
            ? (this.generateAllExpectedDirectoryTest
                ? "regenerate all data expected directories if is is not generate manually "
                : "generate all missing data expected directories ")
            : "launch tests integration ");

    // Initialize ITSuite before create integration test instance
    init();

    // Select tests to execute
    this.testsToExecute = checkValidateTest(tests);

    // Init all IT instances
    this.testsInstance = initIT();
    this.testsCount = this.testsInstance.size();

    getLogger().config("Found " + this.testsCount + " tests to execute.");

    // Initialize debug mode
    setDebugModeEnabled(
        Boolean.getBoolean(ITFactory.IT_DEBUG_ENABLE_SYSTEM_KEY));

  }

}
