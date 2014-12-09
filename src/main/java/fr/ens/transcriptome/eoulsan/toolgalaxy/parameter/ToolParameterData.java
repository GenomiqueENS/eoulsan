package fr.ens.transcriptome.eoulsan.toolgalaxy.parameter;

import java.util.List;

import org.w3c.dom.Element;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;

public class ToolParameterData extends AbstractToolElement {

  private final List<String> formats;
  private final DataFormat dataFormat;

  private String value = "";

  @Override
  boolean isValueParameterValid() {
    return true;
  }

  @Override
  public void setParameterEoulsan(final Parameter stepParameter) {
    this.value = stepParameter.getValue();
  }

  @Override
  public void setParameterEoulsan() {
  }

  @Override
  public String getValue() {
    return this.value;
  }

  @Override
  public boolean isFile() {
    return dataFormat != null;
  }

  @Override
  public DataFormat getDataFormat() {
    if (isFile()) {
      return this.dataFormat;
    }

    throw new UnsupportedOperationException();
  }

  //
  // Constructor
  //
  public ToolParameterData(final Element param) throws EoulsanException {
    this(param, null);
  }

  public ToolParameterData(Element param, String nameSpace)
      throws EoulsanException {
    super(param, nameSpace);
    isSetting = true;

    this.formats = COMMA.splitToList(param.getAttribute("format"));

    // Check count format found
    // TODO
    // if (this.formats.size() > 1) {
    // throw new EoulsanException(
    // "Parsing tool xml: more one format data found,"
    // + Joiner.on(",").join(formats) + " invalid.");
    // }

    if (this.formats.isEmpty()) {
      this.dataFormat = null;
    } else {
      // Convert format in DataFormat
      this.dataFormat = ConvertorToDataFormat.convert(formats.get(0));
    }
  }
}
