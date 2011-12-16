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

import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getAttributeNames;
import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getAttributeValue;

import java.io.File;
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
import org.xml.sax.SAXException;

import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

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

  public void parse(final File file) throws ParserConfigurationException,
      SAXException, IOException {

    parse(FileUtils.createInputStream(file));
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

  private void parse(final Document document) {

    for (Element e : XMLUtils.getElementsByTagName(document, "Summary")) {

      // Parse Summary tag attributes
      for (String attributeName : getAttributeNames(e)) {

        if ("Read".equals(attributeName))
          this.id = Integer.parseInt(getAttributeValue(e, attributeName));
        else if ("ReadType".equals(attributeName))
          this.type = getAttributeValue(e, attributeName);
        else if ("densityRatio".equals(attributeName))
          this.densityRatio =
              Double.parseDouble(getAttributeValue(e, attributeName));
      }

      // Parse Lane tag
      for (Element laneElement : XMLUtils.getElementsByTagName(e, "Lane")) {

        final RTALaneSummary lane =
            new RTALaneSummary(this.id, this.densityRatio);
        lane.parse(laneElement);
        this.lanes.add(lane);
      }

    }

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

}
