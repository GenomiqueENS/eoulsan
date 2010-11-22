/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.datasources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define a S3 Datasource.
 * @author Laurent Jourdren
 */
public final class S3DataSource implements DataSource {

  private String pathname;

  /**
   * Configure the source with properties
   * @param properties Properties to config the source
   */
  @Override
  public void configSource(final Properties properties) {

    if (properties == null)
      return;

    String val = properties.getProperty("file");
    if (val == null)
      this.pathname = "";
    else
      this.pathname = val;

  }

  /**
   * Configure the source with a string
   * @param config String to configure the source
   */
  @Override
  public void configSource(final String config) {

    if (config == null)
      this.pathname = "";
    else
      this.pathname = config;

  }

  /**
   * Get a message that describe the source.
   * @return a message that describe the source
   */
  @Override
  public String getSourceInfo() {

    return this.pathname;
  }

  @Override
  public InputStream getInputStream() {

    if (this.pathname == null)
      throw new NullPointerException("The path for the data source is null.");

    try {
      final Path path = new Path(this.pathname);
      final FileSystem fs = path.getFileSystem(new Configuration());

      final InputStream is = fs.open(path);

      final String extension = StringUtils.compressionExtension(this.pathname);

      if (Common.GZIP_EXTENSION.equals(extension))
        return CompressionType.createGZipInputStream(is);

      if (Common.BZIP2_EXTENSION.equals(extension))
        return CompressionType.createBZip2InputStream(is);

      return is;

    } catch (IOException e) {
      throw new EoulsanRuntimeException("IO error while reading URL data: "
          + pathname);
    }

  }

  @Override
  public String getSourceType() {

    return "s3n";
  }

  @Override
  public String toString() {
    return this.pathname;
  }

  //
  // Constructor
  //

  public S3DataSource(final String source) {

    this.pathname = source;
  }

}
