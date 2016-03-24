package fr.ens.biologie.genomique.eoulsan.galaxytools.cheetah;

import static fr.ens.biologie.genomique.eoulsan.galaxytools.cheetah.CheetahInterpreter.CALL_METHOD;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class AbstractLinePython.
 * @author Sandrine Perrin
 */
abstract class AbstractLinePython {

  /** The variables pattern. */
  private final Pattern VARIABLES_PATTERN =
      Pattern.compile("\\$\\{?[\\w.-_]+\\}?");

  /** The Constant TAB. */
  private static final String TAB = "\t";

  /** The modified line. */
  private String modifiedLine;

  /** The variable names. */
  private final Set<String> variableNames;

  //
  // Abstract methods
  //

  /**
   * End line.
   * @param isCurrentTextCode the is current text code
   * @param firstToken the first token
   * @param currentPos the current pos
   * @return the string
   */
  abstract String endLine(boolean isCurrentTextCode, boolean firstToken,
      int currentPos);

  /**
   * Builds the line script.
   * @param line the line
   * @return the string
   */
  abstract String buildLineScript(final String line,
      final int currentTabCount);

  /**
   * Adds the token.
   * @param newToken the new token
   * @param isPreviousCode the is previous code
   * @param isCurrentCode the is current code
   * @param firstToken the first token
   * @param lastToken the last token
   * @return the string
   */
  abstract String addToken(String newToken, final boolean isPreviousCode,
      final boolean isCurrentCode, final boolean firstToken,
      final boolean lastToken);

  //
  // Private methods
  //

  /**
   * Adds the Python code.
   * @param variableName the variable name
   * @param isPreviousTextCode the is previous text code
   * @param firstToken the first token
   * @param lastToken the last token
   * @return the string
   */
  private String addPythonCode(final String variableName,
      final boolean isPreviousTextCode, final boolean firstToken,
      final boolean lastToken) {

    final String pythonCode =
        this.createPythonCodeVariableCaller(variableName);

    return this.addToken(pythonCode, isPreviousTextCode, true, firstToken,
        lastToken);
  }

  /**
   * Rewrite line.
   * @return the string
   */
  String rewriteLine(final int currentTabCount) {

    final Matcher matcher = this.VARIABLES_PATTERN.matcher(this.modifiedLine);

    boolean isPreviousTextCode = false;
    boolean isCurrentTextCode = false;
    boolean firstToken = true;
    final boolean lastToken = false;
    int currentPos = 0;

    final StringBuilder modifiedLine = new StringBuilder();

    String variableName;

    while (matcher.find()) {

      variableName = matcher.group();
      final int start = matcher.start();
      final int end = matcher.end();

      if (currentPos < start) {

        // Extract text before variable
        final String txt = this.modifiedLine.substring(currentPos, start);

        // Add syntax
        if (!txt.trim().isEmpty()) {
          isCurrentTextCode = false;

          modifiedLine.append(this.addToken(txt, isPreviousTextCode,
              isCurrentTextCode, firstToken, lastToken));
          firstToken = false;
        }

      }
      isPreviousTextCode = isCurrentTextCode;

      // Add motif matched
      modifiedLine.append(this.addPythonCode(variableName, isPreviousTextCode,
          firstToken, lastToken));
      isCurrentTextCode = true;

      // Update current position
      currentPos = end;
      isPreviousTextCode = isCurrentTextCode;
      firstToken = false;
    }

    // No variable name found
    if (modifiedLine.length() == 0) {
      final String s =
          this.endLine(isCurrentTextCode, firstToken, currentPos);

      return this.buildLineScript(s, currentTabCount);
    }

    // End line
    modifiedLine
        .append(this.endLine(isCurrentTextCode, firstToken, currentPos));

    return this.buildLineScript(modifiedLine.toString(), currentTabCount);
  }

  /**
   * Replace variable name by Python.
   * @param variableName the variable name
   * @return a string
   */
  private String createPythonCodeVariableCaller(final String variableName) {

    int n = 0;

    // Check presence accolade
    if (variableName.contains("{")) {
      n = 1;
    }

    final String variableNameTrimmed =
        variableName.substring(1 + n, variableName.length() - n);

    // Update list variable name
    this.variableNames.add(variableNameTrimmed);

    return CALL_METHOD + "(\"" + variableNameTrimmed + "\")";
  }

  /**
   * Tab.
   * @param n the n
   * @return the string
   */
  protected String tab(final int n) {

    final StringBuilder sb = new StringBuilder();

    for (int i = 0; i < n; i++) {
      sb.append(TAB);
    }

    return sb.toString();
  }

  /**
   * Starts with.
   * @param prefixes the prefixes
   * @return true, if successful
   */
  boolean startsWith(final List<String> prefixes) {

    for (final String prefix : prefixes) {
      if (this.startsWith(prefix)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Starts with.
   * @param prefix the prefix
   * @return true, if successful
   */
  boolean startsWith(final String prefix) {
    return this.modifiedLine.startsWith(prefix);
  }

  /**
   * Ends with.
   * @param suffixes the suffixes
   * @return true, if successful
   */
  boolean endsWith(final List<String> suffixes) {

    for (final String suffix : suffixes) {
      if (this.endsWith(suffix)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Ends with.
   * @param suffix the suffix
   * @return true, if successful
   */
  boolean endsWith(final String suffix) {
    return this.modifiedLine.endsWith(suffix);
  }

  /**
   * Clean variable name syntax.
   * @param line the line
   * @return the string
   */
  private String cleanVariableNameSyntax(final String line) {

    boolean foundQuote = false;
    boolean start = false;
    final char[] newLine = new char[line.length()];
    char previous = '\0';
    int i = 0;

    for (int n = 0; n < line.length(); n++) {
      final char c = line.charAt(n);
      if (c == '$') {
        if (foundQuote) {
          // Start variable name
          start = true;
        } else {
          // Quote for simple text to keep in new line
          if (foundQuote) {
            // write save previous character
            newLine[i] = previous;
            i++;
            foundQuote = false;
          }
        }
      }

      if (foundQuote && !start) {
        newLine[i] = previous;
        i++;
        foundQuote = false;
      }

      if (c == '"' || c == '\'') {
        // Save quote, add if not include in variable name
        if (start) {
          // end variable name, not save character
          start = false;
          foundQuote = false;
        } else {
          foundQuote = true;
          previous = '\'';
        }
      } else {
        foundQuote = false;

        // Write character
        newLine[i] = c;
        i++;
      }

    }

    // Case last character is quote
    if (foundQuote && !start) {
      newLine[i] = previous;
    }

    return new String(newLine);
  }

  //
  // Getter
  //

  /**
   * Gets the modified string.
   * @return the modified string
   */
  public String getModifiedString() {
    return this.modifiedLine;
  }

  /**
   * Sets the modified string.
   * @param newLine the new modified string
   */
  public void setModifiedString(final String newLine) {
    this.modifiedLine = newLine;
  }

  //
  // Constructor
  //

  /**
   * Instantiates a new abstract line jython.
   * @param line the line
   */
  public AbstractLinePython(final String line,
      final Set<String> variableNames) {
    // this.rawLine = line;
    this.modifiedLine = this.cleanVariableNameSyntax(line);
    this.variableNames = variableNames;
  }

}