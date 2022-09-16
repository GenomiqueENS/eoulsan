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

package fr.ens.biologie.genomique.eoulsan.data.protocols;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.biologie.genomique.eoulsan.AbstractEoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.HadoopEoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFileMetadata;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.kenetre.io.CompressionType;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.PathUtils;

/**
 * This class define an abstract class for DataProtocols based on the Hadoop
 * framework Path object.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class PathDataProtocol extends AbstractDataProtocol {

  protected Configuration conf;

  /**
   * Get Convert a DataFile object to a Path object.
   * @param dataFile DataFile to convert
   * @return a Path object
   */
  public Path getPath(final DataFile dataFile) {

    if (dataFile == null) {
      throw new NullPointerException("The source is null.");
    }

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

    if (!exists(src, true)) {
      throw new FileNotFoundException("File not found: " + src);
    }

    final Path path = getPath(src);
    final FileStatus status = path.getFileSystem(this.conf).getFileStatus(path);

    final SimpleDataFileMetadata result = new SimpleDataFileMetadata();
    result.setContentLength(status.getLen());
    result.setLastModified(status.getModificationTime());
    result.setDataFormat(DataFormatRegistry.getInstance()
        .getDataFormatFromFilename(src.getName()));

    final CompressionType ct =
        CompressionType.getCompressionTypeByFilename(src.getSource());

    if (ct != null) {
      result.setContentEncoding(ct.getContentEncoding());
    }

    if (status.isDirectory()) {
      result.setDirectory(true);
    }

    if (status.isSymlink()) {
      result.setSymbolicLink(new DataFile(status.getSymlink().toUri()));
    }

    return result;
  }

  @Override
  public boolean exists(final DataFile src, final boolean followLink) {

    final Path path = getPath(src);

    try {

      final FileSystem fs = path.getFileSystem(conf);

      final FileStatus status = fs.getFileStatus(path);

      if (status == null) {
        return false;
      }

      if (status.isSymlink()) {

        return fs.getFileStatus(fs.getLinkTarget(path)) != null;
      }

      return true;
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public boolean canRead() {

    return true;
  }

  @Override
  public boolean canWrite() {

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

    if (!runtime.getMode().isHadoopProtocolMode()
        || !(runtime instanceof HadoopEoulsanRuntime)) {
      throw new IllegalStateException(
          "Can only create PathDataProtocol in hadoop mode.");
    }

    final HadoopEoulsanRuntime hadoopRuntime = (HadoopEoulsanRuntime) runtime;

    this.conf = hadoopRuntime.getConfiguration();

    if (this.conf == null) {
      throw new NullPointerException("The Hadoop configuration object is null");
    }
  }

}
