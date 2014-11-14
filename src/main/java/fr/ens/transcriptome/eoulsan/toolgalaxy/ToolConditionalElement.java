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

package fr.ens.transcriptome.eoulsan.toolgalaxy;

import static fr.ens.transcriptome.eoulsan.toolgalaxy.ToolInterpreter.extractChildElementsByTagName;
import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getElementsByTagName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

public class ToolConditionalElement implements ToolElement {

  private final String name;
  private final int optionCount;

  // Parameter represent choice in option list
  private final ToolParameter toolParameterSelect;

  // Variable name in command tag and tool parameter related
  private final Map<String, ToolParameter> options;
  private final Map<String, ToolParameter> checkOptions;

  private List<ToolParameter> toolParametersResult;

  private String parameterEoulsan;

  // TODO replace by Set<Parameter>
  public void setParameterEoulsan(final Map<String, String> parametersEoulsan) {

    this.toolParametersResult = Lists.newArrayList();

    for (Map.Entry<String, String> p : parametersEoulsan.entrySet()) {
      
      // Check value parameter corresponding to a key
      if (options.containsKey(p.getKey())) {

          this.toolParametersResult.add(toolParameterSelected);
      }
    }

  }
  

  // public Collection<? extends ToolElement> getToolParametersSelected() {
  //
  // if (this.toolParametersResult.isEmpty())
  // return Collections.emptyList();
  //
  // return Collections.unmodifiableList(this.toolParametersResult);
  // }

  //
  // Private methods
  //
  /**
   * @param element
   * @return
   */
  private Map<String, ToolParameter> extractWhenTag(final Element element) {

    final Map<String, ToolParameter> result = Maps.newHashMap();

    final List<Element> elemWhen =
        getElementsByTagName(element, "when");

    final int whenTagCount = elemWhen.size();

    // TODO
    assert whenTagCount == optionCount : "No equals option and when element";

    for (final Element e : elemWhen) {
      final String nameWhenTag = e.getAttribute("name");

      List<Element> paramElement = getElementsByTagName(e, "param");
      assert paramElement.size() != 1 : "When element with more one param element.";

      result.put(nameWhenTag, new ToolParameter(paramElement.get(0), name));
    }

    return result;
  }

  private Map<String, String> extractAllOptions(final Element parent) {
    
    final Map<String, String> allOptions = Maps.newHashMap();
    
    final List<Element> optionsElement = getElementsByTagName(parent, "option");
    
    for (Element e : optionsElement) {
      allOptions.put(e.getAttribute("value"), e.getTextContent());
    }
    
    if (allOptions.isEmpty())
      throw new EoulsanException("Parsing tool xml: no option found in conditional element.");
    
    return Collections.unmodifiableMap(allOptions);
  }

  //
  // Getter
  //
  public String getName() {
    return this.name;
  }

  public ToolParameter getToolParameterSelect() {
    return toolParameterSelect;
  }

  public List<ToolParameter> getToolParametersResult() {
    return toolParametersResult;
  }

  public Map<String, ToolParameter> getOptions() {
    return options;
  }

  public Map<String, ToolParameter> getCheckedOptions() {
    return options;
  }

  @Override
  public boolean isSetting() {
    return !toolParametersResult.isEmpty();
  }

  @Override
  public String getParameterEoulsan() {
    return this.parameterEoulsan;
  }

//  @Override
//  public void setParameterEoulsan(final String paramValue) {
//
//    // Set tool parameter related
//    if (options.containsKey(paramValue)) {
//      options.get(paramValue).setParameterEoulsan(paramValue);
//
//    }
  }

  @Override
  public String toString() {
    return "ToolConditionalElement [name="
        + name + ", optionCount=" + optionCount + ", toolParameterSelect="
        + toolParameterSelect + ", options=" + options + ", checkOptions="
        + checkOptions + ", toolParametersResult=" + toolParametersResult
        + ", parameterEoulsan=" + parameterEoulsan + "]";
  }

  //
  // Constructor
  //
  ToolConditionalElement(final Element element) throws EoulsanException {

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

    this.optionCount =
        extractChildElementsByTagName(param.get(0), "option").size();

    this.toolParameterSelect = new ToolParameter(param.get(0), name);

    // Extract all case available
    this.options = extractWhenTag(element);
    this.checkOptions = Maps.newHashMap();

  }

}