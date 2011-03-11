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

package fr.ens.transcriptome.eoulsan.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Settings;

/**
 * This class define the command object of Eoulsan.
 * @author Laurent Jourdren
 */
public class Command {

  private static final Set<Parameter> EMPTY_SET_PARAMETER =
      Collections.emptySet();

  private String name = "";
  private String description = "";
  private String author = "";

  private List<String> stepNamesList = new ArrayList<String>();
  private final Map<String, Set<Parameter>> stepsMap =
      new HashMap<String, Set<Parameter>>();
  private final Set<Parameter> globalParameters = new HashSet<Parameter>();

  //
  // Getters
  //

  /**
   * Get the name.
   * @return Returns the name
   */
  public String getName() {
    return name;
  }

  /**
   * Get description.
   * @return Returns the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get Author.
   * @return Returns the author
   */
  public String getAuthor() {
    return author;
  }

  //
  // Setters
  //

  /**
   * Set the name
   * @param name The name to set
   */
  public void setName(final String name) {

    if (name != null)
      this.name = name;
  }

  /**
   * Set the description
   * @param description The description to set
   */
  public void setDescription(final String description) {

    if (description != null)
      this.description = description;
  }

  /**
   * Set the author.
   * @param author The author to set
   */
  public void setAuthor(final String author) {

    if (author != null)
      this.author = author;
  }

  /**
   * Set globals parameters.
   * @param parameters parameters to set
   */
  public void setGlobalParameters(final Set<Parameter> parameters) {

    this.globalParameters.addAll(parameters);
  }

  /**
   * Get the globals parameters.
   * @return a set of globals parameters
   */
  public Set<Parameter> getGlobalParameters() {

    return this.globalParameters;
  }

  /**
   * Add a step to the analysis
   * @param stepName name of the step to add
   * @param parameters parameters of the step
   * @throws EoulsanException if an error occurs while adding the step
   */
  public void addStep(final String stepName, final Set<Parameter> parameters)
      throws EoulsanException {

    if (stepName == null)
      throw new EoulsanException("The name of the step is null.");

    final String stepNameLower = stepName.toLowerCase().trim();

    if ("".equals(stepNameLower))
      throw new EoulsanException("The name of the step is empty.");

    if (this.stepsMap.containsKey(stepNameLower))
      throw new EoulsanException("The step already exists: " + stepName);

    if (parameters == null)
      throw new EoulsanException("The parameters are null.");

    this.stepNamesList.add(stepNameLower);
    this.stepsMap.put(stepNameLower, parameters);
  }

  /**
   * Get the list of step names.
   * @return a list of step names
   */
  public List<String> getStepNames() {

    return this.stepNamesList;
  }

  /**
   * Get the parameters of a step
   * @param stepName the name of the step
   * @return a set of the parameters of the step
   */
  public Set<Parameter> getStepParameters(final String stepName) {

    final Set<Parameter> result = this.stepsMap.get(stepName);

    return result == null ? EMPTY_SET_PARAMETER : result;
  }

  /**
   * Add a global parameter.
   * @param key key of the parameter
   * @param value value of the parameter
   */
  private void addGlobalParameter(final String key, final String value) {

    if (key == null || value == null)
      return;

    final String keyTrimmed = key.trim();
    final String valueTrimmed = value.trim();

    if ("".equals(keyTrimmed))
      return;

    final Parameter p = new Parameter(keyTrimmed, valueTrimmed);
    this.globalParameters.add(p);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public Command() {

    this(true);
  }

  /**
   * Public constructor.
   * @param addSettingsValues if all the settings must be added to global
   *          properties
   */
  public Command(final boolean addSettingsValues) {

    if (addSettingsValues) {

      final Settings settings = EoulsanRuntime.getRuntime().getSettings();

      for (String settingName : settings.getSettingsNames())
        addGlobalParameter(settingName, settings.getSetting(settingName));
    }
  }

}
