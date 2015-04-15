/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
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
 * @since 1.1
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

    if (e == null) {
      return null;
    }

    final List<String> result = new ArrayList<>();

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

    if (e == null || attributeName == null) {
      return null;
    }

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

    if (elementName == null || parentElement == null) {
      return null;
    }

    final NodeList nStepsList = parentElement.getElementsByTagName(elementName);
    if (nStepsList == null) {
      return null;
    }

    final List<Element> result = new ArrayList<>();

    for (int i = 0; i < nStepsList.getLength(); i++) {

      final Node node = nStepsList.item(i);

      if (node.getNodeType() == Node.ELEMENT_NODE) {
        result.add((Element) node);
      }
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

    if (elementName == null || document == null) {
      return null;
    }

    final NodeList nStepsList = document.getElementsByTagName(elementName);
    if (nStepsList == null) {
      return null;
    }

    final List<Element> result = new ArrayList<>();

    for (int i = 0; i < nStepsList.getLength(); i++) {

      final Node node = nStepsList.item(i);

      if (node.getNodeType() == Node.ELEMENT_NODE) {
        result.add((Element) node);
      }
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

    if (parentElement == null || elementName == null) {
      return null;
    }

    String result = null;

    for (Element e : XMLUtils.getElementsByTagName(parentElement, elementName)) {
      result = e.getTextContent();
    }

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

    if (parentElement == null || elementName == null) {
      return null;
    }

    List<String> result = new ArrayList<>();

    for (Element e : XMLUtils.getElementsByTagName(parentElement, elementName)) {
      result.add(e.getTextContent());
    }

    return result;
  }

  /**
   * Add an element to a parent element with its value.
   * @param doc DOM document object
   * @param parentElement parent element
   * @param elementName name of the element
   * @param value text of the element
   */
  public static void addTagValue(final Document doc,
      final Element parentElement, final String elementName, final String value) {

    if (doc == null
        || parentElement == null || elementName == null || value == null) {
      return;
    }

    Element child = doc.createElement(elementName);
    parentElement.appendChild(child);
    child.appendChild(doc.createTextNode(value));
  }

  /**
   * Checks if is element exists by tag name.
   * @param element the element
   * @param tagName the tag name
   * @return true, if is element exists by tag name
   */
  public static boolean isElementExistsByTagName(final Element element,
      final String tagName) {

    if (element == null || tagName == null || tagName.isEmpty()) {
      return false;
    }

    // Extract all children on element
    final NodeList res = element.getChildNodes();

    for (int i = 0; i < res.getLength(); i++) {

      final Node node = res.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        final Element elem = (Element) node;

        // Check matching with tagname expected
        if (elem.getTagName().equals(tagName)) {
          return true;
        }
      }
    }

    return false;
  }
}
