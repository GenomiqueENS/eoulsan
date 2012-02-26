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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.checkers.Checker;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.util.Utils;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

public class XMLDataFormat extends AbstractDataFormat {

  private static final String DEFAULT_CONTENT_TYPE = "text/plain";
  private static final int DEFAULT_MAX_FILES_COUNT = 1;

  private String name;
  private String description;
  private String typeName;
  private String contentType = "text/plain";
  private String[] extensions;
  private String generatorClassName;
  private String checkerClassName;
  private int maxFilesCount;

  @Override
  public String getFormatName() {

    return this.name;
  }

  @Override
  public DataType getType() {

    return DataTypeRegistry.getInstance().getDataTypeFromName(this.typeName);
  }

  @Override
  public String getDefaultExtention() {

    return this.extensions[0];
  }

  @Override
  public String[] getExtensions() {

    return this.extensions;
  }

  @Override
  public boolean isGenerator() {

    return this.generatorClassName != null;
  }

  @Override
  public boolean isChecker() {

    return this.checkerClassName != null;
  }

  @Override
  public Step getGenerator() {

    return (Step) loadClass(this.generatorClassName, Step.class);
  }

  @Override
  public Checker getChecker() {

    return (Checker) loadClass(this.checkerClassName, Checker.class);
  }

  @Override
  public String getContentType() {

    return this.contentType;
  }

  @Override
  public int getMaxFilesCount() {
    return this.maxFilesCount;
  }

  //
  // Other methods
  //

  private final Object loadClass(final String classname, Class<?> interf) {

    try {

      final Class<?> result =
          this.getClass().getClassLoader().loadClass(classname);

      if (result == null)
        return null;

      final Object o = result.newInstance();

      if (interf.isInstance(o))
        return result.newInstance();

      return null;
    } catch (ClassNotFoundException e) {
      return null;
    } catch (InstantiationException e) {
      return null;
    } catch (IllegalAccessException e) {
      return null;
    }
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

    for (Element e : XMLUtils.getElementsByTagName(document, "dataformat")) {

      this.name = XMLUtils.getTagValue(e, "name");
      this.description = XMLUtils.getTagValue(e, "description");
      this.typeName = XMLUtils.getTagValue(e, "type");
      this.contentType = XMLUtils.getTagValue(e, "contenttype");
      this.generatorClassName = XMLUtils.getTagValue(e, "generator");
      this.checkerClassName = XMLUtils.getTagValue(e, "checker");

      // Parse max files count
      final String max =
          XMLUtils.getTagValue(e, XMLUtils.getTagValue(e, "maxfilescount"));
      try {
        if (max == null)
          this.maxFilesCount = DEFAULT_MAX_FILES_COUNT;
        else
          this.maxFilesCount = Integer.parseInt(max);
      } catch (NumberFormatException exp) {
        throw new EoulsanException(
            "Invalid maximal files count for data format "
                + this.name + ": " + max);
      }

      // Parse extensions
      List<String> extensions = Utils.newArrayList();
      for (Element e2 : XMLUtils.getElementsByTagName(document, "extensions"))
        for (Element e3 : XMLUtils.getElementsByTagName(e2, "extension")) {

          final String defaultAttribute = e3.getAttribute("default");

          if (defaultAttribute != null
              && "true".equals(defaultAttribute.trim().toLowerCase()))
            extensions.add(0, e3.getTextContent().trim());
          else
            extensions.add(e3.getTextContent().trim());
        }

      this.extensions =
          new LinkedHashSet<String>(extensions).toArray(new String[0]);

    }

    // Check object values
    if (this.name == null)
      throw new EoulsanException("The name of the datatype is null");

    this.name = this.name.trim().toLowerCase();

    if (this.description != null)
      this.description.trim();

    if (this.contentType == null)
      this.contentType = DEFAULT_CONTENT_TYPE;

    if (this.maxFilesCount < 1 || this.maxFilesCount > 2)
      throw new EoulsanException("Invalid maximal files count for data format "
          + this.name + ": " + this.maxFilesCount);
  }

  @Override
  public String toString() {

    return "XMLDataFormat{name="
        + this.name + ", description=" + this.description + ", typeName="
        + typeName + ", contentType=" + this.contentType + ", extensions="
        + Arrays.toString(this.getExtensions()) + ", generatorClassName="
        + this.generatorClassName + ", checkerClassName="
        + this.checkerClassName + ", maxFilesCount=" + this.maxFilesCount + "}";
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param is input stream that contains the value of the data format
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   * @throws EoulsanException
   */
  public XMLDataFormat(final InputStream is) throws EoulsanException {

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
