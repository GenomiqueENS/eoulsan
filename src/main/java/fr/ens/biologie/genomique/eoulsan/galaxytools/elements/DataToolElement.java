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

import static fr.ens.biologie.genomique.eoulsan.galaxytools.GalaxyToolXMLParserUtils.newEoulsanException;

import java.util.List;

import org.w3c.dom.Element;

import com.google.common.base.Joiner;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.galaxytools.ToolInfo;
import fr.ens.biologie.genomique.eoulsan.util.GuavaCompatibility;

/**
 * The Class ToolOutputsData.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class DataToolElement extends AbstractToolElement {

  /** The Constant TAG_NAME. */
  public static final String TAG_NAME = "data";

  /** The Constant TYPE. */
  public final static String TYPE = "data";

  /** The formats. */
  private final List<String> formats;

  /** The data format. */
  private final DataFormat dataFormat;

  /** The value. */
  private String value = "";

  //
  // Getters
  //

  @Override
  public String getValue() {
    return this.value;
  }

  /**
   * Get the data format of the tool element.
   * @return the data format
   */
  public DataFormat getDataFormat() {
    return this.dataFormat;
  }

  //
  // Setters
  //

  @Override
  public void setValue(final String value) {
    this.value = value;
  }

  //
  // Constructors
  //

  /**
   * Instantiates a new tool outputs data.
   * @param toolInfo the ToolInfo object
   * @param param the param
   * @throws EoulsanException if an error occurs while setting the value
   */
  public DataToolElement(final ToolInfo toolInfo, final Element param,
      final String nameSpace) throws EoulsanException {
    super(param, nameSpace);

    this.formats =
        GuavaCompatibility.splitToList(COMMA, param.getAttribute("format"));

    // Check count format found
    if (this.formats.size() > 1) {
      throw newEoulsanException(toolInfo, getName(),
          "more one format data found ("
              + Joiner.on(",").join(this.formats) + ")");
    }

    if (this.formats.isEmpty()) {
      this.dataFormat = null;
      throw newEoulsanException(toolInfo, getName(), "no format found");
    }

    // Get the format
    String format = this.formats.get(0);

    // Convert format in DataFormat
    this.dataFormat = DataFormatRegistry.getInstance()
        .getDataFormatFromGalaxyFormatNameOrNameOrAlias(format);

    // Check if a valid format has been found
    if (this.dataFormat == null) {
      throw newEoulsanException(toolInfo, getName(),
          "unknown format: " + format);
    }

  }

}
