package fr.ens.biologie.genomique.eoulsan.galaxytools.elements;

import org.w3c.dom.Element;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;

/**
 * This class define a text tool element parameter.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TextParameterToolElement extends AbstractParameterToolElement {

  /** The Constant TYPE. */
  public final static String TYPE = "text";

  /** The Constant ATT_DEFAULT_KEY. */
  private final static String ATT_DEFAULT_KEY = "value";

  /** The value. */
  private String value = "";

  private boolean set;

  //
  // Getters
  //

  @Override
  public boolean isParameterValueValid() {
    return true;
  }

  @Override
  public String getValue() {
    return this.value;
  }

  @Override
  public boolean isSet() {
    return this.set;
  }

  //
  // Setters
  //

  @Override
  public void setValue(final String value) throws EoulsanException {
    this.value = value;
    this.set = true;
  }

  //
  // Constructors
  //

  /**
   * Instantiates a new text tool element parameter.
   * @param param the parameter
   * @throws EoulsanException if an error occurs while setting the value
   */
  public TextParameterToolElement(final Element param) throws EoulsanException {
    this(param, null);
  }

  /**
   * Instantiates a new text tool element parameter.
   * @param param the parameter
   * @param nameSpace the name space
   * @throws EoulsanException if an error occurs while setting the value
   */
  public TextParameterToolElement(final Element param, final String nameSpace)
      throws EoulsanException {
    super(param, nameSpace);

    // Set the default value
    this.value = param.getAttribute(ATT_DEFAULT_KEY);
  }

}
