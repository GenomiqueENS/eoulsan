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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.AbstractEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.HadoopEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFileMetadata;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

public abstract class PathDataProtocol extends AbstractDataProtocol {

  protected Configuration conf;

  /**
   * Get Convert a DataFile object to a Path object
   * @param dataFile DataFile to convert
   * @return a Path object
   */
  public Path getPath(final DataFile dataFile) {

    if (dataFile == null)
      throw new NullPointerException("The source is null.");

    return new Path(dataFile.getSource());
  }

  @Override
  public InputStream getData(final DataFile src) throws IOException {

    return PathUtils.createInputStream(getPath(src), this.conf);
  }

  @Override
  public OutputStream putData(final DataFile src) throws IOException {

    return PathUtils.createOutputStream(getPath(src), this.conf);
  }

  @Override
  public DataFileMetadata getMetadata(final DataFile src) throws IOException {

    if (!exists(src))
      throw new FileNotFoundException("File not found: " + src);

    final Path path = getPath(src);
    final FileStatus status = path.getFileSystem(this.conf).getFileStatus(path);

    final SimpleDataFileMetadata result = new SimpleDataFileMetadata();
    result.setContentLength(status.getLen());
    result.setLastModified(status.getModificationTime());
    result.setDataFormat(DataFormatRegistry.getInstance()
        .getDataFormatFromFilename(src.getName()));

    final CompressionType ct =
        CompressionType.getCompressionTypeByFilename(src.getSource());

    if (ct != null)
      result.setContentEncoding(ct.getContentEncoding());

    if (status.isDir())
      result.setDirectory(true);

    return result;
  }

  @Override
  public boolean exists(final DataFile src) {

    final Path path = getPath(src);

    try {
      return PathUtils.exists(path, this.conf);
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public boolean isReadable() {

    return true;
  }

  @Override
  public boolean isWritable() {

    return true;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   */
  public PathDataProtocol() {

    final AbstractEoulsanRuntime runtime = EoulsanRuntime.getRuntime();

    if (!runtime.isHadoopMode() || !(runtime instanceof HadoopEoulsanRuntime))
      throw new IllegalStateException(
          "Can only create PathDataProtocol in hadoop mode.");

    final HadoopEoulsanRuntime hadoopRuntime = (HadoopEoulsanRuntime) runtime;

    this.conf = hadoopRuntime.getConfiguration();

    if (conf == null)
      throw new NullPointerException("The Hadoop configuration object is null");
  }

}
