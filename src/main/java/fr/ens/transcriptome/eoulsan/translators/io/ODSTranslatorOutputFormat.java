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

package fr.ens.transcriptome.eoulsan.translators.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import org.odftoolkit.odfdom.dom.OdfDocumentNamespace;
import org.odftoolkit.odfdom.dom.attribute.office.OfficeValueTypeAttribute;
import org.odftoolkit.odfdom.dom.element.table.TableTableCellElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableRowElement;
import org.odftoolkit.odfdom.pkg.OdfFileDom;
import org.odftoolkit.odfdom.pkg.OdfName;
import org.odftoolkit.odfdom.pkg.OdfXMLFactory;
import org.odftoolkit.odfdom.type.Color;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.style.Font;
import org.odftoolkit.simple.style.StyleTypeDefinitions.FontStyle;
import org.odftoolkit.simple.table.Cell;
import org.odftoolkit.simple.table.Row;
import org.odftoolkit.simple.table.Table;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class define a TranslatorOutputFormat that generate an OpenDocument
 * spreadsheet file.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class ODSTranslatorOutputFormat implements TranslatorOutputFormat {

  private final OutputStream os;
  private final SpreadsheetDocument document;
  private final TableTableElement tableElement;
  private final Table table;
  private final List<String> headers = new ArrayList<String>();
  private TableTableRowElement rowElement;
  private final OdfFileDom dom;

  private boolean first = true;

  private final static class DomNodeList extends AbstractList<Node> {

    private NodeList m_nodeList;

    /** Creates a new instance of NodeList */
    public DomNodeList(NodeList list) {
      m_nodeList = list;
    }

    @Override
    public int size() {
      return m_nodeList.getLength();
    }

    @Override
    public Node get(int index) {
      return m_nodeList.item(index);
    }
  }

  @Override
  public void addHeaderField(String fieldName) throws IOException {

    this.headers.add(fieldName);
  }

  @Override
  public void newLine() throws IOException {

    if (this.first) {

      this.table.appendColumns(headers.size());
      final Row row = table.getRowByIndex(0);
      int i = 0;
      for (String h : headers) {
        final Cell c = row.getCellByIndex(i++);
        c.setCellBackgroundColor(Color.ORANGE);
        Font f = c.getFont();
        f.setFontStyle(FontStyle.ITALIC);
        f.setSize(10);
        c.setFont(f);
        c.setStringValue(h);
      }

      DomNodeList l = new DomNodeList(table.getOdfElement().getChildNodes());

      for (Node n : l)
        if (n instanceof TableTableRowElement)
          this.rowElement = (TableTableRowElement) n;

      this.first = false;
    }

    final TableTableRowElement aRow =
        (TableTableRowElement) OdfXMLFactory.newOdfElement(dom,
            OdfName.newName(OdfDocumentNamespace.TABLE, "table-row"));

    tableElement.appendChild(aRow);
    this.rowElement = aRow;

  }

  private final TableTableCellElement getCell() {

    final TableTableCellElement aCell =
        (TableTableCellElement) OdfXMLFactory.newOdfElement(dom,
            OdfName.newName(OdfDocumentNamespace.TABLE, "table-cell"));

    this.rowElement.appendChild(aCell);

    return aCell;
  }

  @Override
  public void writeEmpty() throws IOException {

    final TableTableCellElement cell = getCell();
    cell.setOfficeValueTypeAttribute(OfficeValueTypeAttribute.Value.VOID
        .toString());
  }

  @Override
  public void writeLong(long l) throws IOException {

    final TableTableCellElement cell = getCell();
    cell.setOfficeValueTypeAttribute(OfficeValueTypeAttribute.Value.FLOAT
        .toString());
    cell.setOfficeValueAttribute(Double.valueOf(l));
  }

  @Override
  public void writeDouble(double d) throws IOException {

    final TableTableCellElement cell = getCell();
    cell.setOfficeValueTypeAttribute(OfficeValueTypeAttribute.Value.FLOAT
        .toString());
    cell.setOfficeValueAttribute(Double.valueOf(d));
  }

  @Override
  public void writeText(String text) throws IOException {

    final TableTableCellElement cell = getCell();
    cell.setOfficeValueTypeAttribute(OfficeValueTypeAttribute.Value.STRING
        .toString());
    cell.setOfficeStringValueAttribute(text == null ? "" : text);

  }

  @Override
  public void writeLink(String text, String link) throws IOException {

    if (text == null || link == null)
      writeText(text);

    final TableTableCellElement cell = getCell();
    cell.setTableFormulaAttribute("=HYPERLINK(\""
        + link + "\";\"" + text + "\")");
  }

  @Override
  public void close() throws IOException {

    try {
      document.save(this.os);
    } catch (Exception e) {
      throw new IOException(e.getMessage());
    }

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param os output stream
   * @throws IOException if an error occurs while creating the output
   */
  public ODSTranslatorOutputFormat(final OutputStream os) throws IOException {

    if (os == null)
      throw new NullPointerException("The output stream is null");

    try {
      this.os = os;
      this.document = SpreadsheetDocument.newSpreadsheetDocument();

      this.table = document.getSheetByIndex(0);
      this.tableElement = table.getOdfElement();
      this.dom = (OdfFileDom) table.getOdfElement().getOwnerDocument();
      Table.newTable(document);
    } catch (Exception e) {
      throw new IOException(e.getMessage());
    }
  }

  /**
   * Public constructor.
   * @param file output file
   * @throws IOException if an error occurs while creating the output
   */
  public ODSTranslatorOutputFormat(final File file) throws IOException {

    this(new FileOutputStream(file));
  }

}
