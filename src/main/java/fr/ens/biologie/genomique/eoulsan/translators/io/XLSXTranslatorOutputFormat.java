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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.translators.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.util.DefaultTempFileCreationStrategy;
import org.apache.poi.util.TempFile;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

/**
 * This class define a TranslatorOutputFormat that generate a Microsoft Excel
 * XLSX file.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class XLSXTranslatorOutputFormat implements TranslatorOutputFormat {

  private static final int MAX_LINES_IN_MEMORY = 10;

  private final OutputStream os;
  private final SXSSFWorkbook wb;
  private final Sheet sheet;
  private final CellStyle defaultStyle;
  private final CellStyle headerStyle;
  private final CellStyle linkStyle;
  private int rowCount;
  private int colCount;
  private Row row;

  @Override
  public void addHeaderField(final String fieldName) throws IOException {

    final Cell cell = this.row.createCell(this.colCount++);
    cell.setCellValue(new XSSFRichTextString(fieldName));
    cell.setCellStyle(this.headerStyle);
  }

  @Override
  public void newLine() throws IOException {

    this.colCount = 0;
    this.row = this.sheet.createRow(this.rowCount++);
  }

  @Override
  public void writeEmpty() throws IOException {

    this.row.createCell(this.colCount++);
  }

  @Override
  public void writeLong(final long l) throws IOException {

    final Cell cell = this.row.createCell(this.colCount++);
    cell.setCellValue(l);
    cell.setCellStyle(this.defaultStyle);
  }

  @Override
  public void writeDouble(final double d) throws IOException {

    final Cell cell = this.row.createCell(this.colCount++);
    cell.setCellValue(d);
    cell.setCellStyle(this.defaultStyle);
  }

  @Override
  public void writeText(final String text) throws IOException {

    final Cell cell = this.row.createCell(this.colCount++);
    if (text != null) {
      cell.setCellValue(new XSSFRichTextString(text));
      cell.setCellStyle(this.defaultStyle);
    }
  }

  @Override
  public void writeLink(final String text, final String link)
      throws IOException {

    final SXSSFCell cell =
        (SXSSFCell) this.row.createCell(this.colCount++, CellType.FORMULA);
    // final Cell cell = this.row.createCell(this.colCount++);

    if (text != null) {

      if (link != null) {
        cell.setCellFormula("HYPERLINK(\""
            + link.replace("\"", "\"\"") + "\",\"" + text.replace("\"", "\"\"")
            + "\")");
        cell.setCellStyle(this.linkStyle);
      }

      cell.setCellValue(text);
    }
  }

  @Override
  public void close() throws IOException {

    this.wb.write(this.os);
    this.os.close();

    // Dispose of temporary files backing the workbook on disk
    this.wb.dispose();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param os output stream
   * @param temporaryDirectory the temporary directory to use
   */
  public XLSXTranslatorOutputFormat(final OutputStream os,
      final File temporaryDirectory) {

    if (os == null) {
      throw new NullPointerException("The output stream is null");
    }

    this.os = os;

    // Set the temporary directory to use
    if (temporaryDirectory != null) {
      TempFile.setTempFileCreationStrategy(
          new DefaultTempFileCreationStrategy(temporaryDirectory));
    }

    // Initialize the workbench
    this.wb = new SXSSFWorkbook(MAX_LINES_IN_MEMORY);
    this.sheet = this.wb.createSheet("new sheet");
    this.row = this.sheet.createRow(this.rowCount++);

    // Temporary files will be compressed
    this.wb.setCompressTempFiles(true);

    // Define default style
    Font defaultFont = this.wb.createFont();
    defaultFont.setFontName("Arial");
    defaultFont.setFontHeightInPoints((short) 10);
    this.defaultStyle = this.wb.createCellStyle();
    this.defaultStyle.setFont(defaultFont);

    // Define header style
    Font headerFont = this.wb.createFont();
    headerFont.setFontName(defaultFont.getFontName());
    headerFont.setFontHeightInPoints(defaultFont.getFontHeightInPoints());
    headerFont.setItalic(true);
    this.headerStyle = this.wb.createCellStyle();
    this.headerStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
    this.headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    this.headerStyle.setFont(headerFont);

    // Define link style
    Font linkfont = this.wb.createFont();
    linkfont.setFontName(defaultFont.getFontName());
    linkfont.setFontHeightInPoints(defaultFont.getFontHeightInPoints());
    linkfont.setUnderline(XSSFFont.U_SINGLE);
    linkfont.setColor(IndexedColors.BLUE.getIndex());
    this.linkStyle = this.wb.createCellStyle();
    this.linkStyle.setFont(linkfont);
  }

  /**
   * Public constructor.
   * @param os output stream
   */
  public XLSXTranslatorOutputFormat(final OutputStream os) {

    this(os, null);
  }

  /**
   * Public constructor.
   * @param file output file
   */
  public XLSXTranslatorOutputFormat(final File file) throws IOException {

    this(new FileOutputStream(file), null);
  }

  /**
   * Public constructor.
   * @param file output file
   * @param temporaryDirectory the temporary directory to use
   */
  public XLSXTranslatorOutputFormat(final File file,
      final File temporaryDirectory) throws IOException {

    this(new FileOutputStream(file), temporaryDirectory);
  }

}
