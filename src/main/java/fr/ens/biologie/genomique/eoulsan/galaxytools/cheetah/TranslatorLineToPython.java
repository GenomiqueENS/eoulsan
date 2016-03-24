package fr.ens.biologie.genomique.eoulsan.galaxytools.cheetah;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The internal class translate a line in Python syntax and add java code to
 * replace variable names by value when the script will be interpreted.
 * @since 2.1
 */
class TranslatorLineToPython {

  /** The Constant PREFIX_IF. */
  static final String PREFIX_IF = "#if";

  /** The Constant PREFIX_ELSE. */
  static final String PREFIX_ELSE = "#else";

  /** The Constant PREFIX_END. */
  private static final String PREFIX_END = "#end";

  /** The Constant PREFIX_INSTRUCTION. */
  private static final String PREFIX_INSTRUCTION = "#";

  /** The line jython. */
  private final AbstractLinePython linePython;

  /** The raw line. */
  private final String rawLine;

  /** The line script. */
  private final String lineScript;

  /** The variable names. */
  private final Set<String> variableNames;

  /** The next tab count. */
  private int nextTabCount = 0;

  /** The current tab count. */
  private int currentTabCount = 0;

  /**
   * Inits the correct instance from line to translate.
   * @return the abstract line Python
   */
  private AbstractLinePython initLineJython() {

    if (this.rawLine.startsWith(PREFIX_INSTRUCTION)) {
      // Init counter tabulation in script
      this.updateCounterTabulation();

      return new InstructionLinePython(this.rawLine, this.variableNames);
    }

    return new AffectationLinePython(this.rawLine, this.variableNames);
  }

  /**
   * Update counter tabulation.
   */
  private void updateCounterTabulation() {
    // Compute tabulation needed for next line
    // System.out.println("Structure line " + line);
    if (this.rawLine.startsWith(PREFIX_IF)) {

      currentTabCount = 0;
      nextTabCount = 1;

    } else if (this.rawLine.startsWith(PREFIX_ELSE)) {

      currentTabCount = -1;
      nextTabCount = 1;

    } else if (this.rawLine.startsWith(PREFIX_END)) {

      currentTabCount = -1;
      nextTabCount = 0;
    }
  }

  /**
   * Gets the variable names.
   * @return the variable names
   */
  public Collection<String> getVariableNames() {

    if (this.variableNames.isEmpty()) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(this.variableNames);
  }

  //
  // Getter
  //

  /**
   * As string.
   * @return the string
   */
  String asString() {
    return this.lineScript;
  }

  int getCurrentTabCount() {
    return this.currentTabCount;
  }

  //
  // Constructor
  //

  /**
   * Instantiates a new script line Python.
   * @param line the line
   */
  TranslatorLineToPython(final String line, final int currentTabCount) {

    this.rawLine = line.trim();
    this.variableNames = new HashSet<>();

    this.linePython = this.initLineJython();

    this.currentTabCount = currentTabCount;

    this.lineScript = this.linePython.rewriteLine(currentTabCount);

    // Update tabulation counter
    this.currentTabCount = nextTabCount;

  }

}
