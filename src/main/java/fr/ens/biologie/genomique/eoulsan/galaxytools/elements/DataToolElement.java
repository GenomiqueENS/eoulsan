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
package fr.ens.biologie.genomique.eoulsan.galaxytools.elements;

import java.util.List;

import com.google.common.base.Joiner;
import org.w3c.dom.Element;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.util.GuavaCompatibility;

/**
 * The Class ToolOutputsData.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class DataToolElement extends AbstractToolElement {

  /** The Constant TAG_NAME. */
  public static final String TAG_NAME = "data";

  /** The formats. */
  private final List<String> formats;

  /** The data format. */
  private final DataFormat dataFormat;

  /** The value. */
  private String value = "";

  @Override
  public void setDefaultValue() {
  }

  @Override
  boolean isValueParameterValid() {
    return true;
  }

  @Override
  public String getValue() {
    return this.value;
  }

  @Override
  public void setValue(final Parameter stepParameter) throws EoulsanException {
    super.setValue(stepParameter);

    this.setValue(stepParameter.getValue());
  }

  private void setValue(final String value) {
    this.value = value;
  }

  @Override
  public boolean isFile() {
    return this.dataFormat != null;
  }

  @Override
  public DataFormat getDataFormat() {
    if (this.isFile()) {
      return this.dataFormat;
    }

    throw new UnsupportedOperationException();
  }

  //
  // Constructors
  //

  /**
   * Instantiates a new tool outputs data.
   * @param param the param
   * @throws EoulsanException the eoulsan exception
   */
  public DataToolElement(final Element param) throws EoulsanException {
    this(param, null);
  }

  /**
   * Instantiates a new tool outputs data.
   * @param param the param
   * @param nameSpace the name space
   * @throws EoulsanException the eoulsan exception
   */
  public DataToolElement(final Element param, final String nameSpace)
      throws EoulsanException {
    super(param, nameSpace);

    this.formats =
        GuavaCompatibility.splitToList(COMMA, param.getAttribute("format"));

    // Check count format found
    if (this.formats.size() > 1) {
      throw new EoulsanException("Parsing tool xml: more one format data found,"
          + Joiner.on(",").join(this.formats) + " invalid.");
    }

    if (this.formats.isEmpty()) {
      this.dataFormat = null;
    } else {
      // Convert format in DataFormat
      this.dataFormat = DataFormatRegistry.getInstance()
          .getDataFormatFromToolshedExtensionOrNameOrAlias(this.formats.get(0));
    }
  }

}
