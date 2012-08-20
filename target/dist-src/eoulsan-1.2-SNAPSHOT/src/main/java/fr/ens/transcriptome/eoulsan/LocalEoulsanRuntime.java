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

package fr.ens.transcriptome.eoulsan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This classe define the Runtime to execute low level IO operation for Eoulsan
 * in local mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class LocalEoulsanRuntime extends AbstractEoulsanRuntime {

  @Override
  public boolean isHadoopMode() {

    return false;
  }

  @Override
  public boolean isAmazonMode() {

    return false;
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

    final File file = new File(dataSource);

    return decompressInputStreamIsNeeded(FileUtils.createInputStream(file),
        dataSource);
  }

  @Override
  public InputStream getRawInputStream(final String dataSource)
      throws IOException {

    if (dataSource == null) {
      throw new IllegalArgumentException("The datasource is null.");
    }

    final File file = new File(dataSource);

    return FileUtils.createInputStream(file);
  }

  @Override
  public OutputStream getOutputStream(final String dataSource)
      throws IOException {

    if (dataSource == null) {
      throw new IllegalArgumentException("The datasource is null.");
    }

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
  public static LocalEoulsanRuntime newEoulsanRuntime(final Settings settings) {

    // Create instance
    final LocalEoulsanRuntime instance = new LocalEoulsanRuntime(settings);

    // Set the instance
    EoulsanRuntime.setInstance(instance);

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
