package fr.ens.transcriptome.eoulsan.toolgalaxy;

import static fr.ens.transcriptome.eoulsan.toolgalaxy.ToolPythonInterpreter.VAR_CMD_NAME;
import static fr.ens.transcriptome.eoulsan.toolgalaxy.VariableRegistry.CALL_METHOD;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.collections.Sets;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class ScriptLineJython {

  private static final String PREFIX_IF = "#if";
  private static final String PREFIX_ELSE = "#else";
  private static final String PREFIX_END = "#end";

  private static final String PREFIX_INSTRUCTION = "#";

  private static int tabulations = 0;
  private static int currentTabCount = 0;
  private static int nextTabCount = 0;

  private final AbstractLineJython lineJython;
  private final String rawLine;

  private final String lineScript;
  private final Set<String> variableNames;

  private AbstractLineJython initLineJython() {

    if (this.rawLine.startsWith(PREFIX_INSTRUCTION)) {
      // Init counter tabulation in script
      updateCounterTabulation();

      return new InstructionLineJython(this.rawLine);
    }

    return new AffectationLineJython(this.rawLine);
  }

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

  //
  // Getter
  //
  String asString() {
    return this.lineScript;
  }

  //
  // Constructor
  //
  ScriptLineJython(final String line) {

    this.rawLine = line.trim();
    this.variableNames = Sets.newHashSet();

    this.lineJython = initLineJython();

    this.lineScript = this.lineJython.rewriteLine();

    // Update tabulation counter
    currentTabCount = nextTabCount;

  }

  //
  // Internal Class
  //
  abstract class AbstractLineJython {

    private final Pattern VARIABLES_PATTERN = Pattern
        .compile("\\$\\{?[\\w.-_]+\\}?");
    private static final String TAB = "\t";

    // private final String rawLine;
    private String modifiedLine;

    //
    // Abstract methods
    //

    abstract String endLine(boolean isCurrentTextCode, boolean firstToken,
        int currentPos);

    abstract String buildLineScript(final String line);

    abstract String addToken(String newToken, final boolean isPreviousCode,
        final boolean isCurrentCode, final boolean firstToken,
        final boolean lastToken);

    //
    // Private methods
    //

    private String addCodeJava(String variableName,
        final boolean isPreviousTextCode, final boolean firstToken,
        final boolean lastToken) {

      final String codeJava = replaceVariableNameByCodeJava(variableName);

      return addToken(codeJava, isPreviousTextCode, true, firstToken, lastToken);
    }

    private String rewriteLine() {

      final Matcher matcher = VARIABLES_PATTERN.matcher(modifiedLine);

      boolean isPreviousTextCode = false;
      boolean isCurrentTextCode = false;
      boolean firstToken = true;
      boolean lastToken = false;
      int currentPos = 0;

      List<String> modifiedLine = Lists.newArrayList();

      String variableName = "";

      while (matcher.find()) {
        // System.out.println("found "
        // + matcher.group() + "\n\tstart: " + matcher.start() + "-"
        // + matcher.end() + "\tcurrent pos: " + currentPos + "\tend txt: "
        // + (rawLine.length() - 1));
        variableName = matcher.group();
        int start = matcher.start();
        int end = matcher.end();

        if (currentPos < start) {
          // Extract text before variable
          String txt = this.modifiedLine.substring(currentPos, start).trim();

          // Remove double quote
          // txt = txt.replaceAll("\"", "'");

          // Add syntax
          if (!txt.isEmpty()) {
            isCurrentTextCode = false;

            // TODO
            System.out.println("add txt " + txt);

            modifiedLine.add(addToken(txt, isPreviousTextCode,
                isCurrentTextCode, firstToken, lastToken));
            firstToken = false;
          }
        }

        // Add motif matched
        modifiedLine.add(addCodeJava(variableName, isPreviousTextCode,
            firstToken, lastToken));
        isCurrentTextCode = true;

        // Update current position
        currentPos = end;
        isPreviousTextCode = isCurrentTextCode;
        firstToken = false;
      }

      // No variable name found
      if (modifiedLine.isEmpty()) {
        String s = endLine(isCurrentTextCode, firstToken, currentPos);
        return buildLineScript(s);
      }

      // End line
      modifiedLine.add(endLine(isCurrentTextCode, firstToken, currentPos));

      return buildLineScript(Joiner.on(" ").join(modifiedLine));
    }

    private String replaceVariableNameByCodeJava(final String variableName) {

      int n = 0;
      // Check presence accolade
      if (variableName.contains("{"))
        n = 1;

      String variableNameTrimmed =
          variableName.substring(1 + n, variableName.length() - n);

      // TODO
      // System.out.println("VAR before\t"
      // + variableName + "\tafter\t" + CALL_METHOD + "(\""
      // + variableNameTrimmed + "\")");

      // Update list variable name
      variableNames.add(variableNameTrimmed);

      return CALL_METHOD + "(\"" + variableNameTrimmed + "\")";
    }

    protected String tab(int n) {

      String str = "";

      for (int i = 0; i < tabulations + n; i++) {
        str += TAB;
      }
      tabulations += n;

      return str;
    }

    void addPrefix(final String prefix) {
      this.modifiedLine = prefix + this.modifiedLine;
    }

    void addSuffix(final String suffix) {
      this.modifiedLine = this.modifiedLine + suffix;
    }

    boolean startsWith(final List<String> prefixes) {

      for (String prefix : prefixes) {
        if (startsWith(prefix)) {
          return true;
        }
      }

      return false;
    }

    boolean startsWith(final String prefix) {
      return this.modifiedLine.startsWith(prefix);
    }

    boolean endsWith(final List<String> suffixes) {

      for (String suffix : suffixes) {
        if (endsWith(suffix)) {
          return true;
        }
      }

      return false;
    }

    boolean endsWith(final String suffix) {
      return this.modifiedLine.endsWith(suffix);
    }

    private String cleanVariableNameSyntax(final String line) {

      boolean foundQuote = false;
      boolean start = false;
      char[] newLine = new char[line.length()];
      char previous = '\0';
      int i = 0;

      for (int n = 0; n < line.length(); n++) {
        char c = line.charAt(n);
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

      // TODO
      System.out.println("CLEAN ------------ " + new String(newLine));

      return new String(newLine);
    }

    //
    // Getter
    //

    public String getModifiedString() {
      return this.modifiedLine;
    }

    public void setModifiedString(final String newLine) {
      this.modifiedLine = newLine;
    }

    //
    // Constructor
    //
    public AbstractLineJython(final String line) {
      // this.rawLine = line;
      this.modifiedLine = cleanVariableNameSyntax(line);
    }

  }

  class InstructionLineJython extends AbstractLineJython {

    private final List<String> START_PREFIX = Lists.newArrayList(PREFIX_IF,
        PREFIX_ELSE);

    @Override
    String endLine(boolean isCurrentTextCode, boolean firstToken, int currentPos) {

      String endString = "";

      // Check ':' final exist
      if (!getModifiedString().endsWith(":")) {
        endString = ":";
      }

      if (currentPos >= getModifiedString().length()) {
        return endString;
      }

      // Extract last token from string
      final String lastToken =
          getModifiedString().substring(currentPos).trim() + endString;

      return addToken(lastToken, isCurrentTextCode, true, firstToken, true);

    }

    @Override
    String addToken(String newToken, final boolean isPreviousCode,
        final boolean isCurrentCode, final boolean firstToken,
        final boolean lastToken) {

      return newToken;
    }

    @Override
    String buildLineScript(final String line) {

      final StringBuilder txt = new StringBuilder();
      txt.append(tab(currentTabCount));

      txt.append(line);

      return txt.toString();
    }

    //
    // Constructor
    //
    public InstructionLineJython(final String line) {
      super(line);

      if (startsWith(START_PREFIX))
        // Remove #
        setModifiedString(getModifiedString().replaceAll("#", ""));
    }

  }

  class AffectationLineJython extends AbstractLineJython {

    @Override
    String endLine(boolean isCurrentTextCode, boolean firstToken, int currentPos) {

      String txt = "";

      if (!isCurrentTextCode) {
        if (!getModifiedString().endsWith("\""))
          // Close string
          txt += "\"";
      } else {
        // Add space
        // txt += " ";
      }

      if (currentPos >= getModifiedString().length()) {
        return txt;
      }

      // Extract last token from string
      String lastToken = getModifiedString().substring(currentPos).trim();

      if (lastToken.isEmpty())
        return txt;

      // Skip token equals single or double quote
      if (lastToken.equals("\"") || lastToken.equals("\'"))
        return txt;

      lastToken += txt;
      return addToken(lastToken, isCurrentTextCode, false, firstToken, true);
    }

    @Override
    String addToken(String newToken, final boolean isPreviousCode,
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

      if (txt.isEmpty())
        return "";

      // // TODO
      // System.out.println("before \t"
      // + newToken + "\tafter\t" + Joiner.on(" ").join(txt));

      // TODO
      System.out.println("newtoken \t"
          + newToken + "\t isPrevCode: " + isPreviousCode
          + "\t isCurrentCode: " + isCurrentCode + "\tfirst: " + firstToken
          + "\tlast: " + lastToken + "\n\t----------" + newToken + " ====> "
          + Joiner.on(" ").join(txt).trim());

      // Return string with default separator escape
      return Joiner.on(" ").join(txt);

    }

    @Override
    String buildLineScript(final String line) {

      final StringBuilder txt = new StringBuilder();
      txt.append(tab(currentTabCount));

      txt.append(VAR_CMD_NAME + " +=  \" \" + ");
      txt.append(line);

      // if (!line.endsWith("\""))
      // txt.append("\"");

      return txt.toString();
    }

    //
    // Constructor
    //
    public AffectationLineJython(final String line) {
      super(line);
    }

  }

  public Collection<String> getVariableNames() {

    if (this.variableNames.isEmpty()) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(variableNames);
  }
}
