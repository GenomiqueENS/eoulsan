package fr.ens.transcriptome.eoulsan.toolgalaxy;

import static fr.ens.transcriptome.eoulsan.toolgalaxy.ToolPythonInterpreter.VAR_CMD_NAME;
import static fr.ens.transcriptome.eoulsan.toolgalaxy.VariableRegistry.CALL_METHOD;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.collections.Sets;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class LineScriptJython {

  private static final String TAB = "\t";
  private static final String PREFIX_IF = "#if";
  private static final String PREFIX_ELSE = "#else";
  private static final String PREFIX_END = "#end";

  private static final String PREFIX_INSTRUCTION = "#";

  public final static Splitter TOKEN = Splitter.onPattern("[ ,;]")
      .trimResults().omitEmptyStrings();

  final static Set<VariablePattern> patterns = initPatterns();

  private static final Pattern VARIABLES_PATTERN = Pattern
      .compile("$\\{?[\\w.-_]+\\}?");

  // Pattern for "$var" or '$var' or "${var}"
  final static Pattern STRING_PATTERN = Pattern
      .compile("[\"\']\\$[\\w.-_{}]+[\"\']");

  private static int tabulations = 0;
  private static int currentTabCount = 0;
  private static int nextTabCount = 0;

  final String rawLine;
  final String lineScript;
  final boolean isInstruction;

  private String replaceVariableByJavaCode(final String line) {

    String modifiedLine = line;

    if (isInstruction())
      modifiedLine = parseLineInstruction(line);

    // return newMethodToParseAndModifyLine(line);
    return rewriteLine(modifiedLine);
  }

  private String rewriteLine(final String line) {

    final Matcher matcher = VARIABLES_PATTERN.matcher(line);

    boolean isPreviousTextCode = false;
    boolean isCurrentTextCode = false;
    boolean firstToken = true;
    boolean lastToken = false;
    int count = 0;

    List<String> modifiedLine = Lists.newArrayList();

    while (matcher.find()) {
      String currentText = matcher.group();
      int start = matcher.start();
      int end = matcher.end();

      firstToken = start == 0;
      lastToken = end == line.length() - 1;

    }

    if (modifiedLine.isEmpty())
      return line;

    return Joiner.on(" ").join(modifiedLine);
  }

  private String buildNewVariableSyntax(final String variableName,
      final int numberCharacterToRemove) {

    // Replace Variable name in python by call method java
    final String variableNameTrimmed =
        variableName.substring(1 + numberCharacterToRemove,
            variableName.length() - numberCharacterToRemove);

    String s = CALL_METHOD + "(\"" + variableNameTrimmed + "\")";

    // Add code to concatenate in string
    if (STRING_PATTERN.matcher(this.rawLine).find()) {
      final String beforeCode = " \" + ";
      final String afterCode = " + ";

      return beforeCode + s + afterCode;
    }

    // Return variable
    return s;
  }

  public String newMethodToParseAndModifyLine(final String line) {

    final List<String> tokens = TOKEN.splitToList(line);
    // TODO
    // System.out.println(Joiner.on("\n").join(tokens));

    final int tokensCount = tokens.size();

    boolean isPreviousCode = false;
    boolean isCurrentCode = false;
    boolean firstToken = true;
    boolean lastToken = false;
    int count = 0;

    // Add space at first
    final List<String> newLine = Lists.newArrayList("\" \" + ");

    // Parse list token to build string
    for (final String token : tokens) {
      char c0 = token.charAt(0);
      String newToken = token;

      lastToken = ++count == tokensCount;

      if (token.startsWith("\"$") || token.startsWith("\'$")) {
        // Remove quote, double quote
        newToken = token.substring(1, token.length() - 1);
        // Re-init first character
        c0 = newToken.charAt(0);
      }

      if (c0 == '$') {
        newToken = replaceVariableNameByCodeJava(newToken);
        isCurrentCode = true;

      } else {
        // Replace double quote by simple
        newToken = newToken.replaceAll("\"", "'");
        isCurrentCode = false;
      }

      // Add token in string
      String t =
          addTokenInString(newToken, isPreviousCode, isCurrentCode, firstToken,
              lastToken);
      newLine.add(addTokenInString(newToken, isPreviousCode, isCurrentCode,
          firstToken, lastToken));

      // TODO
      // System.out.println("newtoken \t"
      // + newToken + "\t isPrevCode: " + isPreviousCode
      // + "\t isCurrentCode: " + isCurrentCode + "\tfirst: " + firstToken
      // + "\tlast: " + lastToken + "\n\t----------" + t + " ====> "
      // + Joiner.on(" ").join(newLine).trim());

      // Update
      isPreviousCode = isCurrentCode;
      firstToken = false;
    }

    if (newLine.isEmpty())
      return "";

    return Joiner.on(" ").join(newLine).trim();
  }

  private String addTokenInString(final String newToken,
      final boolean isPreviousCode, final boolean isCurrentCode,
      final boolean firstToken, final boolean lastToken) {

    final List<String> txt = Lists.newArrayList();

    if (firstToken) {
      if (!isCurrentCode) {
        // Start string
        txt.add("\"");
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
    txt.add(newToken);

    if (lastToken) {
      if (!isCurrentCode) {
        if (!newToken.endsWith("\"") || !newToken.endsWith("\'"))
          // Close string
          txt.add("\"");
      } else {
        // Add space
        txt.add(" ");
      }
    }

    if (txt.isEmpty())
      return "";

    // Return string with default separator escape
    return Joiner.on(" ").join(txt);
  }

  private String replaceVariableNameByCodeJava(final String text) {

    String txtModified = text;

    // Parse pattern
    for (final VariablePattern p : patterns) {
      final Matcher matcher = p.getMatcher(txtModified);

      // Pattern found in text
      while (matcher.find()) {
        // Extract variable name
        final String variableName = matcher.group();

        // Build text to replace variable name
        final String newVariableSyntax =
            buildNewVariableSyntax(variableName, p.getNumberCharacterToRemove());

        // TODO
        // System.out.println("init "
        // + variableName + "\tnew " + newVariableSyntax);

        // Modify line
        txtModified = txtModified.replace(variableName, newVariableSyntax);
      }
    }

    return txtModified;

    // // Parse pattern
    // for (final VariablePattern p : LineScriptJython.patterns) {
    // final Matcher matcher = p.getMatcher(token);
    //
    // // Pattern found in text
    // if (matcher.find()) {
    //
    // String variableNameTrimmed =
    // token.substring(1 + p.getNumberCharacterToRemove(), token.length()
    // - p.getNumberCharacterToRemove());
    //
    // return CALL_METHOD + "(\"" + variableNameTrimmed + "\")";
    // }
    // }
    //
    // return null;
  }

  private static Set<VariablePattern> initPatterns() {
    // Syntax available in command tag
    final Set<VariablePattern> patterns = Sets.newHashSet();
    patterns.add(VariablePattern.SIMPLE_PATTERN);
    patterns.add(VariablePattern.ACCOLATE_PATTERN);

    return patterns;
  }

  private String parseLineInstruction(final String line) {
    String newLine = line;

    // Check ':' final exist
    if (!line.endsWith(":"))
      newLine = newLine + ":";

    // Compute tabulation needed for next line
    // System.out.println("Structure line " + line);
    if (line.startsWith(PREFIX_IF)) {
      // Remove #
      newLine = newLine.replace("#", "");

      currentTabCount = 0;
      nextTabCount = 1;

    } else if (line.startsWith(PREFIX_ELSE)) {
      // Remove #
      newLine = newLine.replace("#", "");

      currentTabCount = -1;
      nextTabCount = 1;

    } else if (line.startsWith(PREFIX_END)) {

      currentTabCount = -1;
      nextTabCount = 0;
    }

    // // Parse pattern
    // for (final VariablePattern p : patterns) {
    // final Matcher matcher = p.getMatcher(this.rawLine);
    //
    // // Pattern found in text
    // while (matcher.find()) {
    // // Extract variable name
    // final String variableName = matcher.group();
    //
    // // Build text to replace variable name
    // final String newVariableSyntax =
    // buildNewVariableSyntax(variableName, p.getNumberCharacterToRemove());
    //
    // // TODO
    // // System.out.println("init "
    // // + variableName + "\tnew " + newVariableSyntax);
    //
    // // Modify line
    // newLine = newLine.replace(variableName, newVariableSyntax);
    // }
    // }

    return replaceVariableNameByCodeJava(newLine);
  }

  private String buildLineScript(final String line) {

    final StringBuilder txt = new StringBuilder();
    txt.append(tab(currentTabCount));

    if (!isInstruction)
      txt.append(VAR_CMD_NAME + " +=  ");

    txt.append(line);

    return txt.toString();

  }

  private String tab(int n) {

    String str = "";

    for (int i = 0; i < tabulations + n; i++) {
      str += TAB;
    }
    tabulations += n;

    return str;
  }

  //
  // Getter
  //
  public String getRawLine() {
    return rawLine;
  }

  public boolean isInstruction() {
    return isInstruction;
  }

  public String asString() {
    return this.lineScript;
  }

  //
  // Constructor
  //
  public LineScriptJython(final String line) {

    this.rawLine = line.trim();
    this.isInstruction = this.rawLine.startsWith(PREFIX_INSTRUCTION);

    String newLine = replaceVariableByJavaCode(this.rawLine);

    // if (isInstruction())
    // newLine = writeLineInstruction(newLine);

    this.lineScript = buildLineScript(newLine);

    // Update tabulation counter
    currentTabCount = nextTabCount;

  }

  //
  // Internal class
  //
  enum VariablePattern {

    SIMPLE_PATTERN("\\$[\\w.-_]+", 0),
    ACCOLATE_PATTERN("\\$\\{[\\w.-_]+\\}", 1);

    int getNumberCharacterToRemove() {
      return this.numberCharacterToRemove;
    }

    public Matcher getMatcher(String txt) {
      return pattern.matcher(txt);
    }

    Pattern getPattern() {
      return this.pattern;
    }

    String getMotif(final String txt) {
      Matcher matcher = pattern.matcher(txt);
      return matcher.group();
    }

    boolean isMatched(final String txt) {
      Matcher matcher = pattern.matcher(txt);
      return matcher.find();
    }

    private final Pattern pattern;
    private final int numberCharacterToRemove;

    //
    // Constructor
    //

    VariablePattern(final String regex, final int n) {
      this.pattern = Pattern.compile(regex);
      this.numberCharacterToRemove = n;
    }

  }
}
