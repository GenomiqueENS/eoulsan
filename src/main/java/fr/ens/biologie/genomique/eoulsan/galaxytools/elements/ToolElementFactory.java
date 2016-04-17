package fr.ens.biologie.genomique.eoulsan.galaxytools.elements;

import org.w3c.dom.Element;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;

/**
 * This class define a factory for ToolElement objects.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ToolElementFactory {

  /**
   * Gets the instance tool element.
   * @param param the param
   * @return the instance tool element
   * @throws EoulsanException the eoulsan exception
   */
  public static ToolElement newToolElement(final Element param)
      throws EoulsanException {
    return ToolElementFactory.newToolElement(param, null);
  }

  /**
   * Gets the instance tool element.
   * @param tag the param
   * @param nameSpace the name space
   * @return the instance tool element
   * @throws EoulsanException the eoulsan exception
   */
  public static ToolElement newToolElement(final Element tag,
      final String nameSpace) throws EoulsanException {

    if (tag == null) {
      throw new EoulsanException(
          "Parsing xml: no element param found to instantiate a tool element.");
    }

    final String tagName = tag.getTagName();

    if (tagName.equals(DataToolElement.TAG_NAME)) {
      return new DataToolElement(tag, nameSpace);
    }

    // Instantiate a tool parameter according to attribute type value
    final String type =
        tag.getAttribute("type").toLowerCase(Globals.DEFAULT_LOCALE);

    final ToolElement toolElement;

    switch (type) {

    case BooleanParameterToolElement.TYPE:
      toolElement = new BooleanParameterToolElement(tag, nameSpace);
      break;
    case IntegerParameterToolElement.TYPE:
      toolElement = new IntegerParameterToolElement(tag, nameSpace);
      break;
    case FloatParameterToolElement.TYPE:
      toolElement = new FloatParameterToolElement(tag, nameSpace);
      break;
    case SelectParameterToolElement.TYPE:
      toolElement = new SelectParameterToolElement(tag, nameSpace);
      break;

    default:
      toolElement = new DataParameterToolElement(tag, nameSpace);
      break;
    }

    return toolElement;
  }

}
