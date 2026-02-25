package fr.ens.biologie.genomique.eoulsan.galaxytools.elements;

import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.newEoulsanException;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.galaxytools.ToolInfo;
import org.w3c.dom.Element;

/**
 * This class define a factory for ToolElement objects.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ToolElementFactory {

  /**
   * Gets the instance tool element.
   *
   * @param toolInfo ToolInfo object
   * @param param the parameter
   * @return the instance tool element
   * @throws EoulsanException if an error occurs while creating ToolElement
   */
  public static ToolElement newToolElement(final ToolInfo toolInfo, final Element param)
      throws EoulsanException {
    return ToolElementFactory.newToolElement(toolInfo, param, null);
  }

  /**
   * Gets the instance tool element.
   *
   * @param toolInfo ToolInfo object
   * @param tag the parameter
   * @param nameSpace the name space
   * @return the instance tool element
   * @throws EoulsanException if an error occurs while creating ToolElement
   */
  public static ToolElement newToolElement(
      final ToolInfo toolInfo, final Element tag, final String nameSpace) throws EoulsanException {

    if (tag == null) {
      throw newEoulsanException(toolInfo, "no element found to instantiate a tool element");
    }

    final String tagName = tag.getTagName();

    if (tagName.equals(DataToolElement.TAG_NAME)) {
      return new DataToolElement(toolInfo, tag, nameSpace);
    }

    // Instantiate a tool parameter according to attribute type value
    final String type = tag.getAttribute("type").toLowerCase(Globals.DEFAULT_LOCALE);

    final ToolElement toolElement;

    switch (type) {
      case BooleanParameterToolElement.TYPE:
        toolElement = new BooleanParameterToolElement(toolInfo, tag, nameSpace);
        break;
      case IntegerParameterToolElement.TYPE:
        toolElement = new IntegerParameterToolElement(toolInfo, tag, nameSpace);
        break;
      case FloatParameterToolElement.TYPE:
        toolElement = new FloatParameterToolElement(toolInfo, tag, nameSpace);
        break;
      case SelectParameterToolElement.TYPE:
        toolElement = new SelectParameterToolElement(toolInfo, tag, nameSpace);
        break;
      case TextParameterToolElement.TYPE:
        toolElement = new TextParameterToolElement(toolInfo, tag, nameSpace);
        break;
      case DataToolElement.TYPE:
        toolElement = new DataToolElement(toolInfo, tag, nameSpace);
        break;

      default:
        throw new EoulsanException("Unknown parameter type:" + type);
    }

    return toolElement;
  }
}
