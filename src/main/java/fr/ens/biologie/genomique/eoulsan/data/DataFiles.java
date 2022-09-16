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

package fr.ens.biologie.genomique.eoulsan.data;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.data.protocols.DataProtocol;
import fr.ens.biologie.genomique.kenetre.io.FileUtils;

/**
 * This class contains utility methods on DataFile objects.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DataFiles {

  /**
   * Copy a file, if input data is compressed, data will be uncompressed and if
   * output require to be compressed output will be compressed.
   * @param input input file
   * @param output output file.
   * @throws IOException if an error occurs while copying data
   */
  public static void copy(final DataFile input, final DataFile output)
      throws IOException {

    requireNonNull(input, "input file cannot be null");
    requireNonNull(output, "output file cannot be null");

    // Do not (un)compress data if the input and output has the same compression
    // type
    if (input.getCompressionType() == output.getCompressionType()) {
      rawCopy(input, output);
    } else {
      FileUtils.copy(input.open(), output.create());
    }
  }

  /**
   * Copy a file.
   * @param input input file
   * @param output output file.
   * @throws IOException if an error occurs while copying data
   */
  public static void rawCopy(final DataFile input, final DataFile output)
      throws IOException {

    requireNonNull(input, "input file cannot be null");
    requireNonNull(output, "output file cannot be null");

    FileUtils.copy(input.rawOpen(), output.rawCreate());
  }

  /**
   * Create a symbolic link if the input and output use the same protocol and if
   * symbolic links are supported by the protocol. If symbolic link cannot be
   * created, the input file will be copied.
   * @param input input file
   * @param output output file
   * @throws IOException if an error occurs while copying data or creating the
   *           symbolic link
   */
  public static void symlinkOrCopy(final DataFile input, final DataFile output)
      throws IOException {

    symlinkOrCopy(input, output, false);
  }

  /**
   * Create a symbolic link if the input and output use the same protocol and if
   * symbolic links are supported by the protocol. If symbolic link cannot be
   * created, the input file will be copied.
   * @param input input file
   * @param output output file
   * @param relativize relativize the link target path
   * @throws IOException if an error occurs while copying data or creating the
   *           symbolic link
   */
  public static void symlinkOrCopy(final DataFile input, final DataFile output,
      final boolean relativize) throws IOException {

    requireNonNull(input, "input file cannot be null");
    requireNonNull(output, "output file cannot be null");

    // If compression of input and output is not the same, copy data
    if (input.getCompressionType() != output.getCompressionType()) {

      copy(input, output);
    } else {

      // Else test if a symbolic link can be created

      final DataProtocol inProtocol = input.getProtocol();
      final DataProtocol outProtocol = output.getProtocol();

      if (inProtocol.equals(outProtocol) && inProtocol.canSymlink()) {

        input.symlink(output, relativize);
      } else {
        copy(input, output);
      }
    }
  }

}
