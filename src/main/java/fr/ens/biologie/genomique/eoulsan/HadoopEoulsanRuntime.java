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

package fr.ens.biologie.genomique.eoulsan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * This class define the Runtime to execute low level IO operation for Eoulsan in Hadoop mode.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class HadoopEoulsanRuntime extends AbstractEoulsanRuntime {

  private static final String HADOOP_TEMP_DIR = "mapreduce.cluster.temp.dir";

  private final Configuration conf;
  private final EoulsanExecMode mode;

  /**
   * Get Hadoop configuration.
   *
   * @return Hadoop Configuration object
   */
  public Configuration getConfiguration() {

    return this.conf;
  }

  //
  // EoulsanRuntime methods
  //

  @Override
  public EoulsanExecMode getMode() {

    return mode;
  }

  @Override
  public File getTempDirectory() {

    // In Hadoop task mode do not use Eoulsan settings to get the temporary
    // directory as it often does not exists
    if (this.mode == EoulsanExecMode.HADOOP_TASK) {

      // Use Hadoop temporary directory if defined or the default JVM
      // temporary directory
      return java.nio.file.Path.of(
              this.conf.get(HADOOP_TEMP_DIR, System.getProperty("java.io.tmpdir")))
          .toFile();
    }

    return getSettings().getTempDirectoryFile();
  }

  @Override
  public InputStream getInputStream(final String dataSource) throws IOException {

    if (dataSource == null) {
      throw new IllegalArgumentException("The datasource is null.");
    }

    final Path p = new Path(dataSource);
    final FileSystem fs = p.getFileSystem(this.conf);

    return decompressInputStreamIsNeeded(fs.open(p), dataSource);
  }

  @Override
  public InputStream getRawInputStream(final String dataSource) throws IOException {

    if (dataSource == null) {
      throw new IllegalArgumentException("The datasource is null.");
    }

    final Path p = new Path(dataSource);
    final FileSystem fs = p.getFileSystem(this.conf);

    return fs.open(p);
  }

  @Override
  public OutputStream getOutputStream(final String dataSource) throws IOException {

    if (dataSource == null) {
      throw new IllegalArgumentException("The datasource is null.");
    }

    final Path p = new Path(dataSource);
    final FileSystem fs = p.getFileSystem(this.conf);

    return fs.create(p);
  }

  //
  // Constructor
  //

  /**
   * Public constructor, initialize the runtime. This constructor is useful in mappers or reducers
   * for initialize Eoulsan DataProtocols.
   *
   * @param conf Hadoop configuration of the application
   * @return an Hadoop Eoulsan runtime
   * @throws IOException if error occurs while creating the runtime
   */
  public static HadoopEoulsanRuntime newEoulsanRuntime(final Configuration conf)
      throws IOException {

    try {
      return newEoulsanRuntime(new Settings(false), conf, EoulsanExecMode.HADOOP_TASK);
    } catch (EoulsanException e) {

      throw new IOException(e);
    }
  }

  /**
   * Package constructor, initialize the runtime. This constructor can only called by MainHadoop
   * class.
   */
  static HadoopEoulsanRuntime newEoulsanRuntime(final Settings settings) {

    // Create Hadoop configuration object
    final Configuration conf = CommonHadoop.createConfiguration(settings);

    // Initialize runtime
    return newEoulsanRuntime(settings, conf, EoulsanExecMode.HADOOP);
  }

  /**
   * Private constructor, initialize the runtime.
   *
   * @param settings Settings of the application
   * @param conf Hadoop configuration object
   */
  private static synchronized HadoopEoulsanRuntime newEoulsanRuntime(
      final Settings settings, final Configuration conf, final EoulsanExecMode mode) {

    // Create instance
    final HadoopEoulsanRuntime instance = new HadoopEoulsanRuntime(settings, conf, mode);

    // Set the instance
    EoulsanRuntime.setInstance(instance, true);

    return (HadoopEoulsanRuntime) EoulsanRuntime.getRuntime();
  }

  /**
   * Private constructor.
   *
   * @param settings Settings of the application
   * @param conf Hadoop configuration object
   */
  private HadoopEoulsanRuntime(
      final Settings settings, final Configuration conf, final EoulsanExecMode mode) {

    super(settings);

    if (conf == null) {
      throw new IllegalArgumentException("The configuration is null");
    }

    this.conf = conf;
    this.mode = mode;
  }
}
