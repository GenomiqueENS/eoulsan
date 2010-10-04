/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.programs.mgmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * This class define the command object of Eoulsan.
 * @author Laurent Jourdren
 */
public class Command {

  private String name;
  private String description;
  private String author;

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
    this.name = name;
  }

  /**
   * Set the description
   * @param description The description to set
   */
  public void setDescription(final String description) {
    this.description = description;
  }

  /**
   * Set the author.
   * @param author The author to set
   */
  public void setAuthor(final String author) {
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

    if ("".equals(stepName))
      throw new EoulsanException("The name of the step is empty.");

    if (this.stepsMap.containsKey(stepName))
      throw new EoulsanException("The step already exists: " + stepName);

    if (parameters == null)
      throw new EoulsanException("The parameters are null.");

    this.stepNamesList.add(stepName);
    this.stepsMap.put(stepName, parameters);
  }

  /**
   * Get the list of step names.
   * @return a list of step names
   */
  public List<String> getStepNames() {

    return this.stepNamesList;
  }

  /**
   * Get the parameter of a step
   * @param stepName the name of the step
   * @return a set of the parameter of the step
   */
  public Set<Parameter> getStepParameters(final String stepName) {

    return this.stepsMap.get(stepName);
  }

}
