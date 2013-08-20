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

import static com.google.common.base.Objects.equal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.checkers.Checker;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * This class define a DataFormat from an XML file.
 * @since 1.2
 * @author Laurent Jourdren
 */
public final class XMLDataFormat extends AbstractDataFormat implements
    Serializable {

  private static final long serialVersionUID = -6926659317643003910L;

  private static final String DEFAULT_CONTENT_TYPE = "text/plain";
  private static final int DEFAULT_MAX_FILES_COUNT = 1;

  private String name;
  private String description;
  private String prefix;
  private boolean oneFilePerAnalysis;
  private boolean dataTypeFromDesignFile;
  private String designFieldName;
  private String contentType = "text/plain";
  private final List<String> extensions = Lists.newArrayList();
  private String generatorClassName;
  private Set<Parameter> generatorParameters = Sets.newLinkedHashSet();
  private String checkerClassName;
  private int maxFilesCount;

  @Override
  public String getName() {

    return this.name;
  }

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

  @Override
  public String getDefaultExtention() {

    return this.extensions.get(0);
  }

  @Override
  public List<String> getExtensions() {

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

    final Step generator =
        (Step) loadClass(this.generatorClassName, Step.class);

    if (generator == null)
      return null;

    try {
      generator.configure(this.generatorParameters);

      return generator;
    } catch (EoulsanException e) {

      final Logger logger = EoulsanLogger.getLogger();
      logger.severe("Cannot create generator: " + e.getMessage());
      return null;
    }
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

    if (classname == null)
      return null;

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
      this.prefix = XMLUtils.getTagValue(e, "prefix");
      this.oneFilePerAnalysis =
          Boolean.parseBoolean(XMLUtils.getTagValue(e, "onefileperanalysis"));
      this.designFieldName = XMLUtils.getTagValue(e, "designfieldname");
      this.contentType = XMLUtils.getTagValue(e, "content-type");
      this.generatorClassName = XMLUtils.getTagValue(e, "generator");
      this.checkerClassName = XMLUtils.getTagValue(e, "checker");

      if (this.designFieldName != null)
        this.dataTypeFromDesignFile = true;

      // Get the parameters of the generator step
      for (Element generatorElement : XMLUtils.getElementsByTagName(e,
          "generator")) {
        final List<String> attributeNames =
            XMLUtils.getAttributeNames(generatorElement);

        for (String attributeName : attributeNames)
          this.generatorParameters.add(new Parameter(attributeName,
              generatorElement.getAttribute(attributeName)));
      }

      // Parse max files count
      final String maxFiles = XMLUtils.getTagValue(e, "maxfilescount");

      try {
        if (maxFiles == null)
          this.maxFilesCount = DEFAULT_MAX_FILES_COUNT;
        else
          this.maxFilesCount = Integer.parseInt(maxFiles);
      } catch (NumberFormatException exp) {
        throw new EoulsanException(
            "Invalid maximal files count for data format "
                + this.name + ": " + maxFiles);
      }

      // Parse extensions
      for (Element e2 : XMLUtils.getElementsByTagName(document, "extensions"))
        for (Element e3 : XMLUtils.getElementsByTagName(e2, "extension")) {

          final String defaultAttribute = e3.getAttribute("default");

          if (defaultAttribute != null
              && "true".equals(defaultAttribute.trim().toLowerCase()))
            this.extensions.add(0, e3.getTextContent().trim());
          else
            this.extensions.add(e3.getTextContent().trim());
        }

    }

    // Check object values
    if (this.name == null)
      throw new EoulsanException("The name of the datatype is null");

    this.name = this.name.trim().toLowerCase();

    if (this.description != null)
      this.description = this.description.trim();

    if (this.contentType == null || "".equals(this.contentType.trim()))
      this.contentType = DEFAULT_CONTENT_TYPE;

    if (this.generatorClassName != null
        && "".equals(this.generatorClassName.trim()))
      this.generatorClassName = null;

    if (this.checkerClassName != null
        && "".equals(this.checkerClassName.trim()))
      this.checkerClassName = null;

    if (this.maxFilesCount < 1 || this.maxFilesCount > 2)
      throw new EoulsanException("Invalid maximal files count for data format "
          + this.name + ": " + this.maxFilesCount);

    if (this.extensions.size() == 0)
      throw new EoulsanException("No extension define for the data format "
          + this.name);
  }

  //
  // Object methods
  //

  @Override
  public boolean equals(final Object o) {

    if (o == this)
      return true;

    if (!(o instanceof DataFormat)) {
      return false;
    }

    if (!(o instanceof XMLDataFormat))
      return super.equals(o);

    final XMLDataFormat that = (XMLDataFormat) o;

    return equal(this.name, that.name)
        && equal(this.description, that.description)
        && equal(this.prefix, that.prefix)
        && equal(this.oneFilePerAnalysis, that.oneFilePerAnalysis)
        && equal(this.dataTypeFromDesignFile, that.dataTypeFromDesignFile)
        && equal(this.designFieldName, that.designFieldName)
        && equal(this.contentType, that.contentType)
        && equal(this.extensions, that.extensions)
        && equal(this.generatorClassName, that.generatorClassName)
        && equal(this.checkerClassName, that.checkerClassName)
        && equal(this.maxFilesCount, that.maxFilesCount);
  }

  @Override
  public int hashCode() {

    return Objects.hashCode(this.name, this.description, this.prefix,
        this.oneFilePerAnalysis, this.dataTypeFromDesignFile,
        this.designFieldName, this.contentType, this.extensions,
        this.generatorClassName, this.checkerClassName, this.maxFilesCount);
  }

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("name", this.name)
        .add("description", this.description).add("prefix", prefix)
        .add("contentType", this.contentType)
        .add("defaultExtension", this.extensions.get(0))
        .add("extensions", this.extensions)
        .add("generatorClassName", this.generatorClassName)
        .add("generatorParameters", this.generatorParameters)
        .add("checkerClassName", this.checkerClassName)
        .add("maxFilesCount", this.maxFilesCount).toString();
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
