package fr.ens.transcriptome.eoulsan.it;

/**
 * The class represents output result of file comparison.
 * @author Sandrine Perrin
 * @since 2.0
 */
final class ITOutputComparisonResult implements
    Comparable<ITOutputComparisonResult> {

  private String filename;
  // Init status comparison at to compare
  private StatusComparison statusComparison = StatusComparison.TO_COMPARE;
  private String message = "none";

  /**
   * Get report on comparison.
   * @return report on comparison
   */
  public String getReport() {
    return getStatutComparison() + " : " + filename + " " + getMessage();
  }

  /**
   * Set comparison result.
   * @param statusComparison status comparison object
   * @param message detail on comparison
   */
  public void setResult(final StatusComparison statusComparison,
      final String message) {
    setStatutComparison(statusComparison);
    setMessage(message);
  }

  @Override
  public int compareTo(final ITOutputComparisonResult that) {

    return this.getFilename().compareTo(that.getFilename());
  }

  //
  // Getter
  //

  public String getFilename() {
    return filename;
  }

  public StatusComparison getStatutComparison() {
    return statusComparison;
  }

  public String getMessage() {
    return message;
  }

  public void setFilename(final String filename) {
    this.filename = filename;
  }

  public void setStatutComparison(final StatusComparison statutComparison) {
    this.statusComparison = statutComparison;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  //
  // Constructor
  //
  /**
   * Public constructor.
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
        "Comparison(s) failed for output result file(s): "), EQUALS("equals",
        true, ""), UNEXPECTED("unexpected", false,
        "Found unexpected file(s) in result test directory: "),
    MISSING("missing", false,
        "Missing expected file(s) in result test directory: "), TO_COMPARE(
        "to compare", false, "Not comparison to start.");

    private final String name;
    private final String exceptionMessage;
    private final boolean isSuccess;

    public String getName() {
      return name;
    }

    public boolean isSuccess() {
      return isSuccess;
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