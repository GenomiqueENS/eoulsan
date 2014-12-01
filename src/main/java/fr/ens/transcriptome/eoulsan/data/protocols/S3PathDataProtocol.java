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

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This class define the s3 protocol in Hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class S3PathDataProtocol extends PathDataProtocol {

  /** Protocol name. */
  public static final String PROTOCOL_NAME = "s3";

  @Override
  public String getName() {

    return PROTOCOL_NAME;
  }

  @Override
  public void mkdir(final DataFile dir) throws IOException {

    mkdirs(dir);
  }

  @Override
  public void mkdirs(final DataFile dir) throws IOException {

    final Path path = getPath(dir);

    if (path == null)
      throw new NullPointerException("Path to create is null");
    if (this.conf == null)
      throw new NullPointerException("The configuration object is null");

    final FileSystem fs = path.getFileSystem(this.conf);

    if (fs == null)
      throw new IOException(
          "Unable to create the directory, The FileSystem is null");

    if (!fs.mkdirs(path))
      throw new IOException("Unable to create the directory: " + dir);
  }

  @Override
  public boolean canMkdir() {

    return true;
  }

}
