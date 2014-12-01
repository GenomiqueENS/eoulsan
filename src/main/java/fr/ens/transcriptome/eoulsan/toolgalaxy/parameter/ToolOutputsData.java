package fr.ens.transcriptome.eoulsan.toolgalaxy.parameter;

import org.w3c.dom.Element;

public class ToolOutputsData extends AbstractToolElement {

  public static final String TAG_NAME = "data";

  private String value = "";

  @Override
  public boolean setParameterEoulsan() {
    return false;
  }

  @Override
  boolean isValueParameterValid() {
    return true;
  }

  @Override
  public String getValue() {
    return this.value;
  }

  @Override
  public boolean setParameterEoulsan(String paramValue) {
    this.value = paramValue;
    return true;
  }

  //
  // Constructor
  //

  public ToolOutputsData(Element param) {
    this(param, null);
  }

  public ToolOutputsData(Element param, String nameSpace) {
    super(param, nameSpace);
  }

}
