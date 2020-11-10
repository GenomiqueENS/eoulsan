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

package fr.ens.biologie.genomique.eoulsan.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * This class allow to load the list of available resources for a service
 * @since 1.2
 * @author Laurent Jourdren
 */
public class ServiceListLoader {

  private static final String PREFIX = "META-INF/services/";

  private final String serviceName;
  private final ClassLoader loader;

  public static final class Entry {

    private final URL url;
    private final int lineNumber;
    private final String value;

    /**
     * Get the URL of the file of the entry.
     * @return the URL of the file of the etry
     */
    public URL getUrl() {

      return this.url;
    }

    /**
     * Get line number of the entry.
     * @return the line number of the entry
     */
    public int getLineNumber() {

      return this.lineNumber;
    }

    /**
     * Get he value of the entry.
     * @return the value of the entry
     */
    public String getValue() {

      return this.value;
    }

    @Override
    public String toString() {

      return this.getClass().getSimpleName()
          + "{url=" + this.url + ", lineNumber=" + this.lineNumber + ", value="
          + this.value;
    }

    /**
     * Private constructor.
     * @param url URL
     * @param lineNumber line number
     * @param value value
     */
    private Entry(final URL url, final int lineNumber, final String value) {

      this.url = url;
      this.lineNumber = lineNumber;
      this.value = value;
    }

  }

  /**
   * Get the list of available services.
   * @return a list with the entries of the available services
   * @throws IOException if an error occurs while reading the list of services
   */
  private List<Entry> getServiceEntries() throws IOException {

    final String fullName = PREFIX + this.serviceName;
    final Enumeration<URL> urls;

    // Get the list of urls to the resources files
    if (this.loader == null) {
      urls = ClassLoader.getSystemResources(fullName);
    } else {
      urls = this.loader.getResources(fullName);
    }

    // Parse the URLs files
    final List<Entry> result = new ArrayList<>();
    for (URL url : Utils.newIterable(urls)) {
      parse(url, result);
    }

    return result;
  }

  /**
   * Parse a resource list.
   * @param url URL of the resource list
   * @param result the result object
   * @throws IOException if an error occurs while reading the list
   */
  private void parse(final URL url, final List<Entry> result)
      throws IOException {

    final InputStream is = url.openStream();
    BufferedReader reader = FileUtils.createBufferedReader(is);

    int lineNumber = 0;
    String line;
    while ((line = reader.readLine()) != null) {

      lineNumber++;

      final String trimLine = line.trim();
      if ("".equals(trimLine) || trimLine.startsWith("#")) {
        continue;
      }

      result.add(new Entry(url, lineNumber, trimLine));
    }

    is.close();
  }

  //
  // Static methods
  //

  /**
   * Get the list of available services.
   * @param serviceName name of the service
   * @throws IOException if an error occurs while reading the resources
   */
  public static List<String> load(final String serviceName) throws IOException {
    return load(serviceName, null);
  }

  /**
   * Get the list of available services.
   * @param serviceName name of the service
   * @param loader ClassLoader to use to read resources
   * @throws IOException if an error occurs while reading the resources
   */
  public static List<String> load(final String serviceName,
      final ClassLoader loader) throws IOException {

    final List<Entry> entries = loadEntries(serviceName, loader);
    final List<String> result = new ArrayList<>(entries.size());

    for (Entry e : entries) {
      result.add(e.getValue());
    }

    return result;
  }

  /**
   * Get the list of available services.
   * @param serviceName name of the service
   * @throws IOException if an error occurs while reading the resources
   */
  public static List<Entry> loadEntries(final String serviceName)
      throws IOException {
    return loadEntries(serviceName, null);
  }

  /**
   * Get the list of available services.
   * @param serviceName name of the service
   * @param loader ClassLoader to use to read resources
   * @throws IOException if an error occurs while reading the resources
   */
  public static List<Entry> loadEntries(final String serviceName,
      final ClassLoader loader) throws IOException {

    return new ServiceListLoader(serviceName, loader).getServiceEntries();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param serviceName name of the service
   * @param loader class loader to use to load resource files
   */
  private ServiceListLoader(final String serviceName,
      final ClassLoader loader) {

    if (serviceName == null) {
      throw new NullPointerException("The service name is null");
    }

    this.loader = loader == null ? this.getClass().getClassLoader() : loader;

    this.serviceName = serviceName;
  }

}
