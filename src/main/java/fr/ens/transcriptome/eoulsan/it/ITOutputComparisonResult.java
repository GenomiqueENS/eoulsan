package fr.ens.transcriptome.eoulsan.it;

/**
 * The internal class represents output result of file comparison.
 * @author Sandrine Perrin
 * @since 2.0
 */
final class ITOutputComparisonResult implements
    Comparable<ITOutputComparisonResult> {

  private String filename;
  private StatutComparison statutComparison = StatutComparison.TO_COMPARE;
  private String message = "none";

  public String getReport() {
    return getStatutComparison() + " : " + filename + " " + getMessage();
  }

  public void setResult(final StatutComparison statutComparison,
      final String message) {
    setStatutComparison(statutComparison);
    setMessage(message);
  }

  //
  // Getter
  //

  public String getFilename() {
    return filename;
  }

  public StatutComparison getStatutComparison() {
    return statutComparison;
  }

  public String getMessage() {
    return message;
  }

  public void setFilename(final String filename) {
    this.filename = filename;
  }

  public void setStatutComparison(StatutComparison statutComparison) {
    this.statutComparison = statutComparison;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  //
  // Constructor
  //
  public ITOutputComparisonResult(final String filename,
      final StatutComparison statutComparison, final String message) {
    this.filename = filename;
    this.message = message;
    this.statutComparison = statutComparison;
  }

  public ITOutputComparisonResult(final String filename) {
    this.filename = filename;
  }

  @Override
  public int compareTo(final ITOutputComparisonResult that) {

    return this.getFilename().compareTo(that.getFilename());
  }

  //
  //
  //

  enum StatutComparison {

    NOT_EQUALS("not equals", false), EQUALS("equals", true), UNEXPECTED(
        "unexpected", false), MISSING("missing", false), TO_COMPARE(
        "to compare", false);

    private final String name;
    private final boolean isSuccess;

    public String getName() {
      return name;
    }

    public boolean isSuccess() {
      return isSuccess;
    }

    StatutComparison(final String name, final boolean isSuccess) {
      this.name = name;
      this.isSuccess = isSuccess;
    }
  }
}