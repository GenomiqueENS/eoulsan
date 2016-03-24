package fr.ens.biologie.genomique.eoulsan.galaxytools.cheetah;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

/**
 * The Class InstructionLinePython.
 * @author Sandrine Perrin
 */
class InstructionLinePython extends AbstractLinePython {

  /** The start prefix. */
  private final List<String> START_PREFIX =
      Lists.newArrayList(TranslatorLineToPython.PREFIX_IF, TranslatorLineToPython.PREFIX_ELSE);

  @Override
  String endLine(final boolean isCurrentTextCode, final boolean firstToken,
      final int currentPos) {

    String endString = "";

    // Check ':' final exist
    if (!this.getModifiedString().endsWith(":")) {
      endString = ":";
    }

    if (currentPos >= this.getModifiedString().length()) {
      return endString;
    }

    // Extract last token from string
    final String lastToken =
        this.getModifiedString().substring(currentPos).trim() + endString;

    return this.addToken(lastToken, isCurrentTextCode, true, firstToken,
        true);

  }

  @Override
  String addToken(final String newToken, final boolean isPreviousCode,
      final boolean isCurrentCode, final boolean firstToken,
      final boolean lastToken) {

    return newToken;
  }

  @Override
  String buildLineScript(final String line, final int currentTabCount) {

    return this.tab(currentTabCount) + line;
  }

  //
  // Constructor
  //

  /**
   * Instantiates a new instruction line Python.
   * @param line the line
   */
  public InstructionLinePython(final String line,
      final Set<String> variableNames) {
    super(line, variableNames);

    if (this.startsWith(this.START_PREFIX)) {
      // Remove #
      this.setModifiedString(this.getModifiedString().replaceAll("#", ""));
    }
  }

}