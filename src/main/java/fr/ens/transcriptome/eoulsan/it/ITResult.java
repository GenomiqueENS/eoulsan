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

    // End test
    getLogger().info(getLoggerTest(toTimeHumanReadable(duration)));

    final String filename = isSuccess() ? "SUCCESS" : "FAIL";

    final File reportFile =
        new File(this.it.getOutputTestDirectory(), filename);
    Writer fw;
    try {
      fw =
          newWriter(reportFile, Charset.forName(Globals.DEFAULT_FILE_ENCODING));
      fw.write(createReportText(true));
      fw.write("\n");

      fw.flush();
      fw.close();

    } catch (final Exception e) {
      e.printStackTrace();
    }

    if (isGeneratedData()) {
      try {
        Files.copy(reportFile.toPath(),
            new File(this.it.getExpectedTestDirectory(), filename).toPath(),
            StandardCopyOption.REPLACE_EXISTING);
      } catch (final IOException e) {
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

    txt += createExceptionText(this.exception, false);
    return txt;
  }

  /**
   * Create report retrieve by global tests logger.
   * @param duration duration of execution
   * @return report text
   */
  private String getLoggerTest(final String duration) {
    if (this.nothingToDo) {
      return "Nothing_to_do: for " + this.it.getTestName();
    }

    String txt =
        (isSuccess() ? "SUCCESS" : "FAIL")
            + ": for "
            + this.it.getTestName()
            + ((isGeneratedData())
                ? ": generate expected data" : ": launch test and comparison")
            + " in " + duration;
    txt += "\n\tdirectory: " + this.it.getOutputTestDirectory();

    if (!isSuccess()) {
      txt += createExceptionText(this.exception, false);
    }

    return txt;
  }

  /**
   * Create report text.
   * @param withStackTrace if true contains the stack trace if exist
   * @return report text
   */
  private String createReportText(final boolean withStackTrace) {

    final StringBuilder report = new StringBuilder();
    report.append(isSuccess() ? "SUCCESS" : "FAIL");
    report.append(": for ");
    report.append(this.it.getTestName());
    report.append(isGeneratedData()
        ? ": generate expected data" : ": launch test and comparison");

    report.append("\n\nPatterns:");
    report.append("\n\t compare file ");
    report.append(this.it.getFileToComparePatterns());
    report.append("\n\t check size file ");
    report.append(this.it.getCheckExistenceFilePatterns());
    report.append("\n\t exclude file ");
    report.append(this.it.getExcludeToComparePatterns());
    report.append('\n');

    // Add synthesis on execution script
    if (!this.commandsResults.isEmpty()) {
      for (final ITCommandResult icr : this.commandsResults) {
        report.append(icr.getReport());
      }
    }

    if (isGeneratedData()) {
      report.append("\nSUCCESS: copy files to ");
      report.append(this.it.getExpectedTestDirectory().getAbsolutePath());
    }

    // Add report text on comparison execution
    if (!this.comparisonsResults.isEmpty()) {
      for (final ITOutputComparisonResult ocr : this.comparisonsResults) {
        report.append('\n');
        report.append(ocr.getReport());
      }
    }

    // Add message on exception
    if (this.exception != null) {
      report.append('\n');
      report.append(createExceptionText(this.exception, withStackTrace));
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
        msg.append("\tfile: " + ocr.getFilename());
        
        setException(new EoulsanException(msg.toString()));
      }
    }
  }

  /**
   * Create message exception with stack trace if required.
   * @param withStackTrace if true contains the stack trace if exist
   * @return message
   */
  static String createExceptionText(final Throwable exception,
      final boolean withStackTrace) {

    if (exception == null) {
      return "";
    }

    final StringBuilder msgException = new StringBuilder();

    msgException.append("\n=== Execution Test Error ===");
    msgException.append("\nFrom class: \n\t"
        + exception.getClass().getName() + "");
    msgException.append("\nException message: \n"
        + exception.getMessage() + "\n");

    if (ITSuite.getInstance().isDebugEnabled() && withStackTrace) {
      // Add the stack trace
      msgException.append("\n=== Execution Test Debug Stack Trace ===\n");
      msgException.append(Joiner.on("\n\t").join(exception.getStackTrace()));
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
  }

}
