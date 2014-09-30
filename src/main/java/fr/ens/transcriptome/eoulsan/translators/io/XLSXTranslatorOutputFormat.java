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

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * This class define a TranslatorOutputFormat that generate a Microsoft Excel
 * XLSX file.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class XLSXTranslatorOutputFormat implements TranslatorOutputFormat {

  private final OutputStream os;
  private final Workbook wb = new XSSFWorkbook();
  private final Sheet sheet = wb.createSheet("new sheet");
  private final CellStyle style;
  private int rowCount;
  private int colCount;
  private Row row = sheet.createRow(this.rowCount++);

  @Override
  public void addHeaderField(final String fieldName) throws IOException {

    final Cell cell = row.createCell(this.colCount++);
    cell.setCellValue(new XSSFRichTextString(fieldName));
    cell.setCellStyle(style);
  }

  @Override
  public void newLine() throws IOException {

    this.colCount = 0;
    this.row = sheet.createRow(this.rowCount++);
  }

  @Override
  public void writeEmpty() throws IOException {

    row.createCell(this.colCount++);
  }

  @Override
  public void writeLong(long l) throws IOException {

    final Cell cell = row.createCell(this.colCount++);
    cell.setCellValue(l);
  }

  @Override
  public void writeDouble(double d) throws IOException {

    final Cell cell = row.createCell(this.colCount++);
    cell.setCellValue(d);
  }

  @Override
  public void writeText(String text) throws IOException {

    final Cell cell = row.createCell(this.colCount++);
    if (text != null)
      cell.setCellValue(new XSSFRichTextString(text));
  }

  @Override
  public void writeLink(String text, String link) throws IOException {

    final Cell cell = row.createCell(this.colCount++);

    if (text != null) {

      if (link != null)
        cell.setCellFormula("HYPERLINK(\"" + link + "\",\"" + text + "\")");
      else
        cell.setCellValue(new XSSFRichTextString(text));

    }

  }

  @Override
  public void close() throws IOException {

    this.wb.write(this.os);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param os output stream
   */
  public XLSXTranslatorOutputFormat(final OutputStream os) {

    if (os == null)
      throw new NullPointerException("The output stream is null");

    this.os = os;

    // Create a new font and alter it.
    Font font = wb.createFont();
    font.setItalic(true);

    // Fonts are set into a style so create a new one to use.
    this.style = wb.createCellStyle();
    this.style.setFillForegroundColor(HSSFColor.ORANGE.index);
    this.style.setFillPattern(CellStyle.SOLID_FOREGROUND);
    this.style.setFont(font);
  }

  /**
   * Public constructor.
   * @param file output file
   */
  public XLSXTranslatorOutputFormat(final File file) throws IOException {

    this(new FileOutputStream(file));
  }

}
