package fr.ens.transcriptome.eoulsan.toolgalaxy.parameter;

import static fr.ens.transcriptome.eoulsan.toolgalaxy.ToolInterpreter.extractChildElementsByTagName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.python.google.common.base.Joiner;
import org.w3c.dom.Element;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;

public class ToolParameterSelect extends AbstractToolElement {

  public final static String TYPE = "select";

  private static final String ATT_SELECTED_KEY = "selected";

  private static final String ATT_VALUE_KEY = "value";

  private final List<String> optionsValue;
  private final List<Element> optionsElement;

  private String value = "";

  @Override
  boolean isValueParameterValid() {
    // Check value contains in options values
    return optionsValue.contains(this.value);
  }

  @Override
  public void setParameterEoulsan() {
    // TODO Auto-generated method stub
  }

  @Override
  public void setParameterEoulsan(final Parameter stepParameter)
      throws EoulsanException {

    this.value = stepParameter.getStringValue();
    isSetting = true;

    if (!isValueParameterValid()) {
      throw new EoulsanException("ToolGalaxy step: parameter "
          + this.getName() + " value setting : " + value
          + " is invalid. \n\tAvailable values: "
          + Joiner.on(",").join(optionsValue));
    }
  }

  private List<String> extractAllOptions() throws EoulsanException {

    final List<String> options = new ArrayList<>();

    for (Element e : optionsElement) {
      options.add(e.getAttribute(ATT_VALUE_KEY));

      // Check default settings
      final String attributeSelected = e.getAttribute(ATT_SELECTED_KEY);
      if (!attributeSelected.isEmpty()) {
        this.value = e.getAttribute(ATT_VALUE_KEY);
        isSetting = true;
      }
    }

    if (options.isEmpty()) {
      // throw new EoulsanException(
      // "Parsing tool xml: no option found in conditional element: "
      // + getName());
      return Collections.emptyList();
    }

    return Collections.unmodifiableList(options);
  }

  @Override
  public String toString() {
    return "ToolParameterSelect [" + super.toString() + "]";
  }

  @Override
  public String getValue() {
    return this.value;
  }

  //
  // Constructor
  //
  public ToolParameterSelect(final Element param) throws EoulsanException {
    this(param, null);
  }

  public ToolParameterSelect(final Element param, final String nameSpace)
      throws EoulsanException {
    super(param, nameSpace);

    this.optionsElement = extractChildElementsByTagName(param, "option");
    this.optionsValue = extractAllOptions();

  }

}
