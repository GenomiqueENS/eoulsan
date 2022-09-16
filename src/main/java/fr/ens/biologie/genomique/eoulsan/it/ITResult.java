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

import static com.google.common.io.Files.newWriter;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;

/**
 * This class compute result on integration test execution.
 * @author Sandrine Perrin
 * @since 2.0
 */
@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
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
      // No report
      return;
    }

    final String filename = isSuccess() ? "SUCCESS" : "FAIL";

    final File reportFile =
        new File(this.it.getOutputTestDirectory(), filename);
    Writer fw;
    try {
      fw = newWriter(reportFile,
          Charset.forName(Globals.DEFAULT_FILE_ENCODING));

      // Build text report
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

        getLogger()
            .warning("Error while copying the result execution integration "
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
   */
  private void updateLogger(final String duration) {

    String txt = "";

    if (this.nothingToDo) {
      txt += "NOTHING TO DO of the " + this.it.getTestName();
    } else {

      txt += (isSuccess() ? "SUCCESS" : "FAIL")
          + " of the test " + this.it.getTestName()
          + ((isGeneratedData())
              ? ": generate expected data" : ": launch test and comparison")
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
    report.append(isSuccess() ? "SUCCESS" : "FAIL");
    report.append(": ");
    report.append(this.it.getTestName());
    report.append(isGeneratedData()
        ? ": generate expected data"
        : ": test execution and output files comparison.");
    // TODO add stop here

    report.append("\n\nDate: ");
    report.append(getCurrentFormatedDate());
    report.append('\n');

    report.append("\n\nDirectories:");

    report.append("\n\tExpected:");
    report.append(this.it.getExpectedTestDirectory().getAbsolutePath());
    report.append("\n\tOuput:");
    report.append(this.it.getOutputTestDirectory().getAbsolutePath());

    report.append("\n\nPatterns:");

    // Result for comparison files
    report.append("\n\tFile count to compare from pattern(s): ");
    report.append(this.it.getFileToComparePatterns());

    if (!this.it.getFileToComparePatterns().equals("none")) {
      report.append(": ");
      report.append(this.it.getCountFilesToCheckContent());
      report.append(" file(s)");
    }

    // Result for checking length files
    report.append("\n\tFile lengths count to check from pattern(s): ");
    report.append(this.it.getCheckLengthFilePatterns());

    if (!this.it.getCheckLengthFilePatterns().equals("none")) {
      report.append(": ");
      report.append(this.it.getCountFilesToCheckLength());
      report.append(" file(s)");
    }

    // Result to check if files exist
    report.append("\n\tFile count to check if it exists from pattern(s): ");
    report.append(this.it.getCheckExistenceFilePatterns());
    if (!this.it.getCheckExistenceFilePatterns().equals("none")) {
      report.append(": ");
      report.append(this.it.getCountFilesToCheckExistence());
      report.append(" file(s)");
    }

    // List patterns to exclude files on comparison
    report.append("\n\tPatterns files to exclude comparisons:\t");
    report.append(this.it.getExcludeToComparePatterns());

    // Result to check if files exist
    report
        .append("\n\tFile count to remove from pattern(s) if test succeeded: ");
    report.append(this.it.getFileToRemovePatterns());
    if (!this.it.getFileToRemovePatterns().equals("none")) {
      report.append(": ");
      report.append(this.it.getCountFilesToRemove());
      report.append(" file(s)");
    }

    report.append("\n\nDuration one script maximum: ");
    report.append(
        toTimeHumanReadable(this.it.getDurationMaxInMinutes() * 60 * 1000));
    report.append('\n');

    // Add synthesis on executions scripts
    if (!this.commandsResults.isEmpty()) {
      for (final ITCommandResult icr : this.commandsResults) {
        report.append(icr.getReport());
      }
    }

    if (isGeneratedData()) {
      report.append("\nSUCCESS: copy files ");
      report.append(this.it.getCountFilesToCompare());
      report.append(" to ");
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

    // Add duration on integrated test
    report.append("\n\nTest duration: ");
    report.append(duration);

    // Return text
    return report.toString();
  }

  /**
   * Gets the current formated date.
   * @return the current formated date
   */
  private String getCurrentFormatedDate() {

    final DateFormat df =
        new SimpleDateFormat("yyyy.MM.dd kk:mm:ss", Globals.DEFAULT_LOCALE);

    return df.format(new Date());

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
      if (!ocr.getStatusComparison().isSuccess()) {
        final StringBuilder msg = new StringBuilder();

        if (getException() != null) {
          msg.append(getException().getMessage());
          msg.append("\n");
        }

        // Compile exception message
        msg.append("\t");
        msg.append(ocr.getStatusComparison().getExceptionMessage());
        msg.append("\t");
        msg.append(ocr.getFilename());

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
    msgException.append("\nFrom class: \n\t");
    msgException.append(this.exception.getClass().getName());
    msgException.append("\nException message: \n");
    msgException.append(this.exception.getMessage());
    msgException.append("\n");

    if (ITSuite.getInstance().isDebugModeEnabled() && withStackTrace) {
      // Add the stack trace
      msgException.append("\n=== Execution Test Debug Stack Trace ===\n");
      msgException
          .append(Joiner.on("\n\t").join(this.exception.getStackTrace()));
    }

    // Add last command result message if command has failed
    ITCommandResult lastCommandResult =
        this.commandsResults.get(this.commandsResults.size() - 1);

    if (lastCommandResult.asErrorFileSave()) {
      // Include message
      msgException.append(lastCommandResult.getSTDERRMessageOnProcess());

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
   * Adds the comments at the end of report.
   * @param msg the message
   */
  public void addCommentsIntoTextReport(final String msg) {

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
