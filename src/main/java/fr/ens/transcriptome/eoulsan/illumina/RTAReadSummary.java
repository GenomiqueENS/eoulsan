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

package fr.ens.transcriptome.eoulsan.illumina;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class RTAReadSummary implements Iterable<RTALaneSummary> {

  private int id;
  private String type;
  private double densityRatio;
  private List<RTALaneSummary> lanes = new ArrayList<RTALaneSummary>();

  //
  // Getters
  //

  /**
   * Get the id of the read
   * @return Returns the id
   */
  public int getId() {
    return id;
  }

  /**
   * Return the type of the read
   * @return Returns the type
   */
  public String getType() {
    return type;
  }

  /**
   * Get the density ration for the read
   * @return Returns the densityRatio
   */
  public double getDensityRatio() {
    return densityRatio;
  }

  //
  // Parser
  //

  private void parse(final Document document) {

    final NodeList nSummaryList = document.getElementsByTagName("Summary");

    for (int i = 0; i < nSummaryList.getLength(); i++) {

      Node nNode = nSummaryList.item(i);
      if (nNode.getNodeType() == Node.ELEMENT_NODE) {

        final Element e = (Element) nNode;

        System.out.println(getAttributeNames(e));

        for (String attributeName : getAttributeNames(e)) {

          if ("Read".equals(attributeName))
            this.id = Integer.parseInt(getAttributeValue(e, attributeName));
          else if ("ReadType".equals(attributeName))
            this.type = getAttributeValue(e, attributeName);
          else if ("densityRatio".equals(attributeName))
            this.densityRatio =
                Double.parseDouble(getAttributeValue(e, attributeName));

        }

        final NodeList nStepsList = e.getElementsByTagName("Lane");

        for (int j = 0; j < nStepsList.getLength(); j++) {

          final Node node = nStepsList.item(j);

          if (node.getNodeType() == Node.ELEMENT_NODE) {

            final Element laneElement = (Element) node;

            final RTALaneSummary lane = new RTALaneSummary(this.densityRatio);
            lane.parse(laneElement);
            this.lanes.add(lane);
          }
        }

      }

    }

  }

  public void parse(final File file) throws ParserConfigurationException,
      SAXException, IOException {

    parse(new FileInputStream(file));
  }

  public void parse(final InputStream is) throws ParserConfigurationException,
      SAXException, IOException {

    final Document doc;

    final DocumentBuilderFactory dbFactory =
        DocumentBuilderFactory.newInstance();
    final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    doc = dBuilder.parse(is);
    doc.getDocumentElement().normalize();

    parse(doc);
    
    is.close();
  }

  //
  // Other methods
  //

  private List<String> getAttributeNames(final Element e) {

    final List<String> result = new ArrayList<String>();

    final NamedNodeMap map = e.getAttributes();

    for (int i = 0; i < map.getLength(); i++) {

      final Node attribute = map.item(i);

      result.add(attribute.getNodeName());
    }

    return result;
  }

  private String getAttributeValue(final Element e, final String attributeName) {

    return e.getAttribute(attributeName);
  }

  //
  // Iterable method
  //

  @Override
  public Iterator<RTALaneSummary> iterator() {

    return this.lanes.iterator();
  }

  //
  // Object method
  //

  @Override
  public String toString() {

    return this.getClass().getSimpleName()
        + "{id=" + this.id + ", type=" + this.type + ", densityRatio="
        + this.densityRatio + ", lane=" + this.lanes + "}";
  }

  public static void main(String[] args) throws ParserConfigurationException,
      SAXException, IOException {

    File f =
        new File(
            "/import/disir01/hiseq_data/work/110628_SNL110_0025_AB0866ABXX"
                + "/Data/reports/Summary/read1.xml");

    new RTAReadSummary().parse(f);

  }

}
