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

package fr.ens.transcriptome.eoulsan.illumina;

import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getAttributeNames;
import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getElementsByTagName;
import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getTagValue;

import java.io.File;
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

import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * This class handle RTA run info data.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class RunInfo {

  /**
   * Handle information about a read.
   * @author Laurent Jourdren
   */
  public static class Read {

    private int number;
    private int numberCycles;
    private boolean indexedRead;

    /**
     * @return Returns the number
     */
    public int getNumber() {
      return this.number;
    }

    /**
     * @return Returns the numberCycles
     */
    public int getNumberCycles() {
      return this.numberCycles;
    }

    /**
     * @return Returns the indexedRead
     */
    public boolean isIndexedRead() {
      return this.indexedRead;
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
  private final List<Read> reads = new ArrayList<>();

  // FlowcellLayout

  private int flowCellLaneCount;
  private int flowCellSurfaceCount;
  private int flowCellSwathCount;
  private int flowCellTileCount;

  private final List<Integer> alignToPhix = new ArrayList<>();

  //
  // Getters
  //

  /**
   * @return Returns the id
   */
  public String getId() {
    return this.id;
  }

  /**
   * @return Returns the number
   */
  public int getNumber() {
    return this.number;
  }

  /**
   * @return Returns the flowCell
   */
  public String getFlowCell() {
    return this.flowCell;
  }

  /**
   * @return Returns the instrument
   */
  public String getInstrument() {
    return this.instrument;
  }

  /**
   * @return Returns the date
   */
  public String getDate() {
    return this.date;
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
    return this.flowCellLaneCount;
  }

  /**
   * @return Returns the flowCellSurfaceCount
   */
  public int getFlowCellSurfaceCount() {
    return this.flowCellSurfaceCount;
  }

  /**
   * @return Returns the flowCellSwathCount
   */
  public int getFlowCellSwathCount() {
    return this.flowCellSwathCount;
  }

  /**
   * @return Returns the flowCellTileCount
   */
  public int getFlowCellTileCount() {
    return this.flowCellTileCount;
  }

  /**
   * @return Returns the alignToPhix
   */
  public List<Integer> getAlignToPhix() {
    return Collections.unmodifiableList(this.alignToPhix);
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

    for (Element e : XMLUtils.getElementsByTagName(document, "RunInfo")) {

      for (Element e1 : XMLUtils.getElementsByTagName(e, "Run")) {

        // Parse attribute of the Run tag
        for (String name : XMLUtils.getAttributeNames(e1)) {

          final String value = e1.getAttribute(name);

          switch (name) {
          case "Id":
            this.id = value;
            break;
          case "Number":
            this.number = Integer.parseInt(value);
            break;
          }
        }

        this.flowCell = getTagValue(e1, "Flowcell");
        this.instrument = getTagValue(e1, "Instrument");
        this.date = getTagValue(e1, "Date");

        int readCount = 0;
        // Parse Reads tag
        for (Element e2 : XMLUtils.getElementsByTagName(e1, "Reads")) {
          for (Element e3 : XMLUtils.getElementsByTagName(e2, "Read")) {

            final Read read = new Read();
            readCount++;

            for (String name : XMLUtils.getAttributeNames(e3)) {

              final String value = e3.getAttribute(name);

              switch (name) {
              case "Number":
                read.number = Integer.parseInt(value);
                break;
              case "NumCycles":
                read.numberCycles = Integer.parseInt(value);
                break;
              case "IsIndexedRead":
                read.indexedRead = "Y".equals(value.toUpperCase().trim());
                break;
              }

              if (read.getNumber() == 0) {
                read.number = readCount;
              }
            }

            this.reads.add(read);
          }
        }

        // Parse FlowcellLayout tag
        for (Element e2 : getElementsByTagName(e1, "FlowcellLayout")) {

          for (String name : getAttributeNames(e2)) {

            final int value = Integer.parseInt(e2.getAttribute(name));

            switch (name) {
            case "LaneCount":
              this.flowCellLaneCount = value;
              break;
            case "SurfaceCount":
              this.flowCellSurfaceCount = value;
              break;
            case "SwathCount":
              this.flowCellSwathCount = value;
              break;
            case "TileCount":
              this.flowCellTileCount = value;
              break;
            }
          }

        }

        for (Element e2 : getElementsByTagName(e1, "AlignToPhiX")) {
          for (Element e3 : getElementsByTagName(e2, "Lane")) {
            this.alignToPhix.add(Integer.parseInt(e3.getTextContent()));
          }
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
        + "{id=" + this.id + ", number=" + this.number + ", flowCell="
        + this.flowCell + ", instrument=" + this.instrument + ", date="
        + this.date + ", reads=" + this.reads + ", flowCellLaneCount="
        + this.flowCellLaneCount + ", flowCellSurfaceCount="
        + this.flowCellSurfaceCount + ", flowCellSwathCount="
        + this.flowCellSwathCount + ", flowCellTileCount="
        + this.flowCellTileCount + ", alignToPhix=" + this.alignToPhix + "}";

  }

}
