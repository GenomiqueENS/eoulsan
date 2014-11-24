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

package fr.ens.transcriptome.eoulsan.toolgalaxy.parameter;

import static fr.ens.transcriptome.eoulsan.toolgalaxy.ToolInterpreter.extractChildElementsByTagName;
import static fr.ens.transcriptome.eoulsan.toolgalaxy.parameter.AbstractToolParameter.getInstanceToolParameter;
import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getElementsByTagName;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;

public class ToolConditionalElement implements ToolElement {

  public final static String TYPE = "boolean";

  private final String name;

  // Parameter represent choice in option list
  private final ToolElement toolParameterSelect;

  // Variable name in command tag and tool parameter related
  private final Map<String, ToolElement> actionsRelatedOptions;

  private List<ToolElement> toolParametersResult;

  private String value;

  private boolean isSettings = false;

  // TODO replace by Set<Parameter>
  public void setParameterEoulsan(final Map<String, String> parametersEoulsan) {

    this.toolParametersResult = Lists.newArrayList();

    for (Map.Entry<String, String> p : parametersEoulsan.entrySet()) {

      // Check value parameter corresponding to a key
      final ToolElement toolParameter = actionsRelatedOptions.get(p.getValue());

      if (toolParameter != null) {
        toolParameter.setParameterEoulsan();
        this.toolParametersResult.add(toolParameter);
      }
    }

    // Save setting parameter
    this.isSettings = true;
  }

  //
  // Private methods
  //
  /**
   * @param element
   * @return
   */
  private Map<String, ToolElement> parseActionsRelatedOptions(
      final Element element) throws EoulsanException {

    // Associate value options with param define in when tag, can be empty
    final Map<String, ToolElement> result = Maps.newHashMap();
    final List<Element> whenElement = getElementsByTagName(element, "when");

    final int whenElementCount = whenElement.size();

    for (final Element e : whenElement) {
      final String whenName = e.getAttribute("value");

      List<Element> paramElement = getElementsByTagName(e, "param");

      // Can be empty, nothing to do
      if (paramElement == null || paramElement.isEmpty()) {
        result.put(whenName, null);
        continue;
      }
      assert paramElement.size() != 1 : "When element with more one param element.";

      // Initialize tool parameter related to the choice
      final ToolElement toolParameter =
          getInstanceToolParameter(paramElement.get(0), this.name);

      // Add tool parameter in result
      result.put(whenName, toolParameter);
    }

    return result;
  }

  //
  // Getter
  //
  public String getName() {
    return this.name;
  }

  public ToolElement getToolParameterSelect() {
    return toolParameterSelect;
  }

  public List<ToolElement> getToolParametersResult() {

    if (toolParametersResult.isEmpty())
      return Collections.emptyList();

    return toolParametersResult;
  }

  public Map<String, ToolElement> getOptions() {
    return actionsRelatedOptions;
  }

  public Map<String, ToolElement> getCheckedOptions() {
    return actionsRelatedOptions;
  }

  @Override
  public boolean isSetting() {
    return isSettings;
  }

  @Override
  public String getValue() {
    return this.value;
  }

  @Override
  public boolean setParameterEoulsan(final String paramValue) {

    // Set tool parameter related
    if (actionsRelatedOptions.containsKey(paramValue)) {
      actionsRelatedOptions.get(paramValue).setParameterEoulsan("true");

      this.value = actionsRelatedOptions.get(paramValue).getValue();
    }

    return true;
  }

  @Override
  public String toString() {
    return "ToolConditionalElement [name="
        + name + ", toolParameterSelect=" + toolParameterSelect + ", options="
        + actionsRelatedOptions + ", toolParametersResult="
        + toolParametersResult + ", parameterEoulsan=" + getValue() + "]";
  }

  //
  // Constructor
  //
  public ToolConditionalElement(final Element element) throws EoulsanException {

    this.name = element.getAttribute("name");

    final List<Element> param = extractChildElementsByTagName(element, "param");

    if (param.isEmpty() || param.size() != 1) {
      throw new EoulsanException(
          "Parsing tool xml: not found valid param element "
              + param.size()
              + ". Must be 1 in conditional element, for type select");
    }

    if (!param.get(0).getAttribute("type").equals("select"))
      throw new EoulsanException(
          "Parsing tool xml: no parameter type select found, in conditional element.");

    // Init parameter select
    this.toolParameterSelect = new ToolParameterSelect(param.get(0), name);

    // Init default value
    if (this.toolParameterSelect.isSetting())
      this.value = this.toolParameterSelect.getValue();

    // Extract all case available
    this.actionsRelatedOptions = parseActionsRelatedOptions(element);

  }

  @Override
  public boolean setParameterEoulsan() {
    // TODO Auto-generated method stub
    return true;
  }
}