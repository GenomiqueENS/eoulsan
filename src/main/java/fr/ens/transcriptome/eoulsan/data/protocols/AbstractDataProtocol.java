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

package fr.ens.transcriptome.eoulsan.data.protocols;

import java.io.IOException;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFileMetadata;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class is an abstract Protocol class that implements generic
 * getIn/OutputStream with length.
 * @author Laurent Jourdren
 */
abstract class AbstractDataProtocol implements DataProtocol {

  @Override
  public OutputStream putData(final DataFile src, final DataFileMetadata md)
      throws IOException {

    if (!isWritable())
      throw new IOException("Writing is not allowed for the source: " + src);

    return putData(src);
  }

  @Override
  public void putData(DataFile src, DataFile dest) throws IOException {

    if (src == null)
      throw new NullPointerException("The source of the data to put is null");

    if (dest == null)
      throw new NullPointerException(
          "The destination of the data to put is null");

    final DataFileMetadata mdSrc = src.getMetaData();

    FileUtils.copy(src.getProtocol().getData(src), dest.getProtocol().putData(
        dest, mdSrc));
  }

}
