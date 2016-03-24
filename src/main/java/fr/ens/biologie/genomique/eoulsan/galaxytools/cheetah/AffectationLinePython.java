package fr.ens.biologie.genomique.eoulsan.galaxytools.cheetah;

import static fr.ens.biologie.genomique.eoulsan.galaxytools.cheetah.CheetahInterpreter.VAR_CMD_NAME;

import java.util.Set;

/**
 * The Class AffectationLineJython.
 * @author Sandrine Perrin
 */
class AffectationLinePython extends AbstractLinePython {

  @Override
  String endLine(final boolean isCurrentTextCode, final boolean firstToken,
      final int currentPos) {

    final StringBuilder sb = new StringBuilder();

    if (!isCurrentTextCode) {
      if (!this.getModifiedString().endsWith("\"")) {
        // Close string
        sb.append('\"');
      }
    }

    if (currentPos >= this.getModifiedString().length()) {
      return sb.toString();
    }

    // Extract last token from string
    final String lastToken = this.getModifiedString().substring(currentPos);

    if (lastToken.trim().isEmpty()) {
      return sb.toString();
    }

    // Skip token equals single or double quote
    if (lastToken.equals("\"") || lastToken.equals("\'")) {
      return sb.toString();
    }

    sb.insert(0, lastToken);

    return this.addToken(sb.toString(), isCurrentTextCode, false, firstToken,
        true);
  }

  @Override
  String addToken(final String newToken, final boolean isPreviousCode,
      final boolean isCurrentCode, final boolean firstToken,
      final boolean lastToken) {

    final StringBuilder sb = new StringBuilder();

    if (firstToken) {
      if (!isCurrentCode && !newToken.startsWith("\"")) {
        // Start string
        sb.append("\"");
      }
    } else {

      // Add in middle string
      if (!isPreviousCode && isCurrentCode) {
        // Concatenate text with code
        sb.append("\"+");

      } else if (isPreviousCode) {
        if (isCurrentCode) {
          // Concatenate code with code
          sb.append("+\" \"+");
        } else {
          // Concatenate code with text
          sb.append("+\"");
        }
      }
    }

    // Add text
    sb.append(newToken);

    // Return string with default separator escape
    if (lastToken) {
      if (!isCurrentCode && !newToken.endsWith("\"")) {
        // Start string
        sb.append("\"");
      }
    }
    return sb.toString();
  }

  @Override
  String buildLineScript(final String line, final int currentTabCount) {

    return this.tab(currentTabCount)
        + VAR_CMD_NAME + " += " + line + " + \" \"";
  }

  //
  // Constructor
  //

  /**
   * Instantiates a new affectation line jython.
   * @param line the line
   */
  public AffectationLinePython(final String line,
      final Set<String> variableNames) {
    super(line, variableNames);
  }

}