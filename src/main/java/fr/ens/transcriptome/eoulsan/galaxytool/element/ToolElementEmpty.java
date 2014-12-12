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

import java.util.Map;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;

// TODO: Auto-generated Javadoc
/**
 * The Class ToolParameterEmpty.
 * @author Sandrine Perrin
 * @since 2.4
 */
public class ToolElementEmpty implements ToolElement {

  /**
   * Instantiates a new tool parameter empty.
   */
  public ToolElementEmpty() {
  }

  @Override
  public String getName() {
    return "No name";
  }

  @Override
  public boolean isSetting() {
    return false;
  }

  @Override
  public String getValue() {
    return "No Value";
  }

  @Override
  public void setValue() {
  }

  @Override
  public void setValue(final Parameter stepParameter) {
  }

  @Override
  public void setValues(final Map<String, Parameter> stepParameters) {

  }

  @Override
  public void setValue(final String value) throws EoulsanException {

  }

  @Override
  public boolean isFile() {
    return false;
  }

  @Override
  public DataFormat getDataFormat() {
    throw new UnsupportedOperationException();
  }

}
