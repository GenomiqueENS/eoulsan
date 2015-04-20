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

import fr.ens.transcriptome.eoulsan.Globals;

/**
 * The class represents output result of file comparison.
 * @author Sandrine Perrin
 * @since 2.0
 */
final class ITOutputComparisonResult implements
    Comparable<ITOutputComparisonResult> {

  private static final String TYPE_FAIL = "FAIL";
  private static final String TYPE_OK = "OK";

  private String filename;
  // Init status comparison at to compare
  private StatusComparison statusComparison = StatusComparison.TO_COMPARE;
  private String message = "none";
  private String fileTestedPath;
  private String fileExpectedPath;

  /**
   * Get report on comparison.
   * @return report on comparison
   */
  public String getReport() {

    final StringBuilder txt = new StringBuilder();

    txt.append("\t" + this.statusComparison.getType() + " : " + this.filename);

    if (this.statusComparison.getType().equals(TYPE_FAIL)) {

      txt.append(" " + this.statusComparison.getName());
      txt.append("\n\t\tOuput file: " + this.fileTestedPath);
      txt.append("\n\t\tExpected file: " + this.fileExpectedPath);
      txt.append("\n\t\tError message: " + this.message);
    }

    return txt.toString();
  }

  /**
   * Set comparison result.
   * @param statusComparison status comparison object
   * @param message detail on comparison
   */
  public void setResult(final StatusComparison statusComparison,
      final String message) {
    setResult(statusComparison, null, null, message);
  }

  public void setResult(final StatusComparison statusComparison) {
    setResult(statusComparison, null, null, "");
  }

  public void setResult(final StatusComparison status, final File fileExpected,
      final File fileTested, final String msg) {
    setStatutComparison(status);
    setMessage(msg);

    this.fileExpectedPath =
        fileExpected == null ? "none" : fileExpected.getAbsolutePath();
    this.fileTestedPath =
        fileTested == null ? "none" : fileTested.getAbsolutePath();

  }

  @Override
  public int compareTo(final ITOutputComparisonResult that) {

    return this.filename.compareTo(that.filename);
  }

  //
  // Getters & setters
  //

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime
            * result + ((this.filename == null) ? 0 : this.filename.hashCode());
    result =
        prime * result + ((this.message == null) ? 0 : this.message.hashCode());
    result =
        prime
            * result
            + ((this.statusComparison == null) ? 0 : this.statusComparison
                .hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ITOutputComparisonResult other = (ITOutputComparisonResult) obj;
    if (this.filename == null) {
      if (other.filename != null) {
        return false;
      }
    } else if (!this.filename.equals(other.filename)) {
      return false;
    }
    if (this.message == null) {
      if (other.message != null) {
        return false;
      }
    } else if (!this.message.equals(other.message)) {
      return false;
    }
    if (this.statusComparison != other.statusComparison) {
      return false;
    }
    return true;
  }

  /**
   * Gets the filename.
   * @return the filename
   */
  public String getFilename() {
    return this.filename;
  }

  /**
   * Gets the status comparison.
   * @return the status comparison
   */
  public StatusComparison getStatutComparison() {
    return this.statusComparison;
  }

  /**
   * Gets the message.
   * @return the message
   */
  public String getMessage() {
    return this.message;
  }

  /**
   * Sets the filename.
   * @param filename the new filename
   */
  public void setFilename(final String filename) {
    this.filename = filename;
  }

  /**
   * Sets the status comparison.
   * @param statutComparison the new status comparison
   */
  public void setStatutComparison(final StatusComparison statutComparison) {
    this.statusComparison = statutComparison;
  }

  /**
   * Sets the message.
   * @param message the new message
   */
  public void setMessage(final String message) {
    this.message = message;
  }

  //
  // Constructors
  //
  /**
   * Public constructor.
   * @param itOuput the it output instance
   * @param filename filename to compare
   * @param statusComparison status comparison object
   * @param message detail of comparison
   */
  public ITOutputComparisonResult(final String filename,
      final StatusComparison statusComparison, final String message) {

    this.filename = filename;
    this.message = message;
    this.statusComparison = statusComparison;
  }

  /**
   * Public constructor.
   * @param filename filename to compare
   */
  public ITOutputComparisonResult(final String filename) {
    this.filename = filename;
  }

  //
  // Internal class
  //

  /**
   * The class define status comparison available to compare files.
   * @author Sandrine Perrin
   */
  enum StatusComparison {

    NOT_EQUALS("not equals", false,
        "Comparison failed for output result file: "), EQUALS("equals", true,
        ""), UNEXPECTED("unexpected", false,
        "Found unexpected file in result test directory: "), MISSING("missing",
        false, "Missing expected file in result test directory: "), TO_COMPARE(
        "to compare", false, "Not comparison start.");

    private final String name;
    private final String exceptionMessage;
    private final boolean isSuccess;

    public String getName() {
      return this.name.toUpperCase(Globals.DEFAULT_LOCALE);
    }

    public String getType() {
      return isSuccess() ? TYPE_OK : TYPE_FAIL;
    }

    public boolean isSuccess() {
      return this.isSuccess;
    }

    public String getExceptionMessage() {
      return this.exceptionMessage;
    }

    StatusComparison(final String name, final boolean isSuccess,
        final String exceptionMessage) {
      this.name = name;
      this.isSuccess = isSuccess;
      this.exceptionMessage = exceptionMessage;
    }
  }

}