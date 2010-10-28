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

package fr.ens.transcriptome.eoulsan;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * This classe define the Runtime to execute low level IO operation for Eoulsan
 * in Hadoop mode.
 * @author Laurent Jourdren
 */
public class HadoopEoulsanRuntime extends AbstractEoulsanRuntime {

  private Configuration conf;

  /**
   * Get Hadoop configuration.
   * @return Hadoop Configuration object
   */
  public Configuration getConfiguration() {

    return this.conf;
  }

  //
  // EoulsanRuntime methods
  //

  @Override
  public boolean isHadoopMode() {

    return true;
  }

  @Override
  public boolean isAmazonMode() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public InputStream getInputStream(final String dataSource) throws IOException {

    if (dataSource == null)
      throw new NullPointerException("The datasource is null.");

    final Path p = new Path(dataSource);
    final FileSystem fs = p.getFileSystem(this.conf);

    return decompressInputStreamIsNeeded(fs.open(p), dataSource);
  }

  @Override
  public InputStream getRawInputStream(String dataSource) throws IOException {

    if (dataSource == null)
      throw new NullPointerException("The datasource is null.");

    final Path p = new Path(dataSource);
    final FileSystem fs = p.getFileSystem(this.conf);

    return fs.open(p);
  }

  @Override
  public OutputStream getOutputStream(String dataSource) throws IOException {

    if (dataSource == null)
      throw new NullPointerException("The datasource is null.");

    final Path p = new Path(dataSource);
    final FileSystem fs = p.getFileSystem(this.conf);

    return fs.create(p);
  }

  //
  // Init method
  //

  /**
   * Initialize the runtime.
   * @param settings Settings of the application
   * @param conf Hadoop configuration object
   */
  public static void init(final Settings settings, final Configuration conf) {

    EoulsanRuntime.setInstance(new HadoopEoulsanRuntime(settings, conf));
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param settings Settings of the application
   * @param conf Hadoop configuration object
   */
  private HadoopEoulsanRuntime(final Settings settings, final Configuration conf) {

    super(settings);

    if (conf == null)
      throw new NullPointerException("The configuration is null");

    this.conf = conf;
  }

}
