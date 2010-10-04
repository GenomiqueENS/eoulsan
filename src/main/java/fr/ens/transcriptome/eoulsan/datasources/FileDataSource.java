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
 * of the �cole Normale Sup�rieure and the individual authors.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.io.compression.CompressionFactory;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class implements a DataSource for a file.
 * @author Laurent Jourdren
 */
public class FileDataSource implements DataSource, Serializable {
  static final long serialVersionUID = 3045302852608368293L;

  private String file = "";
  private String baseDir = "";

  /**
   * Configure the source with properties
   * @param properties Properties to config the source
   */
  public void configSource(final Properties properties) {

    if (properties == null)
      return;

    String val = properties.getProperty("file");
    if (val == null)
      this.file = "";
    else
      this.file = val;

  }

  /**
   * Configure the source with a string
   * @param config String to configure the source
   */
  public void configSource(final String config) {

    this.baseDir = "";

    if (config == null)
      this.file = "";
    else
      this.file = config;

  }

  /**
   * Get a message that describe the source.
   * @return a message that describe the source
   */

  public String getSourceInfo() {

    return this.file;
  }

  /**
   * Get the source type (file, database...)
   * @return The type of source
   */
  public String getSourceType() {

    return "File";
  }

  /**
   * Get the inputStream for the source
   * @return The input stream
   */
  public InputStream getInputStream() {

    try {

      File f = new File(this.file);
      if (!f.exists())
        f = new File("".equals(this.baseDir) ? null : this.baseDir, this.file);

      final InputStream is = new FileInputStream(f);

      final String extension = StringUtils.compressionExtension(this.file);

      if (Common.GZIP_EXTENSION.equals(extension))
        return CompressionFactory.createGZInputStream(is);

      if (Common.BZIP2_EXTENSION.equals(extension))
        return CompressionFactory.createBZip2InputStream(is);

      return is;

    } catch (FileNotFoundException e) {
      throw new EoulsanRuntimeException("File not Found: " + this.file);
    } catch (IOException e) {
      throw new EoulsanRuntimeException("Error while decompressing: "
          + this.file);
    }
  }

  @Override
  public String toString() {

    return this.file;
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   */
  public FileDataSource() {
  }

  /**
   * Public constructor
   * @param file File to set
   */
  public FileDataSource(final String file) {

    configSource(file);
  }

  /**
   * Public constructor
   * @param baseDir of the file
   * @param file File to set
   */
  public FileDataSource(final String baseDir, final String file) {

    configSource(file);
    this.baseDir = baseDir;
  }

  /**
   * Public constructor
   * @param file File to set
   */
  public FileDataSource(final File file) {

    configSource(file == null ? null : file.getPath());
  }

}
