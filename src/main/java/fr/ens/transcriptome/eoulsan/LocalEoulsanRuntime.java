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
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public InputStream getRawInputStream(String dataSource) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OutputStream getOutputStream(String dataSource) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  //
  // Init method
  //

  /**
   * Initialize the runtime.
   * @param settings Settings of the application
   */
  public static void init(final Settings settings) {

    EoulsanRuntime.setInstance(new LocalEoulsanRuntime(settings));
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param settings Settings of the application
   */
  public LocalEoulsanRuntime(final Settings settings) {

    super(settings);
  }

}
