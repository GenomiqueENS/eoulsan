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
import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getElementsByTagName;
import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getTagValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import fr.ens.transcriptome.eoulsan.util.XMLUtils;

public class RunInfo {

  public static class Read {

    private int number;
    private int numberCycles;
    private boolean indexedRead;

    /**
     * @return Returns the number
     */
    public int getNumber() {
      return number;
    }

    /**
     * @return Returns the numberCycles
     */
    public int getNumberCycles() {
      return numberCycles;
    }

    /**
     * @return Returns the indexedRead
     */
    public boolean isIndexedRead() {
      return indexedRead;
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName()
          + "{number=" + this.number + ", numberCycles=" + this.numberCycles
          + ", indexedRead=" + this.indexedRead + "}";
    }

  }

  private String id;
  private int number;
  private String flowCell;
  private String instrument;
  private String date;
  private List<Read> reads = new ArrayList<Read>();

  // FlowcellLayout

  private int flowCellLaneCount;
  private int flowCellSurfaceCount;
  private int flowCellSwathCount;
  private int flowCellTileCount;

  private List<Integer> alignToPhix = new ArrayList<Integer>();

  //
  // Getters
  //

  /**
   * @return Returns the id
   */
  public String getId() {
    return id;
  }

  /**
   * @return Returns the number
   */
  public int getNumber() {
    return number;
  }

  /**
   * @return Returns the flowCell
   */
  public String getFlowCell() {
    return flowCell;
  }

  /**
   * @return Returns the instrument
   */
  public String getInstrument() {
    return instrument;
  }

  /**
   * @return Returns the date
   */
  public String getDate() {
    return date;
  }

  /**
   * @return Returns the reads
   */
  public List<Read> getReads() {
    return Collections.unmodifiableList(this.reads);
  }

  /**
   * @return Returns the flowCellLaneCount
   */
  public int getFlowCellLaneCount() {
    return flowCellLaneCount;
  }

  /**
   * @return Returns the flowCellSurfaceCount
   */
  public int getFlowCellSurfaceCount() {
    return flowCellSurfaceCount;
  }

  /**
   * @return Returns the flowCellSwathCount
   */
  public int getFlowCellSwathCount() {
    return flowCellSwathCount;
  }

  /**
   * @return Returns the flowCellTileCount
   */
  public int getFlowCellTileCount() {
    return flowCellTileCount;
  }

  /**
   * @return Returns the alignToPhix
   */
  public List<Integer> getAlignToPhix() {
    return Collections.unmodifiableList(alignToPhix);
  }

  //
  // Parser
  //

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

  private void parse(final Document document) {

    for (Element e : XMLUtils.getElementsByTagName(document, "RunInfo")) {

      for (Element e1 : XMLUtils.getElementsByTagName(e, "Run")) {

        // Parse attribute of the Run tag
        for (String name : XMLUtils.getAttributeNames(e1)) {

          final String value = e1.getAttribute(name);

          if ("Id".equals(name))
            this.id = value;
          else if ("Number".equals(name))
            this.number = Integer.parseInt(value);
        }

        this.flowCell = getTagValue(e1, "Flowcell");
        this.instrument = getTagValue(e1, "Instrument");
        this.date = getTagValue(e1, "Date");

        // Parse Reads tag
        for (Element e2 : XMLUtils.getElementsByTagName(e1, "Reads"))
          for (Element e3 : XMLUtils.getElementsByTagName(e2, "Read")) {

            final Read read = new Read();

            for (String name : XMLUtils.getAttributeNames(e3)) {

              final String value = e3.getAttribute(name);

              if ("Number".equals(name))
                read.number = Integer.parseInt(value);
              else if ("NumCycles".equals(name))
                read.numberCycles = Integer.parseInt(value);
              else if ("IsIndexedRead".equals(name))
                read.indexedRead = "Y".equals(value.toUpperCase().trim());
            }

            this.reads.add(read);
          }

        // Parse FlowcellLayout tag
        for (Element e2 : getElementsByTagName(e1, "FlowcellLayout")) {

          for (String name : getAttributeNames(e2)) {

            final int value = Integer.parseInt(e2.getAttribute(name));

            if ("LaneCount".equals(name))
              this.flowCellLaneCount = value;
            else if ("SurfaceCount".equals(name))
              this.flowCellSurfaceCount = value;
            else if ("SwathCount".equals(name))
              this.flowCellSwathCount = value;
            else if ("TileCount".equals(name))
              this.flowCellTileCount = value;
          }

        }

        for (Element e2 : getElementsByTagName(e1, "AlignToPhiX"))
          for (Element e3 : getElementsByTagName(e2, "Lane")) {
            this.alignToPhix.add(Integer.parseInt(e3.getTextContent()));
          }

      }

    }

  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return this.getClass().getSimpleName()
        + "{id=" + id + ", number=" + this.number + ", flowCell="
        + this.flowCell + ", instrument=" + this.instrument + ", date="
        + this.date + ", reads=" + this.reads + ", flowCellLaneCount="
        + flowCellLaneCount + ", flowCellSurfaceCount=" + flowCellSurfaceCount
        + ", flowCellSwathCount=" + flowCellSwathCount + ", flowCellTileCount="
        + flowCellTileCount + ", alignToPhix=" + this.alignToPhix + "}";

  }

  public static void main(String[] args) throws ParserConfigurationException,
      SAXException, IOException {

    File f =
        new File(
            "/import/disir01/hiseq_data/work/110628_SNL110_0025_AB0866ABXX/RunInfo.xml");
    RunInfo ri = new RunInfo();
    ri.parse(f);

    System.out.println(ri);

  }

}
