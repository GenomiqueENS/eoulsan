package fr.ens.biologie.genomique.eoulsan.galaxytools.elements;

import org.w3c.dom.Element;

/**
 * This class define an abstract class for tool element parameters.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractParameterToolElement extends AbstractToolElement {

  /**
   * Test if the parameter value is valid
   *
   * @return true if the paramter value is valid
   */
  public abstract boolean isParameterValueValid();

  /**
   * Checks if the value has been set (is not the default value).
   *
   * @return true if the value has been set
   */
  public abstract boolean isSet();

  //
  // Constructor
  //

  /**
   * Protected constructor.
   *
   * @param param parameter element
   * @param nameSpace name space
   */
  protected AbstractParameterToolElement(final Element param, final String nameSpace) {
    super(param, nameSpace);
  }
}
