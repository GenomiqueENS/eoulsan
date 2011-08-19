/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.util;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class define utility methods for parsing XML.
 * @author Laurent Jourdren
 */
public final class XMLUtils {

  //
  // Other methods
  //

  /**
   * Get a list of the attribute names of an element.
   * @param e Element to use
   * @return a list with all the names of the attributes of the node
   */
  public static List<String> getAttributeNames(final Element e) {

    if (e == null)
      return null;

    final List<String> result = new ArrayList<String>();

    final NamedNodeMap map = e.getAttributes();

    for (int i = 0; i < map.getLength(); i++) {

      final Node attribute = map.item(i);

      result.add(attribute.getNodeName());
    }

    return result;
  }

  /**
   * Get the value of an attribute.
   * @param e Element to use
   * @param attributeName name of the attribute
   * @return the value of the attribute
   */
  public static String getAttributeValue(final Element e,
      final String attributeName) {

    if (e == null || attributeName == null)
      return null;

    return e.getAttribute(attributeName);
  }

  /**
   * Get the list of all the elements of a parent element.
   * @param parentElement the parent element
   * @param elementName the name of the element
   * @return the list of the elements
   */
  public static List<Element> getElementsByTagName(final Element parentElement,
      final String elementName) {

    if (elementName == null || parentElement == null)
      return null;

    final NodeList nStepsList = parentElement.getElementsByTagName(elementName);
    if (nStepsList == null)
      return null;

    final List<Element> result = new ArrayList<Element>();

    for (int i = 0; i < nStepsList.getLength(); i++) {

      final Node node = nStepsList.item(i);

      if (node.getNodeType() == Node.ELEMENT_NODE)
        result.add((Element) node);
    }

    return result;
  }

  /**
   * Get the list of all the first level elements of a document.
   * @param document the document object
   * @param elementName the name of the element
   * @return the list of the elements
   */
  public static List<Element> getElementsByTagName(final Document document,
      final String elementName) {

    if (elementName == null || document == null)
      return null;

    final NodeList nStepsList = document.getElementsByTagName(elementName);
    if (nStepsList == null)
      return null;

    final List<Element> result = new ArrayList<Element>();

    for (int i = 0; i < nStepsList.getLength(); i++) {

      final Node node = nStepsList.item(i);

      if (node.getNodeType() == Node.ELEMENT_NODE)
        result.add((Element) node);
    }

    return result;
  }

  /**
   * Get the value of a tag.
   * @param parentElement the tag element
   * @param elementName name of the tag
   * @return the value of the tag
   */
  public static String getTagValue(final Element parentElement,
      final String elementName) {

    if (parentElement == null || elementName == null)
      return null;

    String result = null;

    for (Element e : XMLUtils.getElementsByTagName(parentElement, elementName))
      result = e.getTextContent();

    return result;
  }

  /**
   * Get the values of a tag.
   * @param parentElement the tag element
   * @param elementName name of the tag
   * @return a list of string with the values of the tag
   */
  public static List<String> getTagValues(final Element parentElement,
      final String elementName) {

    if (parentElement == null || elementName == null)
      return null;

    List<String> result = new ArrayList<String>();

    for (Element e : XMLUtils.getElementsByTagName(parentElement, elementName))
      result.add(e.getTextContent());

    return result;
  }

}
