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
import static fr.ens.transcriptome.eoulsan.toolgalaxy.parameter.AbstractToolElement.getInstanceToolElement;
import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getElementsByTagName;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import fr.ens.transcriptome.eoulsan.EoulsanException;

public class ToolConditionalElement implements ToolElement {

  public final static String TYPE = "boolean";

  private final String nameSpace;

  // Parameter represent choice in option list
  private final ToolElement toolParameterSelect;

  // Variable name in command tag and tool parameter related
  private final Multimap<String, ToolElement> actionsRelatedOptions;

  private String value;

  private boolean isSettings = false;

  @Override
  public boolean setParameterEoulsan() {
    // TODO Auto-generated method stub
    return true;
  }

  // TODO replace by Set<Parameter>
  @Override
  public void setParameterEoulsan(final Map<String, String> parametersEoulsan) {

    // Retrieve choice select from analysis
    this.toolParameterSelect.setParameterEoulsan(parametersEoulsan);

    // Parameter corresponding to choice
    final Collection<ToolElement> toolParameters =
        this.actionsRelatedOptions.get(this.toolParameterSelect.getValue());

    // Check value parameter corresponding to a key
    for (final ToolElement toolParameter : toolParameters) {
      final String value = parametersEoulsan.get(toolParameter.getName());

      if (value == null) {
        toolParameter.setParameterEoulsan();
      } else {
        toolParameter.setParameterEoulsan(value);
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
  private Multimap<String, ToolElement> parseActionsRelatedOptions(
      final Element element) throws EoulsanException {

    // Associate value options with param define in when tag, can be empty
    final Multimap<String, ToolElement> result = ArrayListMultimap.create();
    final List<Element> whenElement = getElementsByTagName(element, "when");

    for (final Element e : whenElement) {
      final String whenName = e.getAttribute("value");

      List<Element> paramElement = getElementsByTagName(e, "param");

      // Can be empty, nothing to do
      if (paramElement == null || paramElement.isEmpty()) {
        result.put(whenName, new ToolParameterEmpty());
        continue;
      }

      // Save param element
      for (final Element elem : paramElement) {
        // Initialize tool parameter related to the choice
        final ToolElement toolParameter =
            getInstanceToolElement(elem, this.nameSpace);

        if (toolParameter != null) {
          // Add tool parameter in result
          result.put(whenName, toolParameter);
        }
      }
    }

    return result;
  }

  //
  // Getter
  //
  @Override
  public String getName() {
    return this.nameSpace;
  }

  public ToolElement getToolParameterSelect() {
    return toolParameterSelect;
  }

  public Collection<ToolElement> getToolParametersResult() {

    if (actionsRelatedOptions.isEmpty()) {
      return Collections.emptyList();
    }

    return actionsRelatedOptions.values();
  }

  // public Map<String, ToolElement> getOptions() {
  // return actionsRelatedOptions;
  // }
  //
  // public Map<String, ToolElement> getCheckedOptions() {
  // return actionsRelatedOptions;
  // }

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

    // // Set tool parameter related
    // if (actionsRelatedOptions.containsKey(paramValue)) {
    // actionsRelatedOptions.get(paramValue).setParameterEoulsan("true");
    //
    // this.value = actionsRelatedOptions.get(paramValue).getValue();
    // }

    return true;
  }

  @Override
  public String toString() {
    return "ToolConditionalElement [name="
        + nameSpace + ", toolParameterSelect=" + toolParameterSelect
        + ", options=" + actionsRelatedOptions + ", parameterEoulsan="
        + getValue() + "]";
  }

  //
  // Constructor
  //
  public ToolConditionalElement(final Element element) throws EoulsanException {

    this.nameSpace = element.getAttribute("name");

    final List<Element> param = extractChildElementsByTagName(element, "param");

    if (param.isEmpty() || param.size() != 1) {
      throw new EoulsanException(
          "Parsing tool xml: not found valid param element "
              + param.size()
              + ". Must be 1 in conditional element, for type select");
    }

    if (!param.get(0).getAttribute("type").equals("select")) {
      throw new EoulsanException(
          "Parsing tool xml: no parameter type select found, in conditional element.");
    }

    // Init parameter select
    this.toolParameterSelect = new ToolParameterSelect(param.get(0), nameSpace);

    // Init default value
    if (this.toolParameterSelect.isSetting()) {
      this.value = this.toolParameterSelect.getValue();
    }

    // Extract all case available
    this.actionsRelatedOptions = parseActionsRelatedOptions(element);

  }
}