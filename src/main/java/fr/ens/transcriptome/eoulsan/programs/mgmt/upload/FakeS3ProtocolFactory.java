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

package fr.ens.transcriptome.eoulsan.programs.mgmt.upload;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import fr.ens.transcriptome.eoulsan.Common;

/**
 * This is a fake S3 protocol factory used when upload data from local to s3.
 * @author Laurent Jourdren
 */
public class FakeS3ProtocolFactory implements URLStreamHandlerFactory {

  @Override
  public URLStreamHandler createURLStreamHandler(String protocol) {

    if (protocol.equals(Common.S3_PROTOCOL)) {

      return new URLStreamHandler() {

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
          // TODO Auto-generated method stub
          return null;
        }

      };
    }

    if (protocol.equals("hdfs")) {

      return new URLStreamHandler() {

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
          // TODO Auto-generated method stub
          return null;
        }

      };
    }

    return null;
  }

}
