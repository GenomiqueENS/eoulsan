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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * This class define the Runtime to execute low level IO operation for Eoulsan in local mode.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class LocalEoulsanRuntime extends AbstractEoulsanRuntime {

  private EoulsanExecMode mode;

  @Override
  public EoulsanExecMode getMode() {

    return this.mode != null ? this.mode : EoulsanExecMode.LOCAL;
  }

  @Override
  public File getTempDirectory() {

    return getSettings().getTempDirectoryFile();
  }

  @Override
  public InputStream getInputStream(final String dataSource) throws IOException {

    if (dataSource == null) {
      throw new IllegalArgumentException("The datasource is null.");
    }

    return decompressInputStreamIsNeeded(Files.newInputStream(Path.of(dataSource)), dataSource);
  }

  @Override
  public InputStream getRawInputStream(final String dataSource) throws IOException {

    if (dataSource == null) {
      throw new IllegalArgumentException("The datasource is null.");
    }

    return Files.newInputStream(Path.of(dataSource));
  }

  @Override
  public OutputStream getOutputStream(final String dataSource) throws IOException {

    if (dataSource == null) {
      throw new IllegalArgumentException("The datasource is null.");
    }

    return Files.newOutputStream(Path.of(dataSource));
  }

  /**
   * Set the cluster mode.
   *
   * @param mode Eoulsan execution mode
   */
  public void setMode(final EoulsanExecMode mode) {

    if (mode == null) {
      throw new NullPointerException("mode argument cannot be null");
    }

    if (this.mode != null) {
      throw new IllegalStateException("Eoulsan mode has been already set");
    }

    this.mode = mode;
  }

  //
  // Constructor
  //

  /**
   * Public constructor, initialize the runtime.
   *
   * @param settings Settings of the application
   * @return a local Eoulsan runtime
   */
  public static LocalEoulsanRuntime newEoulsanRuntime(final Settings settings) {

    // Create instance
    final LocalEoulsanRuntime instance = new LocalEoulsanRuntime(settings);

    // Set the instance
    EoulsanRuntime.setInstance(instance);

    return instance;
  }

  /**
   * Private constructor.
   *
   * @param settings Settings of the application
   */
  private LocalEoulsanRuntime(final Settings settings) {

    super(settings);
  }

  /**
   * Initialization Eoulsan runtime for external application who needed Eoulsan
   *
   * @throws IOException if an error occurs while initializing the runtime
   * @throws EoulsanException if an error occurs while initializing the runtime
   */
  public static void initEoulsanRuntimeForExternalApp() throws IOException, EoulsanException {

    if (!EoulsanRuntime.isRuntime()) {
      newEoulsanRuntime(new Settings(true));
      ((LocalEoulsanRuntime) EoulsanRuntime.getRuntime()).setMode(EoulsanExecMode.EXTERNAL_APP);
    }

    // Disable logging
    final Handler[] handlers = EoulsanLogger.getLogger().getHandlers();
    if (handlers != null) {
      for (Handler handler : handlers) {
        handler.setLevel(Level.OFF);
      }
    }
  }
}
