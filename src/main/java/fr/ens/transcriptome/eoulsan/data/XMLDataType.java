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
package fr.ens.transcriptome.eoulsan.data;

import static fr.ens.transcriptome.eoulsan.util.Utils.equal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.base.Objects;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * This class allow to create a DataType from an XML file
 * @since 1.2
 * @author Laurent Jourdren
 */
public final class XMLDataType extends AbstractDataType {

  private String name;
  private String description;
  private String prefix;
  private boolean oneFilePerAnalysis;
  private boolean dataTypeFromDesignFile;
  private String designFieldName;

  @Override
  public String getName() {

    return this.name;
  }

  @Override
  public String getDescription() {

    return this.name == null ? this.name : this.description;
  }

  @Override
  public String getPrefix() {

    return this.prefix;
  }

  @Override
  public boolean isOneFilePerAnalysis() {

    return this.oneFilePerAnalysis;
  }

  @Override
  public boolean isDataTypeFromDesignFile() {

    return this.dataTypeFromDesignFile;
  }

  @Override
  public String getDesignFieldName() {

    return this.designFieldName;
  }

  //
  // Parsing methods
  //

  private void parse(final InputStream is) throws ParserConfigurationException,
      SAXException, IOException, EoulsanException {

    final Document doc;

    final DocumentBuilderFactory dbFactory =
        DocumentBuilderFactory.newInstance();
    final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    doc = dBuilder.parse(is);
    doc.getDocumentElement().normalize();

    parse(doc);

    is.close();
  }

  private void parse(final Document document) throws EoulsanException {

    for (Element e : XMLUtils.getElementsByTagName(document, "datatype")) {

      this.name = XMLUtils.getTagValue(e, "name");
      this.description = XMLUtils.getTagValue(e, "description");
      this.prefix = XMLUtils.getTagValue(e, "prefix");
      this.oneFilePerAnalysis =
          Boolean.parseBoolean(XMLUtils.getTagValue(e, "onefileperanalysis"));
      this.designFieldName = XMLUtils.getTagValue(e, "designfieldname");

      if (this.designFieldName != null)
        this.dataTypeFromDesignFile = true;

    }

    if (this.name == null)
      throw new EoulsanException("The name of the datatype is null");

    this.name = this.name.trim().toLowerCase();

    if (this.description != null)
      this.description.trim();

    if (this.prefix == null)
      throw new EoulsanException("The prefix of the datatype is null");

    // TODO prefix must ends with a '_'
    // TODO prefix may only contains letters and digit and '_'
    this.prefix = this.prefix.trim().toLowerCase();

    if (this.designFieldName != null)
      this.designFieldName.trim();
  }

  //
  // Object methods
  //

  @Override
  public boolean equals(final Object o) {

    if (o == this)
      return true;

    if (!(o instanceof DataType))
      return false;

    if (!(o instanceof XMLDataType))
      return super.equals(o);

    final XMLDataType that = (XMLDataType) o;

    return equal(this.name, that.name)
        && equal(this.description, that.description)
        && equal(this.prefix, that.prefix)
        && equal(this.oneFilePerAnalysis, that.oneFilePerAnalysis)
        && equal(this.dataTypeFromDesignFile, that.dataTypeFromDesignFile)
        && equal(this.designFieldName, that.designFieldName);
  }

  @Override
  public int hashCode() {

    return Objects.hashCode(this.name, this.description, this.prefix,
        this.oneFilePerAnalysis, this.dataTypeFromDesignFile,
        this.designFieldName);
  }

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("name", this.name)
        .add("description", this.description).add("prefix", this.prefix)
        .add("dataTypeFromDesignFile", this.dataTypeFromDesignFile)
        .add("designFieldName", this.designFieldName).toString();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param is input stream that contains the value of the data type
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   * @throws EoulsanException
   */
  public XMLDataType(final InputStream is) throws EoulsanException {

    if (is == null)
      throw new NullPointerException("The input stream is null");

    try {
      parse(is);
    } catch (ParserConfigurationException e) {
      throw new EoulsanException(e.getMessage());
    } catch (FileNotFoundException e) {
      throw new EoulsanException(e.getMessage());
    } catch (SAXException e) {
      throw new EoulsanException(e.getMessage());
    } catch (IOException e) {
      throw new EoulsanException(e.getMessage());
    }
  }

}
