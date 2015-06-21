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

package fr.ens.transcriptome.eoulsan.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * This class allow to define a resource loader for resources in the class path.
 * @param <S> Type of the data to load
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class ClassPathResourceLoader<S> implements ResourceLoader<S> {

  private final Class<S> clazz;
  private final String resourcePath;

  /**
   * Load a resource.
   * @param in input stream
   * @return a resource object
   * @throws IOException if an error occurs while loading the resource
   * @throws EoulsanException if an error occurs while creating the resource
   *           object
   */
  protected abstract S load(final InputStream in) throws IOException,
      EoulsanException;

  @Override
  public List<S> loadResources() throws IOException, EoulsanException {

    final List<S> result = new ArrayList<>();

    // Get the classloader
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    for (String filename : ServiceListLoader.load(this.clazz.getName())) {

      final String resource = this.resourcePath + filename;
      getLogger().fine(
          "Try to load "
              + this.clazz.getSimpleName() + " from " + filename + " resource");

      result.add(load(loader.getResourceAsStream(resource)));
    }

    return Collections.unmodifiableList(result);
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
  public ClassPathResourceLoader(final Class<S> clazz, final String resourcePath) {

    checkNotNull(clazz, "clazz argument cannot be null");
    checkNotNull(resourcePath, "resourcePath argument cannot be null");

    this.clazz = clazz;
    this.resourcePath = resourcePath;
  }

}
