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
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;

/**
 * This class define an URL DataSource.
 * @author Laurent Jourdren
 */
public class URLDataSource extends FileDataSource implements Serializable {

  private String url = "";

  /**
   * Configure the source with properties
   * @param properties Properties to config the source
   */
  public void configSource(final Properties properties) {

    if (properties == null)
      return;

    String val = properties.getProperty("file");
    if (val == null)
      this.url = "";
    else
      this.url = val;

  }

  /**
   * Configure the source with a string
   * @param config String to configure the source
   */
  public void configSource(final String config) {

    if (config == null)
      this.url = "";
    else
      this.url = config;

  }

  /**
   * Get a message that describe the source.
   * @return a message that describe the source
   */

  public String getSourceInfo() {

    return this.url;
  }

  /**
   * Get the inputStream for the source
   * @return The input stream
   */
  public InputStream getInputStream() {

    try {

      return new URL(this.url).openStream();
    } catch (MalformedURLException e) {

      try {
        URL url = (new File(this.url)).toURI().toURL();
        return url.openStream();

      } catch (MalformedURLException e1) {
        throw new EoulsanRuntimeException("Invalid URL: " + this.url);
      } catch (IOException e1) {
        throw new EoulsanRuntimeException("IO error while reading URL data: "
            + url);
      }
    } catch (IOException e) {
      throw new EoulsanRuntimeException("IO error while reading URL data: "
          + url);
    }

  }

  @Override
  public String toString() {

    return this.url;
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   */
  public URLDataSource() {
  }

  /**
   * Public constructor
   * @param url URL to set
   */
  public URLDataSource(final String url) {

    configSource(url);
  }

  /**
   * Public constructor
   * @param url URL to set
   */
  public URLDataSource(final URL url) {

    configSource(url == null ? null : url.toString());
  }

}
