package fr.ens.transcriptome.eoulsan.toolgalaxy.parameter;

import java.util.Map;

import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;

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
  public void setParameterEoulsan() {
    // TODO Auto-generated method stub
  }

  @Override
  public void setParameterEoulsan(final Parameter stepParameter) {
    // TODO Auto-generated method stub
  }

  @Override
  public void setParameterEoulsan(final Map<String, Parameter> stepParameters) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean isFile() {
    return false;
  }

  @Override
  public DataFormat getDataFormat() {
    throw new UnsupportedOperationException();
  }
}
