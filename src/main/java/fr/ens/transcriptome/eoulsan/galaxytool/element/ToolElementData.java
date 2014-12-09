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
package fr.ens.transcriptome.eoulsan.galaxytool.element;

import java.util.List;

import org.w3c.dom.Element;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;

// TODO: Auto-generated Javadoc
/**
 * The Class ToolParameterData.
 * @author Sandrine Perrin
 * @since 2.4
 */
public class ToolElementData extends AbstractToolElement {

  /** The formats. */
  private final List<String> formats;

  /** The data format. */
  private final DataFormat dataFormat;

  /** The value. */
  private String value = "";

  @Override
  boolean isValueParameterValid() {
    return true;
  }

  @Override
  public void setParameterEoulsan(final Parameter stepParameter) {
    this.value = stepParameter.getValue();
  }

  @Override
  public void setParameterEoulsan() {
  }

  @Override
  public String getValue() {
    return this.value;
  }

  @Override
  public boolean isFile() {
    return this.dataFormat != null;
  }

  @Override
  public DataFormat getDataFormat() {
    if (isFile()) {
      return this.dataFormat;
    }

    throw new UnsupportedOperationException();
  }

  //
  // Constructor
  //
  /**
   * Instantiates a new tool parameter data.
   * @param param the param
   * @throws EoulsanException the eoulsan exception
   */
  public ToolElementData(final Element param) throws EoulsanException {
    this(param, null);
  }

  /**
   * Instantiates a new tool parameter data.
   * @param param the param
   * @param nameSpace the name space
   * @throws EoulsanException the eoulsan exception
   */
  public ToolElementData(final Element param, final String nameSpace)
      throws EoulsanException {
    super(param, nameSpace);
    this.isSetting = true;

    this.formats = COMMA.splitToList(param.getAttribute("format"));

    // Check count format found
    // TODO
    // if (this.formats.size() > 1) {
    // throw new EoulsanException(
    // "Parsing tool xml: more one format data found,"
    // + Joiner.on(",").join(formats) + " invalid.");
    // }

    if (this.formats.isEmpty()) {
      this.dataFormat = null;
    } else {
      // Convert format in DataFormat
      this.dataFormat = ConvertorToDataFormat.convert(this.formats.get(0));
    }
  }
}
