package fr.ens.biologie.genomique.eoulsan.galaxytools.elements;

import org.w3c.dom.Element;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;

/**
 * This class define a text parameter element.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TextParameterToolElement extends AbstractToolElement {

  /** The Constant TYPE. */
  public final static String TYPE = "text";

  /** The Constant ATT_DEFAULT_KEY. */
  private final static String ATT_DEFAULT_KEY = "value";

  /** The default value. */
  private String defaultValue = "";

  /** The value. */
  private String value = "";

  @Override
  public void setDefaultValue() throws EoulsanException {
    setValue(this.defaultValue);
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
  public void setValue(final Parameter stepParameter) throws EoulsanException {

    this.setValue(stepParameter.getStringValue());

  }

  private void setValue(final String value) throws EoulsanException {
    this.value = value;

    this.set = true;
  }

  //
  // Constructors
  //

  /**
   * Instantiates a new tool parameter integer.
   * @param param the param
   * @throws EoulsanException the eoulsan exception
   */
  public TextParameterToolElement(final Element param) throws EoulsanException {
    this(param, null);
  }

  /**
   * Instantiates a new tool parameter integer.
   * @param param the param
   * @param nameSpace the name space
   * @throws EoulsanException the eoulsan exception
   */
  public TextParameterToolElement(final Element param, final String nameSpace)
      throws EoulsanException {
    super(param, nameSpace);

    // Set value
    this.defaultValue = param.getAttribute(ATT_DEFAULT_KEY);
  }

}
