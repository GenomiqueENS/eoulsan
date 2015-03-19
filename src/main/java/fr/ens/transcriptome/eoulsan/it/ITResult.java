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

import static com.google.common.io.Files.newWriter;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;

/**
 * This class compute result on integration test execution.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class ITResult {

  private final IT it;
  private final StringBuilder commentForReport;

  private Throwable exception;
  private final List<ITCommandResult> commandsResults;
  private Set<ITOutputComparisonResult> comparisonsResults;

  private boolean generatedData = false;

  // True if demand generate data and expected directory already exist
  private boolean nothingToDo = false;

  //
  // Write reports
  //
  /**
   * Create report of the test execution.
   * @param duration of execution test
   */
  public void createReportFile(final long duration) {

    final String durationIT = toTimeHumanReadable(duration);

    // End test
    updateLogger(durationIT);

    if (isNothingToDo()) {
      return;
    }

    final String filename = isSuccess() ? "SUCCESS" : "FAIL";

    final File reportFile =
        new File(this.it.getOutputTestDirectory(), filename);
    Writer fw;
    try {
      fw =
          newWriter(reportFile, Charset.forName(Globals.DEFAULT_FILE_ENCODING));
      fw.write(createReportText(true, durationIT));
      fw.write("\n");

      fw.flush();
      fw.close();

    } catch (final Exception e) {
      e.printStackTrace();
    }

    if (isGeneratedData()) {
      try {
        // Copy result file in expected test directory
        Files.copy(reportFile.toPath(),
            new File(this.it.getExpectedTestDirectory(), filename).toPath(),
            StandardCopyOption.REPLACE_EXISTING);

      } catch (final IOException e) {

        getLogger().warning(
            "Error while copying the result execution integration "
                + "test in expected directory: " + e.getMessage());
      }
    }
  }

  /**
   * Create report retrieve by testng instance and display in report.
   * @return report text
   */
  public String createReportTestngMessage() {

    if (isSuccess()) {
      return "";
    }

    // Text without stack message when an exception occurs
    String txt = "Fail test: " + this.it.getTestName();
    txt += "\n\tdirectory: " + this.it.getOutputTestDirectory();

    txt += createExceptionText(false);
    return txt;
  }

  /**
   * Create report retrieve by global tests logger.
   * @param duration duration of execution
   * @return report text
   */
  private void updateLogger(final String duration) {

    String txt = "";

    if (this.nothingToDo) {
      txt += "NOTHING TO DO of the " + this.it.getTestName();
    } else {

      txt +=
          (isSuccess() ? "SUCCESS" : "FAIL")
              + " of the test "
              + this.it.getTestName()
              + ((isGeneratedData())
                  ? "generate expected data" : "launch test and comparison")
              + ". Duration = " + duration;

      if (!isSuccess()) {
        // Add exception explanation in logger
        txt += createExceptionText(false);
      }
    }

    getLogger().info(txt);

  }

  /**
   * Create report text.
   * @param withStackTrace if true contains the stack trace if exist
   * @param duration the duration on integrated test.
   * @return report text
   */
  private String createReportText(final boolean withStackTrace,
      final String duration) {

    final StringBuilder report = new StringBuilder();
    report.append((isSuccess() ? "SUCCESS" : "FAIL")
        + ": " + this.it.getTestName());
    report.append(isGeneratedData()
        ? ": generate expected data"
        : ": test execution and output files comparison.");

    report.append("\n\nDirectories:");
    report.append("\n\tExpected:"
        + this.it.getExpectedTestDirectory().getAbsolutePath());
    report.append("\n\tOuput:"
        + this.it.getOutputTestDirectory().getAbsolutePath());

    report.append("\n\nPatterns:");

    // Result for comparison files
    report.append("\n\tFile count to compare from pattern(s) "
        + this.it.getFileToComparePatterns());

    if (!this.it.getFileToComparePatterns().equals("none")) {
      report.append(": " + this.it.getCountFilesToCheckContent() + " file(s)");
    }

    // Result for checking length files
    report.append("\n\tFile lengths count to check from pattern(s) "
        + this.it.getCheckLengthFilePatterns());

    if (!this.it.getCheckLengthFilePatterns().equals("none")) {
      report.append(": " + this.it.getCountFilesToCheckLength() + " file(s)");
    }

    // Result to check if files exist
    report.append("\n\tFile count to check if it exists from pattern(s) "
        + this.it.getCheckExistenceFilePatterns());
    if (!this.it.getCheckExistenceFilePatterns().equals("none")) {
      report
          .append(": " + this.it.getCountFilesToCheckExistence() + " file(s)");
    }

    // List patterns to exclude files on comparison
    report.append("\n\tPatterns files to exclude comparisons :\t"
        + this.it.getExcludeToComparePatterns());

    report.append('\n');

    // Add synthesis on executions scripts
    if (!this.commandsResults.isEmpty()) {
      for (final ITCommandResult icr : this.commandsResults) {
        report.append(icr.getReport());
      }
    }

    // Add duration on integrated test
    report.append("\nDuration test: " + duration);

    if (isGeneratedData()) {
      report.append("\nSUCCESS: copy files "
          + this.it.getCountFilesToCompare() + " to ");
      report.append(this.it.getExpectedTestDirectory().getAbsolutePath());
    }

    // Check comparison execute
    if (this.comparisonsResults.isEmpty()) {

      // Add message on exception
      if (this.exception != null) {
        report.append('\n');
        report.append(createExceptionText(withStackTrace));
        report.append('\n');
      }

    } else {

      // Add report text on comparison execution
      report.append("\n\nComparisons:");

      for (final ITOutputComparisonResult ocr : this.comparisonsResults) {
        report.append('\n');
        report.append(ocr.getReport());
      }
      report.append('\n');
    }

    // Add comment(s)
    if (this.commentForReport.length() > 0) {
      report.append(this.commentForReport.toString());
    }

    // Return text
    return report.toString();
  }

  /**
   * Collect all exceptions throw when compare output test generated to output
   * test expected.
   */
  public void checkNeededThrowException() {

    if (this.comparisonsResults.isEmpty()) {
      return;
    }

    // Check comparison output it result
    for (final ITOutputComparisonResult ocr : this.comparisonsResults) {
      if (!ocr.getStatutComparison().isSuccess()) {
        final StringBuilder msg = new StringBuilder();

        if (getException() != null) {
          msg.append(getException().getMessage());
          msg.append("\n");
        }

        // Compile exception message
        msg.append("\t");
        msg.append(ocr.getStatutComparison().getExceptionMessage());
        msg.append("\t" + ocr.getFilename());

        setException(new EoulsanException(msg.toString()));
      }
    }
  }

  /**
   * Create message exception with stack trace if required.
   * @param withStackTrace if true contains the stack trace if exist
   * @return message
   */
  public String createExceptionText(final boolean withStackTrace) {

    if (this.exception == null) {
      return "";
    }

    final StringBuilder msgException = new StringBuilder();

    msgException.append("\n=== Execution Test Error ===");
    msgException.append("\nFrom class: \n\t"
        + this.exception.getClass().getName() + "");
    msgException.append("\nException message: \n"
        + this.exception.getMessage() + "\n");

    if (ITSuite.getInstance().isDebugModeEnabled() && withStackTrace) {
      // Add the stack trace
      msgException.append("\n=== Execution Test Debug Stack Trace ===\n");
      msgException.append(Joiner.on("\n\t")
          .join(this.exception.getStackTrace()));
    }

    // Return text
    return msgException.toString();
  }

  /**
   * Add command line result.
   * @param cmdResult command line result object
   */
  public void addCommandResult(final ITCommandResult cmdResult) {
    if (cmdResult != null) {
      this.commandsResults.add(cmdResult);
    }
  }

  /**
   * Add comparisons results and check if exception has been throw.
   * @param comparisonsResults set of comparison results.
   */
  public void addComparisonsResults(
      final Set<ITOutputComparisonResult> comparisonsResults) {

    if (comparisonsResults != null) {
      this.comparisonsResults = comparisonsResults;
    }

    // Check if exception has been throw.
    checkNeededThrowException();
  }

  /**
   * Adds the comments at the end of repport.
   * @param msg the message
   */
  public void addCommentsForReport(final String msg) {

    if (this.commentForReport.length() == 0) {
      // Add header
      this.commentForReport.append("\nComment(s) on this integated test \n");
    }

    // Add message
    this.commentForReport.append(msg);
  }

  //
  // Getter and Setter
  //

  /**
   * As generated data.
   */
  public void asGeneratedData() {
    this.generatedData = true;
  }

  /**
   * Checks if is generated data.
   * @return true, if is generated data
   */
  public boolean isGeneratedData() {
    return this.generatedData;
  }

  /**
   * Checks if is success.
   * @return true, if is success
   */
  public boolean isSuccess() {
    return this.exception == null;
  }

  /**
   * Gets the exception.
   * @return the exception
   */
  public Throwable getException() {
    return this.exception;
  }

  /**
   * Sets the exception.
   * @param e the new exception
   */
  public void setException(final Throwable e) {
    this.exception = e;
  }

  /**
   * As nothing to do.
   */
  public void asNothingToDo() {
    this.nothingToDo = true;
  }

  /**
   * Checks if is nothing to do.
   * @return true, if is nothing to do
   */
  public boolean isNothingToDo() {
    return this.nothingToDo;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param it integration test object
   */
  public ITResult(final IT it) {
    this.it = it;
    this.commandsResults = new ArrayList<>();
    this.comparisonsResults = Collections.emptySet();
    this.commentForReport = new StringBuilder();
  }
}
