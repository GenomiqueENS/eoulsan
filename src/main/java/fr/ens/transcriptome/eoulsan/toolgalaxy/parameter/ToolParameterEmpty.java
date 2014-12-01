package fr.ens.transcriptome.eoulsan.toolgalaxy.parameter;

import java.util.Map;

public class ToolParameterEmpty implements ToolElement {

  public ToolParameterEmpty() {
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return "No name";
  }

  @Override
  public boolean isSetting() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String getValue() {
    // TODO Auto-generated method stub
    return "No Value";
  }

  @Override
  public boolean setParameterEoulsan() {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public boolean setParameterEoulsan(String value) {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public void setParameterEoulsan(Map<String, String> parametersEoulsan) {
    // TODO Auto-generated method stub

  }

}
