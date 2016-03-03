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

import org.w3c.dom.Element;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;

// TODO: Auto-generated Javadoc
/**
 * The Class ToolParameterData.
 * @author Sandrine Perrin
 * @since 2.1
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
  public void setValue(final Parameter stepParameter) throws EoulsanException {
    super.setValue(stepParameter);

    if (stepParameter != null)
      this.setValue(stepParameter.getValue());

  }

  @Override
  public void setValue(final String value) throws EoulsanException {

    this.value = value;
  }

  @Override
  public void setValue() {
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
    if (this.isFile()) {
      return this.dataFormat;
    }

    throw new UnsupportedOperationException();
  }

  //
  // Constructors
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
    if (this.formats.isEmpty()) {
      this.dataFormat = null;
    } else {
      // Convert format in DataFormat

      this.dataFormat = DataFormatRegistry.getInstance()
          .getDataFormatFromToolshedExtension(this.formats.get(0));
    }
  }

}
