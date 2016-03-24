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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */
package fr.ens.biologie.genomique.eoulsan.galaxytools.cheetah;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;

/**
 * The class can translate a list of string from command tag in tool galaxy XML
 * to script Python then it is interpreted to generate the completed command
 * line on tool.
 * @author Sandrine Perrin
 * @since 2.1
 */
class CheetahToPythonTranslator {

  /** The Constant NEW_LINE. */
  public final static Splitter NEW_LINE =
      Splitter.onPattern("[\r\n]").trimResults().omitEmptyStrings();

  /** The Constant LINE_SEPARATOR. */
  private static final String LINE_SEPARATOR =
      System.getProperties().getProperty("line.separator");

  /** The output Python script. */
  private final String pythonScript;

  /** The variable names. */
  private final Set<String> variableNames = new HashSet<>();

  /**
   * Translate content of command tag from tool XML file.
   * @return script Python to interpreter from building command line tool.
   * @throws EoulsanException an exception occurs if the translation command in
   *           Python return no script.
   */
  private String translate(final String cheetahScript) throws EoulsanException {

    final List<String> translatedLinesFromCommand = new ArrayList<>();

    int currentTabCount = 0;

    // Build line script python
    for (final String line : NEW_LINE.splitToList(cheetahScript)) {

      final TranslatorLineToPython newLine =
          new TranslatorLineToPython(line, currentTabCount);

      translatedLinesFromCommand.add(newLine.asString());

      this.variableNames.addAll(newLine.getVariableNames());

      currentTabCount = newLine.getCurrentTabCount();
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
  public String getPythonScript() {

    return this.pythonScript;
  }

  /**
   * Gets all variable names from command.
   * @return the variable names
   */
  public Set<String> getVariableNames() {

    return Collections.unmodifiableSet(this.variableNames);
  }

  //
  // Constructor
  //

  /**
   * Instantiates a new translator string to script Python.
   * @param cheetahScript the raw command tag
   * @throws EoulsanException occurs if translation fail.
   */
  CheetahToPythonTranslator(final String cheetahScript)
      throws EoulsanException {

    Preconditions.checkNotNull(cheetahScript,
        "Command tag from tool xml is empty.");

    this.pythonScript = translate(cheetahScript);
  }

}
