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

package fr.ens.biologie.genomique.eoulsan.core;

import com.google.common.base.MoreObjects;

import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.kenetre.io.CompressionType;

/**
 * This class define an output port of a step.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class SimpleOutputPort extends AbstractPort
    implements OutputPort {

  private static final long serialVersionUID = 3565485272173523695L;

  private final CompressionType compression;

  @Override
  public CompressionType getCompression() {

    return this.compression;
  }

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this).add("name", getName())
        .add("format", getFormat().getName()).add("list", isList())
        .add("compression", getCompression()).toString();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param name name of the port
   * @param format format of the port
   */
  SimpleOutputPort(final String name, final DataFormat format) {

    this(name, false, format, null);
  }

  /**
   * Constructor.
   * @param name name of the port
   * @param list true if a list is excepted as port value
   * @param format format of the port
   */
  SimpleOutputPort(final String name, final boolean list,
      final DataFormat format) {

    this(name, list, format, null);
  }

  /**
   * Constructor.
   * @param name name of the port
   * @param format format of the port
   * @param compression compression of the output
   */
  protected SimpleOutputPort(final String name, final DataFormat format,
      final CompressionType compression) {

    this(name, false, format, compression);
  }

  /**
   * Constructor.
   * @param name name of the port
   * @param format format of the port
   * @param list true if a list is excepted as port value
   * @param compression compression of the output
   */
  protected SimpleOutputPort(final String name, final boolean list,
      final DataFormat format, final CompressionType compression) {

    // Set the name and the format
    super(name, list, format);

    // Set the compression
    if (compression == null) {
      this.compression = CompressionType.NONE;
    } else {
      this.compression = compression;
    }
  }

  /**
   * Constructor.
   * @param outputPort output port to clone
   */
  SimpleOutputPort(final OutputPort outputPort) {

    this(outputPort.getName(), outputPort.isList(), outputPort.getFormat(),
        outputPort.getCompression());
  }

}
