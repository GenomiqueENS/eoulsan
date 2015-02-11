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
package fr.ens.transcriptome.eoulsan.steps.galaxytool;

import static fr.ens.transcriptome.eoulsan.steps.galaxytool.ToolPythonInterpreter.CALL_METHOD;
import static fr.ens.transcriptome.eoulsan.steps.galaxytool.ToolPythonInterpreter.VAR_CMD_NAME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.collections.Sets;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * The class can translate a list of string from command tag in tool galaxy XML
 * to script Python then it is interpreted to generate the completed command
 * line on tool.
 * @author Sandrine Perrin
 * @since 2.0
 */
class TranslatorStringToPython {

  /** The Constant LINE_SEPARATOR. */
  private static final String LINE_SEPARATOR = System.getProperties()
      .getProperty("line.separator");

  /** The tabulations. */
  private int tabulations = 0;

  /** The current tab count. */
  private int currentTabCount = 0;

  /** The next tab count. */
  private int nextTabCount = 0;

  /** The variable names. */
  private final Set<String> variableNames;

  private final List<String> rawCommand;

  private final String translatedCommand;

  /**
   * Translate content of command tag from tool XML file.
   * @return script python to interpreter from building command line tool.
   * @throws EoulsanException an exception occurs if the translation command in
   *           python return no script.
   */
  private String translate() throws EoulsanException {

    final List<String> translatedLinesFromCommand = new ArrayList<>();

    // Build line script python
    for (final String line : this.rawCommand) {

      final TranslatorLineToPython newLine = new TranslatorLineToPython(line);

      translatedLinesFromCommand.add(newLine.asString());

      this.variableNames.addAll(newLine.getVariableNames());
    }

    if (translatedLinesFromCommand.isEmpty()) {
      throw new EoulsanException(
          "Translator command tag from tool xml is empty.");
    }

    // Convert list in command line executable
    return Joiner.on(LINE_SEPARATOR).join(translatedLinesFromCommand).trim();
  }

  //
  // Getters
  //

  /**
   * Gets all variable names from command.
   * @return the variable names
   */
  public Set<String> getVariableNames() {

    return Collections.unmodifiableSet(this.variableNames);
  }

  /**
   * Gets the translated command in Python.
   * @return the translated command in Python
   */
  public String getTranslatedCommandInPython() {

    return this.translatedCommand;
  }

  //
  // Constructor
  //

  /**
   * Instantiates a new translator string to script Python.
   * @param rawCommandTag the raw command tag
   * @throws EoulsanException occurs if translation fail.
   */
  TranslatorStringToPython(final List<String> rawCommandTag)
      throws EoulsanException {

    Preconditions.checkNotNull(rawCommandTag,
        "Command tag from tool xml is empty.");

    this.rawCommand = rawCommandTag;

    // Extract all variables names found in command
    this.variableNames = Sets.newHashSet();

    // Translate to Python
    this.translatedCommand = translate();

    Preconditions.checkNotNull(translatedCommand,
        "Command tag from tool xml is null, fail to translate in Python code.");

  }

  //
  // Internal classes
  //

  /**
   * The internal class translate a line in Python syntax and add java code to
   * replace variable names by value when the script will be interpreted.
   * @since 2.1
   */
  final class TranslatorLineToPython {

    /** The Constant PREFIX_IF. */
    private static final String PREFIX_IF = "#if";

    /** The Constant PREFIX_ELSE. */
    private static final String PREFIX_ELSE = "#else";

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

    /**
     * Inits the correct instance from line to translate.
     * @return the abstract line Python
     */
    private AbstractLinePython initLineJython() {

      if (this.rawLine.startsWith(PREFIX_INSTRUCTION)) {
        // Init counter tabulation in script
        this.updateCounterTabulation();

        return new InstructionLinePython(this.rawLine);
      }

      return new AffectationLinePython(this.rawLine);
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

    //
    // Constructor
    //

    /**
     * Instantiates a new script line Python.
     * @param line the line
     */
    TranslatorLineToPython(final String line) {

      this.rawLine = line.trim();
      this.variableNames = new HashSet<>();

      this.linePython = this.initLineJython();

      this.lineScript = this.linePython.rewriteLine();

      // Update tabulation counter
      currentTabCount = nextTabCount;

    }

    //
    // Internal Class
    //

    /**
     * The Class AbstractLinePython.
     * @author Sandrine Perrin
     */
    abstract class AbstractLinePython {

      /** The variables pattern. */
      private final Pattern VARIABLES_PATTERN = Pattern
          .compile("\\$\\{?[\\w.-_]+\\}?");

      /** The Constant TAB. */
      private static final String TAB = "\t";

      /** The modified line. */
      private String modifiedLine;

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
      abstract String buildLineScript(final String line);

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
       * Adds the code java.
       * @param variableName the variable name
       * @param isPreviousTextCode the is previous text code
       * @param firstToken the first token
       * @param lastToken the last token
       * @return the string
       */
      private String addCodeJava(final String variableName,
          final boolean isPreviousTextCode, final boolean firstToken,
          final boolean lastToken) {

        final String codeJava =
            this.replaceVariableNameByCodeJava(variableName);

        return this.addToken(codeJava, isPreviousTextCode, true, firstToken,
            lastToken);
      }

      /**
       * Rewrite line.
       * @return the string
       */
      private String rewriteLine() {

        final Matcher matcher =
            this.VARIABLES_PATTERN.matcher(this.modifiedLine);

        boolean isPreviousTextCode = false;
        boolean isCurrentTextCode = false;
        boolean firstToken = true;
        final boolean lastToken = false;
        int currentPos = 0;

        final List<String> modifiedLine = new ArrayList<>();

        String variableName = "";

        while (matcher.find()) {
          // System.out.println("found "
          // + matcher.group() + "\n\tstart: " + matcher.start() + "-"
          // + matcher.end() + "\tcurrent pos: " + currentPos + "\tend txt: "
          // + (rawLine.length() - 1));
          variableName = matcher.group();
          final int start = matcher.start();
          final int end = matcher.end();

          if (currentPos < start) {
            // Extract text before variable
            final String txt =
                this.modifiedLine.substring(currentPos, start).trim();

            // Remove double quote
            // txt = txt.replaceAll("\"", "'");

            // Add syntax
            if (!txt.isEmpty()) {
              isCurrentTextCode = false;

              // TODO
              // System.out.println("add txt " + txt);

              modifiedLine.add(this.addToken(txt, isPreviousTextCode,
                  isCurrentTextCode, firstToken, lastToken));
              firstToken = false;
            }

          }
          isPreviousTextCode = isCurrentTextCode;

          // Add motif matched
          modifiedLine.add(this.addCodeJava(variableName, isPreviousTextCode,
              firstToken, lastToken));
          isCurrentTextCode = true;

          // Update current position
          currentPos = end;
          isPreviousTextCode = isCurrentTextCode;
          firstToken = false;
        }

        // No variable name found
        if (modifiedLine.isEmpty()) {
          final String s =
              this.endLine(isCurrentTextCode, firstToken, currentPos);
          return this.buildLineScript(s);
        }

        // End line
        modifiedLine.add(this
            .endLine(isCurrentTextCode, firstToken, currentPos));

        return this.buildLineScript(Joiner.on(" ").join(modifiedLine));
      }

      /**
       * Replace variable name by code java.
       * @param variableName the variable name
       * @return the string
       */
      private String replaceVariableNameByCodeJava(final String variableName) {

        int n = 0;
        // Check presence accolade
        if (variableName.contains("{")) {
          n = 1;
        }

        final String variableNameTrimmed =
            variableName.substring(1 + n, variableName.length() - n);

        // TODO
        // System.out.println("VAR before\t"
        // + variableName + "\tafter\t" + CALL_METHOD + "(\""
        // + variableNameTrimmed + "\")");

        // Update list variable name
        TranslatorLineToPython.this.variableNames.add(variableNameTrimmed);

        return CALL_METHOD + "(\"" + variableNameTrimmed + "\")";
      }

      /**
       * Tab.
       * @param n the n
       * @return the string
       */
      protected String tab(final int n) {

        String str = "";

        for (int i = 0; i < tabulations + n; i++) {
          str += TAB;
        }
        tabulations += n;

        return str;
      }

      /**
       * Adds the prefix.
       * @param prefix the prefix
       */
      void addPrefix(final String prefix) {
        this.modifiedLine = prefix + this.modifiedLine;
      }

      /**
       * Adds the suffix.
       * @param suffix the suffix
       */
      void addSuffix(final String suffix) {
        this.modifiedLine = this.modifiedLine + suffix;
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
      public AbstractLinePython(final String line) {
        // this.rawLine = line;
        this.modifiedLine = this.cleanVariableNameSyntax(line);
      }

    }

    /**
     * The Class InstructionLinePython.
     * @author Sandrine Perrin
     * @since 2.4
     */
    class InstructionLinePython extends AbstractLinePython {

      /** The start prefix. */
      private final List<String> START_PREFIX = Lists.newArrayList(PREFIX_IF,
          PREFIX_ELSE);

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
      String buildLineScript(final String line) {

        final StringBuilder txt = new StringBuilder();
        txt.append(this.tab(currentTabCount));

        txt.append(line);

        return txt.toString();
      }

      //
      // Constructor
      //

      /**
       * Instantiates a new instruction line Python.
       * @param line the line
       */
      public InstructionLinePython(final String line) {
        super(line);

        if (this.startsWith(this.START_PREFIX)) {
          // Remove #
          this.setModifiedString(this.getModifiedString().replaceAll("#", ""));
        }
      }

    }

    /**
     * The Class AffectationLineJython.
     * @author Sandrine Perrin
     */
    class AffectationLinePython extends AbstractLinePython {

      @Override
      String endLine(final boolean isCurrentTextCode, final boolean firstToken,
          final int currentPos) {

        String txt = "";

        if (!isCurrentTextCode) {
          if (!this.getModifiedString().endsWith("\"")) {
            // Close string
            txt += "\"";
          }
        } else {
          // Add space
          // txt += " ";
        }

        if (currentPos >= this.getModifiedString().length()) {
          return txt;
        }

        // Extract last token from string
        String lastToken =
            this.getModifiedString().substring(currentPos).trim();

        if (lastToken.isEmpty()) {
          return txt;
        }

        // Skip token equals single or double quote
        if (lastToken.equals("\"") || lastToken.equals("\'")) {
          return txt;
        }

        lastToken += txt;
        return this.addToken(lastToken, isCurrentTextCode, false, firstToken,
            true);
      }

      @Override
      String addToken(final String newToken, final boolean isPreviousCode,
          final boolean isCurrentCode, final boolean firstToken,
          final boolean lastToken) {

        final List<String> txt = Lists.newArrayList();

        if (firstToken) {
          if (!isCurrentCode && !newToken.startsWith("\"")) {
            // Start string
            txt.add("\" ");
          }
        } else {
          // Add in middle string
          if (!isPreviousCode && isCurrentCode) {
            // Concatenate text with code
            txt.add("\"+");

          } else if (isPreviousCode) {
            if (isCurrentCode) {
              // Concatenate code with code
              txt.add("+\" \"+");
            } else {
              // Concatenate code with text
              txt.add("+\"");
            }
          }
        }
        // Add text
        // if (!isCurrentCode)
        // newToken = newToken.replaceAll("\"", "'");
        txt.add(newToken);

        if (txt.isEmpty()) {
          return "";
        }

        // // TODO
        // System.out.println("before \t"
        // + newToken + "\tafter\t" + Joiner.on(" ").join(txt));

        // TODO
        // System.out.println("newtoken \t"
        // + newToken + "\t isPrevCode: " + isPreviousCode
        // + "\t isCurrentCode: " + isCurrentCode + "\tfirst: " + firstToken
        // + "\tlast: " + lastToken + "\n\t----------" + newToken + " ====> "
        // + Joiner.on(" ").join(txt).trim());

        // Return string with default separator escape
        return Joiner.on(" ").join(txt).trim();

      }

      @Override
      String buildLineScript(final String line) {

        final StringBuilder txt = new StringBuilder();
        txt.append(this.tab(currentTabCount));

        txt.append(VAR_CMD_NAME + " +=  \" \" + ");
        txt.append(line);

        // if (!line.endsWith("\""))
        // txt.append("\"");

        return txt.toString();
      }

      //
      // Constructor
      //

      /**
       * Instantiates a new affectation line jython.
       * @param line the line
       */
      public AffectationLinePython(final String line) {
        super(line);
      }

    }

  }

}