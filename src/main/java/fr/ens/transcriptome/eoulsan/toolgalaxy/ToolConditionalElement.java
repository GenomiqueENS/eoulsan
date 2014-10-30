package fr.ens.transcriptome.eoulsan.toolgalaxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

public class ToolConditionalElement implements ToolElement {

  private final String name;
  private final int optionCount;

  // Parameter represent choice in option list
  private final ToolParameter paramSelect;

  // Variable name used in command tag to check choice
  private final String nameConditionalElement;

  // Variable name in command tag and tool parameter related
  private final Map<String, ToolParameter> valuesAvailable;

  private List<ToolParameter> toolParametersSelected;

  private String parameterEoulsan;

  private Map<String, ToolParameter> extractWhenTag(Element element) {

    final Map<String, ToolParameter> result = Maps.newHashMap();

    final List<Element> elemWhen =
        XMLUtils.getElementsByTagName(element, "when");

    final int whenTagCount = elemWhen.size();
    // TODO
    assert whenTagCount == optionCount : "No equals option and when element";

    for (Element e : elemWhen) {
      final String nameWhenTag = e.getAttribute("name");

      List<Element> paramElement = XMLUtils.getElementsByTagName(e, "param");
      assert paramElement.size() != 1 : "When element with more one param element.";

      result.put(nameWhenTag, new ToolParameter(paramElement.get(0), name));
    }

    return result;
  }

  public void setParameterEoulsan(Map<String, String> parametersEoulsan) {
    this.toolParametersSelected = Lists.newArrayList();

    if (valuesAvailable.isEmpty() || parametersEoulsan.isEmpty())
      return;

    for (Map.Entry<String, String> p : parametersEoulsan.entrySet()) {
      // Check value parameter corresponding to a key
      if (valuesAvailable.containsKey(p.getKey())) {

        final ToolParameter toolParameterSelected =
            valuesAvailable.get(p.getKey());
        toolParameterSelected.setParameterEoulsan(p.getValue());

        this.toolParametersSelected.add(toolParameterSelected);
      }
    }

  }

  public Collection<? extends ToolElement> getToolParameterSelected() {

    if (this.toolParametersSelected.isEmpty())
      return Collections.emptyList();

    return Collections.unmodifiableList(this.toolParametersSelected);
  }

  //
  // Getter
  //
  public String getName() {
    return this.nameConditionalElement;
  }

  public ToolParameter getParamSelect() {
    return paramSelect;
  }

  public Map<String, ToolParameter> getValuesAvailable() {
    return valuesAvailable;
  }

  @Override
  public boolean isSetting() {
    return !toolParametersSelected.isEmpty();
  }

  @Override
  public String getParameterEoulsan() {
    return this.parameterEoulsan;
  }

  @Override
  public void setParameterEoulsan(final String paramValue) {

    // Set tool parameter related
    if (valuesAvailable.containsKey(paramValue)) {
      valuesAvailable.get(paramValue).setParameterEoulsan(paramValue);

    }
  }

  // TODO test method

  public static List<Element> getChildElementsByTagName(
      final Element parentElement, final String elementName) {

    if (elementName == null || parentElement == null)
      return null;

    final NodeList nStepsList = parentElement.getChildNodes();
    if (nStepsList == null)
      return null;

    final List<Element> result = new ArrayList<Element>();

    for (int i = 0; i < nStepsList.getLength(); i++) {

      final Node node = nStepsList.item(i);

      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element e = (Element) node;

        if (e.getTagName().equals(elementName))
          result.add(e);
      }
    }

    return result;
  }

  //
  // Constructor
  //
  ToolConditionalElement(final Element element) throws EoulsanException {
    this.name = element.getAttribute("name");

    List<Element> param = getChildElementsByTagName(element, "param");

    if (param.size() != 1) {
      throw new EoulsanException(
          "Parsing tool xml: in conditionnal element, found param children element "
              + param.size() + ". Must be 1.");
    }

    if (param.get(0).getAttribute("type").equals("select")) {
      this.optionCount =
          getChildElementsByTagName(param.get(0), "option").size();

      this.paramSelect = new ToolParameter(param.get(0));

      this.nameConditionalElement = name + SEP + paramSelect.getName();

      // Extract all case available
      this.valuesAvailable = extractWhenTag(param.get(0));

    } else {
      this.optionCount = -1;
      this.paramSelect = null;
      this.nameConditionalElement = null;
      this.valuesAvailable = null;
    }
  }

}