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
import java.util.EnumSet;

/**
 * This class define an input port of a step.
 *
 * @since 2.0
 * @author Laurent Jourdren
 */
public class SimpleInputPort extends AbstractPort implements InputPort {

  private static final long serialVersionUID = 4663590179211976634L;

  private final EnumSet<CompressionType> compressionsAccepted;
  private final boolean requiredInWorkingDirectory;

  @Override
  public EnumSet<CompressionType> getCompressionsAccepted() {

    return this.compressionsAccepted;
  }

  @Override
  public boolean isRequiredInWorkingDirectory() {

    return this.requiredInWorkingDirectory;
  }

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this)
        .add("name", getName())
        .add("format", getFormat().getName())
        .add("list", isList())
        .add("compressionsAccepted", getCompressionsAccepted())
        .add("requiredInWorkingDirectory", isRequiredInWorkingDirectory())
        .toString();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   *
   * @param name name of the port
   * @param format format of the port
   */
  SimpleInputPort(final String name, final DataFormat format) {

    this(name, false, format, null);
  }

  /**
   * Constructor.
   *
   * @param name name of the port
   * @param list true if a list is excepted as port value
   * @param format format of the port
   */
  SimpleInputPort(final String name, final boolean list, final DataFormat format) {

    this(name, list, format, null);
  }

  /**
   * Constructor.
   *
   * @param name name of the port
   * @param format format of the port
   * @param compressionsAccepted compression accepted
   */
  SimpleInputPort(
      final String name,
      final DataFormat format,
      final EnumSet<CompressionType> compressionsAccepted) {

    this(name, format, compressionsAccepted, false);
  }

  /**
   * Constructor.
   *
   * @param name name of the port
   * @param list true if a list is excepted as port value
   * @param format format of the port
   * @param compressionsAccepted compression accepted
   */
  SimpleInputPort(
      final String name,
      final boolean list,
      final DataFormat format,
      final EnumSet<CompressionType> compressionsAccepted) {

    this(name, list, format, compressionsAccepted, false);
  }

  /**
   * Constructor.
   *
   * @param name name of the port
   * @param format format of the port
   * @param requiredInWorkingDirectory if data is required in working directory
   */
  SimpleInputPort(
      final String name, final DataFormat format, final boolean requiredInWorkingDirectory) {

    this(name, false, format, null, requiredInWorkingDirectory);
  }

  /**
   * Constructor.
   *
   * @param name name of the port
   * @param list true if a list is excepted as port value
   * @param format format of the port
   * @param requiredInWorkingDirectory if data is required in working directory
   */
  SimpleInputPort(
      final String name,
      final boolean list,
      final DataFormat format,
      final boolean requiredInWorkingDirectory) {

    this(name, list, format, null, requiredInWorkingDirectory);
  }

  /**
   * Constructor.
   *
   * @param name name of the port
   * @param format format of the port
   * @param compressionsAccepted compression accepted
   * @param requiredInWorkingDirectory if data is required in working directory
   */
  protected SimpleInputPort(
      final String name,
      final DataFormat format,
      final EnumSet<CompressionType> compressionsAccepted,
      final boolean requiredInWorkingDirectory) {

    this(name, false, format, compressionsAccepted, requiredInWorkingDirectory);
  }

  /**
   * Constructor.
   *
   * @param name name of the port
   * @param list true if a list is excepted as port value
   * @param format format of the port
   * @param compressionsAccepted compression accepted
   * @param requiredInWorkingDirectory if data is required in working directory
   */
  protected SimpleInputPort(
      final String name,
      final boolean list,
      final DataFormat format,
      final EnumSet<CompressionType> compressionsAccepted,
      final boolean requiredInWorkingDirectory) {

    // Set the name, the format and if the value is a list
    super(name, list, format);

    // Set the compressions accepted
    if (compressionsAccepted == null) {
      this.compressionsAccepted = EnumSet.allOf(CompressionType.class);
    } else {
      this.compressionsAccepted = compressionsAccepted;
    }

    // Set if input data is required in working directory
    this.requiredInWorkingDirectory = requiredInWorkingDirectory;
  }

  /**
   * Constructor.
   *
   * @param inputPort input port to clone
   */
  SimpleInputPort(final InputPort inputPort) {

    this(
        inputPort.getName(),
        inputPort.getFormat(),
        inputPort.getCompressionsAccepted(),
        inputPort.isRequiredInWorkingDirectory());
  }
}
