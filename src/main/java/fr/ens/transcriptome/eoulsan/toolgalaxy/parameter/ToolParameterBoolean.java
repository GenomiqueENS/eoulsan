package fr.ens.transcriptome.eoulsan.toolgalaxy.parameter;

import java.util.List;

import org.python.google.common.collect.Lists;
import org.w3c.dom.Element;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.Parameter;

public class ToolParameterBoolean extends AbstractToolElement {

  public final static String TYPE = "boolean";

  private final static String ATT_CHECKED_KEY = "checked";
  private final static String ATT_TRUEVALUE_KEY = "truevalue";
  private final static String ATT_FALSEVALUE_KEY = "falsevalue";

  private final static List<String> CHECKED_VALUES = Lists.newArrayList("yes",
      "on", "true");

  private final String checked_lowered;
  private final String trueValue;
  private final String falseValue;

  private String value = "";

  @Override
  public boolean isValueParameterValid() {
    return true;
  }

  @Override
  public String getValue() {
    return this.value;
  }

  @Override
  public void setParameterEoulsan() {
    // Set value to the default value
    if (this.value.isEmpty()) {
      this.value = trueValue;
    }

    isSetting = true;
  }

  @Override
  public void setParameterEoulsan(final Parameter stepParameter) {

    final boolean valueParameter = stepParameter.getBooleanValue();
    this.value = valueParameter ? trueValue : falseValue;

    this.isSetting = true;

  }

  @Override
  public String toString() {
    return "ToolParameterBoolean [checked="
        + checked_lowered + ", trueValue=" + trueValue + ", falseValue="
        + falseValue + ", value=" + value + "]";
  }

  //
  // Constructor
  //
  public ToolParameterBoolean(final Element param) {
    this(param, null);
  }

  public ToolParameterBoolean(Element param, String nameSpace) {
    super(param, nameSpace);

    this.checked_lowered =
        param.getAttribute(ATT_CHECKED_KEY).toLowerCase(Globals.DEFAULT_LOCALE);

    this.trueValue = param.getAttribute(ATT_TRUEVALUE_KEY);

    this.falseValue = param.getAttribute(ATT_FALSEVALUE_KEY);

    // Set default if define
    if (CHECKED_VALUES.contains(checked_lowered)) {
      this.value = trueValue;
    }

  }
}
