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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.datatypes.DataProtocolRegistry;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This classe define the Runtime to execute low level IO operation for Eoulsan
 * in local mode.
 * @author Laurent Jourdren
 */
public class LocalEoulsanRuntime extends AbstractEoulsanRuntime {

  @Override
  public boolean isHadoopMode() {

    return false;
  }

  @Override
  public boolean isAmazonMode() {

    return false;
  }

  @Override
  public InputStream getInputStream(String dataSource) throws IOException {

    if (dataSource == null)
      throw new NullPointerException("The datasource is null.");

    final File file = new File(dataSource);

    return decompressInputStreamIsNeeded(FileUtils.createInputStream(file),
        dataSource);
  }

  @Override
  public InputStream getRawInputStream(String dataSource) throws IOException {
    if (dataSource == null)
      throw new NullPointerException("The datasource is null.");

    final File file = new File(dataSource);

    return FileUtils.createInputStream(file);
  }

  @Override
  public OutputStream getOutputStream(String dataSource) throws IOException {

    if (dataSource == null)
      throw new NullPointerException("The datasource is null.");

    final File file = new File(dataSource);

    return FileUtils.createOutputStream(file);
  }

  //
  // Constructor
  //

  /**
   * Public constructor, initialize the runtime.
   * @param settings Settings of the application
   */
  public static final LocalEoulsanRuntime newEoulsanRuntime(
      final Settings settings) {

    // Create instance
    final LocalEoulsanRuntime instance = new LocalEoulsanRuntime(settings);

    // Set the instance
    EoulsanRuntime.setInstance(instance);

    // Register protocols from settings
    DataProtocolRegistry.getInstance().registerProtocolsFromSettings();

    return instance;
  }

  /**
   * Private constructor.
   * @param settings Settings of the application
   */
  private LocalEoulsanRuntime(final Settings settings) {

    super(settings);
  }

}
