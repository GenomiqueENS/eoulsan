package fr.ens.transcriptome.eoulsan.toolgalaxy.parameter;

import static fr.ens.transcriptome.eoulsan.toolgalaxy.ToolInterpreter.extractChildElementsByTagName;

import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;

public class ToolParameterSelect extends AbstractToolParameter {

  public final static String TYPE = "select";

  private static final String ATT_SELECTED_KEY = "selected";

  private static final String ATT_VALUE_KEY = "value";

  private final List<String> optionsValue;
  private final List<Element> optionsElement;

  private String value;

  @Override
  boolean isValueParameterValid() {
    // Check value contains in options values
    return optionsValue.contains(this.value);
  }

  @Override
  public String getValue() {
    return this.value;
  }

  @Override
  public boolean setParameterEoulsan(String paramValue) {

    this.value = paramValue;
    isSetting = true;

    return isValueParameterValid();
  }

  private List<String> extractAllOptions() throws EoulsanException {

    final List<String> options = Lists.newArrayList();

    for (Element e : optionsElement) {
      options.add(e.getAttribute(ATT_VALUE_KEY));

      // Check default settings
      final String attributeSelected = e.getAttribute(ATT_SELECTED_KEY);
      if (!attributeSelected.isEmpty()) {
        this.value = e.getAttribute(ATT_VALUE_KEY);
        isSetting = true;
      }
    }

    if (options.isEmpty())
      throw new EoulsanException(
          "Parsing tool xml: no option found in conditional element.");

    return Collections.unmodifiableList(options);
  }

  @Override
  public String toString() {
    return "ToolParameterSelect [" + super.toString() + "]";
  }

  //
  // Constructor
  //
  public ToolParameterSelect(final Element param) throws EoulsanException {
    this(param, null);
  }

  public ToolParameterSelect(final Element param, final String prefixName)
      throws EoulsanException {
    super(param, prefixName);

    this.optionsElement = extractChildElementsByTagName(param, "option");
    this.optionsValue = extractAllOptions();

  }

  @Override
  public boolean setParameterEoulsan() {
    // TODO Auto-generated method stub
    return false;
  }

}
