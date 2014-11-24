package fr.ens.transcriptome.eoulsan.toolgalaxy.parameter;

import org.w3c.dom.Element;

import fr.ens.transcriptome.eoulsan.EoulsanException;

public class ToolParameterFloat extends AbstractToolParameter {

  public final static String TYPE = "float";

  private final static String ATT_DEFAULT_KEY = "value";
  private final static String ATT_MIN_KEY = "min";
  private final static String ATT_MAX_KEY = "max";

  private final double min;
  private final double max;

  private double value;

  @Override
  public boolean isValueParameterValid() {
    return this.value >= this.min && this.value <= this.max;
  }

  @Override
  public boolean setParameterEoulsan(final String paramValue) {

    this.value = Integer.parseInt(paramValue);
    isSetting = true;

    return isValueParameterValid();
  }

  public String getValue() {
    return "" + this.value;
  }

  @Override
  public String toString() {
    return "ToolParameterFloat [min="
        + min + ", max=" + max + ", value=" + value + "]";
  }

  //
  // Constructor
  //
  public ToolParameterFloat(final Element param) throws EoulsanException {
    this(param, null);
  }

  public ToolParameterFloat(Element param, String prefixName)
      throws EoulsanException {
    super(param, prefixName);

    try {
      double defaultValue =
          Double.parseDouble(param.getAttribute(ATT_DEFAULT_KEY));

      this.value = defaultValue;

    } catch (NumberFormatException e) {
      throw new EoulsanException("No found default value for parameter "
          + getName());
    }

    try {
      String value = param.getAttribute(ATT_MIN_KEY);
      this.min = value.isEmpty() ? Double.MIN_VALUE : Double.parseDouble(value);

      value = param.getAttribute(ATT_MAX_KEY);
      this.max = value.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(value);

    } catch (NumberFormatException e) {
      throw new EoulsanException("Fail extract value " + e.getMessage());
    }
  }

  @Override
  public boolean setParameterEoulsan() {
    // TODO Auto-generated method stub
    return false;
  }
}
