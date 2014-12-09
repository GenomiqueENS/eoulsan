package fr.ens.transcriptome.eoulsan.toolgalaxy.parameter;

import org.w3c.dom.Element;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;

public class ToolParameterInteger extends AbstractToolElement {

  public final static String TYPE = "integer";

  private final static String ATT_DEFAULT_KEY = "value";
  private final static String ATT_MIN_KEY = "min";
  private final static String ATT_MAX_KEY = "max";

  private final int min;
  private final int max;

  private int value;

  @Override
  public boolean isValueParameterValid() {
    return this.value >= this.min && this.value <= this.max;
  }

  @Override
  public void setParameterEoulsan() {
    // TODO Auto-generated method stub
  }

  @Override
  public void setParameterEoulsan(final Parameter stepParameter)
      throws EoulsanException {

    this.value = stepParameter.getIntValue();

    isSetting = true;

    if (!isValueParameterValid()) {
      throw new EoulsanException("ToolGalaxy step: parameter "
          + this.getName() + " value setting for step: "
          + value + ". Invalid to interval [" + min + "," + max + "]");
    }
  }

  @Override
  public String getValue() {
    return "" + this.value;
  }

  @Override
  public String toString() {
    return "ToolParameterInteger [min="
        + min + ", max=" + max + ", value=" + value + "]";
  }

  //
  // Constructor
  //
  public ToolParameterInteger(final Element param) throws EoulsanException {
    this(param, null);
  }

  public ToolParameterInteger(Element param, String nameSpace)
      throws EoulsanException {
    super(param, nameSpace);

    try {
      int defaultValue = Integer.parseInt(param.getAttribute(ATT_DEFAULT_KEY));

      // Set value
      this.value = defaultValue;

    } catch (NumberFormatException e) {
      throw new EoulsanException("No found default value for parameter "
          + getName());
    }

    try {
      String value = param.getAttribute(ATT_MIN_KEY);
      this.min = value.isEmpty() ? Integer.MIN_VALUE : Integer.parseInt(value);

      value = param.getAttribute(ATT_MAX_KEY);
      this.max = value.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(value);

    } catch (NumberFormatException e) {
      throw new EoulsanException("Fail extract value " + e.getMessage());
    }
  }

}
