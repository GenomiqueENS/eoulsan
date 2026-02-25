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

import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFileMetadata;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class define an abstract class for DataProtocols based on the URL class.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class URLDataProtocol extends AbstractDataProtocol {

  private URLConnection createConnection(final DataFile src) throws IOException {

    if (src == null) {
      throw new NullPointerException("The source is null.");
    }

    try {
      return new URL(src.getSource()).openConnection();
    } catch (MalformedURLException e) {
      throw new IOException("Invalid URL: " + src);
    }
  }

  @Override
  public InputStream getData(final DataFile src) throws IOException {

    return createConnection(src).getInputStream();
  }

  @Override
  public OutputStream putData(final DataFile src) throws IOException {

    return createConnection(src).getOutputStream();
  }

  @Override
  public DataFileMetadata getMetadata(final DataFile src) throws IOException {

    if (!exists(src, true)) {
      throw new FileNotFoundException("File not found: " + src);
    }

    final URLConnection con = createConnection(src);

    final SimpleDataFileMetadata result = new SimpleDataFileMetadata();
    result.setContentLength(con.getContentLength());
    result.setLastModified(con.getLastModified());
    result.setContentType(con.getContentType());
    result.setContentEncoding(con.getContentEncoding());

    return result;
  }

  @Override
  public boolean canRead() {

    return true;
  }

  @Override
  public boolean canWrite() {

    return true;
  }

  @Override
  public boolean exists(final DataFile src, final boolean followLink) {

    try {
      createConnection(src);
    } catch (IOException e) {
      return false;
    }

    return true;
  }
}
