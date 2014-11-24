package fr.ens.transcriptome.eoulsan.toolgalaxy.parameter;

import org.w3c.dom.Element;

public class ToolParameter extends AbstractToolParameter {

  private String value = "";

  @Override
  boolean isValueParameterValid() {
    return true;
  }

  @Override
  public boolean setParameterEoulsan(String paramValue) {
    this.value = paramValue;
    return true;
  }

  @Override
  public String getValue() {
    return this.value;
  }

  //
  // Constructor
  //
  public ToolParameter(final Element param) {
    this(param, null);
  }

  public ToolParameter(Element param, String prefixName) {
    super(param, prefixName);
    isSetting = true;
  }

  @Override
  public boolean setParameterEoulsan() {
    // TODO Auto-generated method stub
    return true;
  }

}
