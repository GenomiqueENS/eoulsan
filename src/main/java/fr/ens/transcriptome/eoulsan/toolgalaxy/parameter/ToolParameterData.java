package fr.ens.transcriptome.eoulsan.toolgalaxy.parameter;

import org.w3c.dom.Element;

public class ToolParameterData extends AbstractToolElement {

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
  public boolean setParameterEoulsan() {
    return true;
  }

  @Override
  public String getValue() {
    return this.value;
  }

  //
  // Constructor
  //
  public ToolParameterData(final Element param) {
    this(param, null);
  }

  public ToolParameterData(Element param, String nameSpace) {
    super(param, nameSpace);
    isSetting = true;
  }

}
