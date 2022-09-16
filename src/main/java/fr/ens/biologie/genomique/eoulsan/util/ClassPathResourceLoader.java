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

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ServiceConfigurationError;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.kenetre.util.ServiceListLoader;

/**
 * This class allow to define a resource loader for resources in the class path.
 * @param <S> Type of the data to load
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class ClassPathResourceLoader<S>
    extends AbstractResourceLoader<S> {

  private final Class<S> clazz;
  private final String resourceBasePath;

  //
  // Resources loading
  //

  @Override
  protected InputStream getResourceAsStream(final String resourcePath)
      throws IOException {

    requireNonNull(resourcePath, "resourcePath argument cannot be null");

    // Get the classloader
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    return loader.getResourceAsStream(resourcePath);
  }

  @Override
  public void reload() {

    try {

      for (String filename : ServiceListLoader.load(this.clazz.getName())) {

        final String resourcePath = this.resourceBasePath + filename;
        getLogger().fine("Try to load "
            + this.clazz.getSimpleName() + " from " + filename + " resource");

        final S resource =
            load(getResourceAsStream(resourcePath), resourcePath);

        if (resource == null) {
          throw new EoulsanException("Cannot load resource: " + resourcePath);
        }

        final String resourceName = getResourceName(resource);

        if (resourceName == null) {
          throw new EoulsanException(
              "Cannot get resource name for resource: " + resource);
        }

        addResource(resourceName, resourcePath);
      }
    } catch (IOException | EoulsanException e) {
      throw new ServiceConfigurationError("Unable to load resource", e);
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param clazz the Class type of the resource to load
   * @param resourcePath the path in the class path where are the resources to
   *          load
   */
  public ClassPathResourceLoader(final Class<S> clazz,
      final String resourcePath) {

    requireNonNull(clazz, "clazz argument cannot be null");
    requireNonNull(resourcePath, "resourcePath argument cannot be null");

    this.clazz = clazz;
    this.resourceBasePath = resourcePath;
  }

}
