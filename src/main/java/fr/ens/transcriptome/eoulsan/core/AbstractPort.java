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

package fr.ens.transcriptome.eoulsan.core;

import java.io.Serializable;

import fr.ens.transcriptome.eoulsan.data.DataFormat;

/**
 * Abstract class that define a port.
 * @since 1.3
 * @author Laurent Jourdren
 */
abstract class AbstractPort implements Port, Serializable {

  private static final long serialVersionUID = 1773398938012180465L;

  private final String name;
  private final DataFormat format;

  @Override
  public String getName() {

    return this.name;
  }

  @Override
  public DataFormat getFormat() {

    return this.format;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param name name of the port
   * @param format format of the port
   */
  AbstractPort(final String name, final DataFormat format) {

    if (name == null)
      throw new NullPointerException("The name of the port is null");

    if (format == null)
      throw new NullPointerException("The format of the port "
          + name + " is null");

    this.name = name.trim().toLowerCase();
    this.format = format;
  }

}
