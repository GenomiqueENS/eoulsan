package fr.ens.transcriptome.eoulsan.toolgalaxy.parameter;

import java.util.List;

import org.python.google.common.collect.Lists;
import org.w3c.dom.Element;

import fr.ens.transcriptome.eoulsan.Globals;

public class ToolParameterBoolean extends AbstractToolParameter {

  public final static String TYPE = "boolean";

  private final static String ATT_CHECKED_KEY = "checked";
  private final static String ATT_TRUEVALUE_KEY = "truevalue";
  private final static String ATT_FALSEVALUE_KEY = "falsevalue";

  private final static List<String> CHECKED_VALUES = Lists.newArrayList("yes",
      "on", "true");

  private final String checked;
  private final String trueValue;
  private final String falseValue;

  private String value = "";

  @Override
  public boolean isValueParameterValid() {
    return true;
  }

  public String getValue() {
    return this.value;
  }

  public boolean setParameterEoulsan() {
    // Set value to the default value
    if (this.value.isEmpty())
      this.value = trueValue;

    isSetting = true;
    return isValueParameterValid();
  }

  @Override
  public boolean setParameterEoulsan(final String paramValue) {

    if (CHECKED_VALUES.contains(paramValue.toLowerCase(Globals.DEFAULT_LOCALE)))
      this.value = trueValue;
    else
      this.value = falseValue;

    isSetting = true;

    return isValueParameterValid();
  }

  @Override
  public String toString() {
    return "ToolParameterBoolean [checked="
        + checked + ", trueValue=" + trueValue + ", falseValue=" + falseValue
        + ", value=" + value + "]";
  }

  //
  // Constructor
  //
  public ToolParameterBoolean(final Element param) {
    this(param, null);
  }

  public ToolParameterBoolean(Element param, String prefixName) {
    super(param, prefixName);

    this.checked =
        param.getAttribute(ATT_CHECKED_KEY).toLowerCase(Globals.DEFAULT_LOCALE);

    this.trueValue = param.getAttribute(ATT_TRUEVALUE_KEY);

    this.falseValue = param.getAttribute(ATT_FALSEVALUE_KEY);

    // Set default if define
    if (CHECKED_VALUES.contains(checked)) {
      this.value = trueValue;
    }

  }
}
