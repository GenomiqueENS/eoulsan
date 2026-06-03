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

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFileMetadata;
import fr.ens.biologie.genomique.eoulsan.data.storages.DataFileStorage;
import fr.ens.biologie.genomique.kenetre.util.Utils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This abstract class define a storage protocol. It is useful to easily access common resources
 * like genomes or annotations.
 *
 * @since 1.1
 * @author Laurent Jourdren
 */
public abstract class StorageDataProtocol extends AbstractDataProtocol {

  private static final ReentrantLock JVM_LOCK = new ReentrantLock();

  /**
   * Get the path where searching the files.
   *
   * @return a string with the path where search the files
   */
  protected abstract String getBasePath();

  /**
   * Get the file extensions of the files to search.
   *
   * @return a string with file extension of the files to search
   */
  protected String getExtension() {

    throw new NullPointerException(
        "No extension has been defined for the storage protocol: " + getName());
  }

  /**
   * Get the list of the file extensions of the files to search.
   *
   * @return a list with file extensions of the files to search
   */
  protected List<String> getExtensions() {

    return Collections.singletonList(getExtension());
  }

  @Override
  public InputStream getData(final DataFile src) throws IOException {

    return getUnderLyingData(src).rawOpen();
  }

  @Override
  public OutputStream putData(final DataFile dest) throws IOException {

    throw new IOException("PutData() method is no supported by " + getName() + " protocol");
  }

  @Override
  public boolean exists(final DataFile src, final boolean followLink) {

    try {

      return getUnderLyingData(src).exists();
    } catch (IOException e) {

      return false;
    }
  }

  @Override
  public DataFileMetadata getMetadata(final DataFile src) throws IOException {

    return getUnderLyingData(src).getMetaData();
  }

  @Override
  public boolean canRead() {

    return true;
  }

  @Override
  public boolean canWrite() {

    return false;
  }

  @Override
  public File getSourceAsFile(final DataFile src) {

    try {
      return getUnderLyingData(src).toFile();
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Get the underlying Data.
   *
   * @param src source to use
   * @return a the underlying DataFile
   * @throws IOException if an error occurs while getting the underlying DataFile
   */
  public DataFile getUnderLyingData(final DataFile src) throws IOException {

    final String basePath = getBasePath();

    if (basePath == null) {
      throw new IOException(getName() + " storage is not configured");
    }

    DataFileStorage storage = new DataFileStorage(basePath, getExtensions());

    DataFile result = storage.getDataFile(src.getName());

    if (result == null) {
      throw new IOException("No " + getName() + " found for: " + src.getName());
    }

    if (result.isLocalFile() && EoulsanRuntime.getSettings().isStorageUsageLog()) {

      Path logPath = Path.of(basePath, "usage.log");
      logGet(logPath, src, result);
    }

    return result;
  }

  //
  // Log methods
  //

  private static void logGet(Path logPath, DataFile in, DataFile out) {

    if (logPath == null) {
      return;
    }

    try {
      if (!Files.isRegularFile(logPath)) {

        // Create the log file
        appendLineWithLock(logPath, "#Date\tURL\tFile");
      }

      StringBuilder sb = new StringBuilder();
      sb.append(OffsetDateTime.now());
      sb.append('\t');
      sb.append(in);
      sb.append('\t');
      sb.append(out);

      appendLineWithLock(logPath, sb.toString());

    } catch (IOException e) {
      Utils.nop();
    }
  }

  private static void appendLineWithLock(Path filePath, String line) throws IOException {

    JVM_LOCK.lock();
    try (FileChannel channel =
            FileChannel.open(
                filePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE);
        FileLock lock = channel.lock()) {

      channel.write(Charset.defaultCharset().encode(line + System.lineSeparator()));
    } finally {
      JVM_LOCK.unlock();
    }
  }
}
