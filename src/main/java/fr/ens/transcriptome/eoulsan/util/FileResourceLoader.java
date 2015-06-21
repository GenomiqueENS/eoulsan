package fr.ens.transcriptome.eoulsan.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This class allow to define a resource loader for files.
 * @param <S> Type of the data to load
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class FileResourceLoader<S> implements ResourceLoader<S> {

  private final static String INDEX_FILE = "index";

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

  //
  // Resources loading
  //

  @Override
  public List<S> loadResources() throws IOException, EoulsanException {

    if (this.directories.isEmpty()) {
      return Collections.emptyList();
    }

    final List<S> result = new ArrayList<>();

    for (DataFile directory : this.directories) {

      if (!directory.exists()) {
        return Collections.emptyList();
      }

      for (String filename : findResourcePaths(directory)) {

        getLogger().fine(
            "Try to load "
                + this.clazz.getSimpleName() + " from " + filename
                + " resource");

        final DataFile file = new DataFile(directory, filename);

        result.add(load(file.open()));
      }
    }

    return Collections.unmodifiableList(result);
  }

  /**
   * Find the resource to load.
   * @param directory the directory where loading the resources
   * @return a list of relative paths
   * @throws IOException
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
            && filename.toLowerCase().endsWith(extension.toLowerCase())) {
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
   * Add a resource path.
   * @param resourcePath the resource path to add
   */
  public void addResources(final String resourcePaths) {

    checkNotNull(resourcePaths, "resourcePaths argument cannot be null");

    for (String directory : resourcePaths.split(File.pathSeparator)) {

      directory = directory.trim();

      if (!directory.isEmpty()) {
        addResource(new DataFile(directory));
      }
    }
  }

  /**
   * Add a resource path.
   * @param resourcePath the resource path to add
   */
  public void addResource(final DataFile resourcePath) {

    checkNotNull(resourcePath, "baseDir argument cannot be null");

    this.directories.add(resourcePath);
  }

  /**
   * Add a resource path.
   * @param resourcePath the resource path to remove
   * @return true if the resource has been successfully removed
   */
  public boolean removeResource(final DataFile resourcePath) {

    checkNotNull(resourcePath, "baseDir argument cannot be null");

    return this.directories.remove(resourcePath);
  }

  //
  // Public constructor
  //

  /**
   * Public constructor.
   * @param clazz the Class type of the resource to load @param resourcePath the
   *          path to the resource to load
   */
  public FileResourceLoader(final Class<S> clazz, final DataFile resourcePath) {

    checkNotNull(clazz, "clazz argument cannot be null");

    this.clazz = clazz;
    addResource(resourcePath);
  }

}
