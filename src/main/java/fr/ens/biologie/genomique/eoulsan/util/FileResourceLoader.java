package fr.ens.biologie.genomique.eoulsan.util;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceConfigurationError;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.kenetre.io.FileUtils;

/**
 * This class allow to define a resource loader for files.
 * @param <S> Type of the data to load
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class FileResourceLoader<S> extends AbstractResourceLoader<S> {

  private final static String INDEX_FILE = "INDEX";

  private final Class<S> clazz;
  private final List<DataFile> directories = new ArrayList<>();

  //
  // Abstract methods
  //

  /**
   * Get the extension of the files to load.
   * @return the extension of the files to load
   */
  protected abstract String getExtension();

  //
  // Resources loading
  //

  @Override
  protected InputStream getResourceAsStream(final String resourcePath)
      throws IOException {

    requireNonNull(resourcePath, "resourcePath argument cannot be null");

    return new DataFile(resourcePath).open();
  }

  @Override
  public void reload() {

    if (this.directories.isEmpty()) {
      return;
    }

    try {
      for (DataFile directory : this.directories) {

        for (String filename : findResourcePaths(directory)) {

          getLogger().fine("Try to load "
              + this.clazz.getSimpleName() + " from " + directory + "/" + filename + " resource");

          final DataFile file = new DataFile(directory, filename);

          final S resource = load(file.open(), file.getSource());

          if (resource == null) {
            throw new EoulsanException("Cannot load resource: " + file);
          }

          final String resourceName = getResourceName(resource);

          if (resourceName == null) {
            throw new EoulsanException("Cannot get resource name for resource: "
                + resource + " (file: " + file + ")");
          }

          addResource(resourceName, file.getSource());
        }
      }
    } catch (IOException | EoulsanException e) {
      throw new ServiceConfigurationError(
          "Unable to load resource: " + e.getMessage(), e);
    }
  }

  /**
   * Find the resource to load.
   * @param directory the directory where loading the resources
   * @return a list of relative paths
   * @throws IOException if an error occrs while finding the resource
   */
  private List<String> findResourcePaths(final DataFile directory)
      throws IOException {

    final DataFile indexFile = new DataFile(directory, INDEX_FILE);

    if (indexFile.exists()) {
      return findResourcePathInIndexFile(indexFile);
    }

    return findResourcePathInDirectory(directory);
  }

  /**
   * Find resources in an index file.
   * @param indexFile the index where getting resources
   * @return a list with the list of the resources to load
   */
  private List<String> findResourcePathInIndexFile(final DataFile indexFile)
      throws IOException {

    final List<String> result = new ArrayList<>();

    try (BufferedReader reader =
        FileUtils.createBufferedReader(indexFile.open())) {

      String line = null;

      while ((line = reader.readLine()) != null) {

        final String trimLine = line.trim();
        if ("".equals(trimLine) || trimLine.startsWith("#")) {
          continue;
        }

        result.add(trimLine);
      }
    }

    return result;
  }

  /**
   * Find resources in a directory.
   * @param directory the directory where search resources
   * @return a list with the list of the resources to load
   */
  private List<String> findResourcePathInDirectory(final DataFile directory) {

    final List<String> result = new ArrayList<>();

    final String extension = getExtension() == null ? "" : getExtension();

    try {
      for (DataFile file : directory.list()) {

        final String filename = file.getName();

        if (!filename.startsWith(".")
            && filename.toLowerCase(Globals.DEFAULT_LOCALE).endsWith(extension.toLowerCase(Globals.DEFAULT_LOCALE))) {
          result.add(file.getName());
        }
      }
    } catch (IOException e) {
      // The protocol is not browsable, do nothing
    }

    return result;
  }

  //
  // Resource path management
  //

  /**
   * Add a resource paths.
   * @param resourcePaths the resource path to add
   */
  public void addResourcePaths(final Collection<String> resourcePaths) {

    requireNonNull(resourcePaths, "resourcePaths argument cannot be null");

    for (String directory : resourcePaths) {

      directory = directory.trim();

      if (!directory.isEmpty()) {
        addResourcePath(new DataFile(directory));
      }
    }
  }

  /**
   * Add a resource path.
   * @param resourcePath the resource path to add
   */
  public void addResourcePath(final DataFile resourcePath) {

    requireNonNull(resourcePath, "baseDir argument cannot be null");

    this.directories.add(resourcePath);
  }

  /**
   * Add a resource path.
   * @param resourcePath the resource path to remove
   * @return true if the resource has been successfully removed
   */
  public boolean removeResourcePath(final DataFile resourcePath) {

    requireNonNull(resourcePath, "baseDir argument cannot be null");

    return this.directories.remove(resourcePath);
  }

  //
  // Public constructor
  //

  /**
   * Public constructor.
   * @param clazz the Class type of the resource to load 
   * @param resourcePath the path to the resource to load
   */
  public FileResourceLoader(final Class<S> clazz, final DataFile resourcePath) {

    requireNonNull(clazz, "clazz argument cannot be null");

    this.clazz = clazz;
    addResourcePath(resourcePath);
  }

}
