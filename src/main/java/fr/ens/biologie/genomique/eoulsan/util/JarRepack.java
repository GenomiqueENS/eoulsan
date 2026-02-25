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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * This class allow to repackage a jar file.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public class JarRepack {

  /** The default size of the buffer. */
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

  private final ZipOutputStream zos;

  private void copy(final Path file) throws IOException {

    final ZipInputStream zin = new ZipInputStream(Files.newInputStream(file));
    final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

    ZipEntry entry = zin.getNextEntry();

    do {

      final String entryName = entry.getName();
      this.zos.putNextEntry(new ZipEntry(entryName));

      long count = 0;
      int n = 0;
      while ((n = zin.read(buffer)) != -1) {
        this.zos.write(buffer, 0, n);
        count += n;
      }

      if (entry.getSize() != count) {
        throw new IOException(
            "Copied size of zip entry " + count + " is not as excepted: " + entry.getSize());
      }

    } while ((entry = zin.getNextEntry()) != null);

    zin.close();
  }

  /**
   * Add a file to the jar file.
   *
   * @param file file to add
   * @param destDir destination in the jar file
   * @throws IOException if an error occurs while adding the file
   */
  public void addFile(final Path file, final String destDir) throws IOException {

    if (file == null) {
      return;
    }

    final byte[] data = new byte[DEFAULT_BUFFER_SIZE];

    this.zos.putNextEntry(new ZipEntry(destDir + file.getFileName()));
    final InputStream fis = Files.newInputStream(file);

    BufferedInputStream origin = new BufferedInputStream(fis, DEFAULT_BUFFER_SIZE);

    long count = 0;
    int n;
    while ((n = origin.read(data, 0, DEFAULT_BUFFER_SIZE)) != -1) {
      this.zos.write(data, 0, n);
      count += n;
    }

    if (Files.size(file) != count) {
      origin.close();
      throw new IOException(
          "Copied size of zip entry " + count + " is not as excepted: " + Files.size(file));
    }

    origin.close();
  }

  /**
   * Close the repackaged file.
   *
   * @throws IOException if an error occurs while closing the repackaged file
   */
  public void close() throws IOException {

    this.zos.close();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   *
   * @param inFile the source jar file to repackage
   * @param outFile the path to the new repackaged file
   * @throws IOException if an error occurs while opening the source jar file
   */
  public JarRepack(final Path inFile, final Path outFile) throws IOException {

    if (inFile == null) {
      throw new NullPointerException("the inFile argument is null.");
    }

    if (outFile == null) {
      throw new NullPointerException("the outFile argument is null.");
    }

    this.zos = new ZipOutputStream(Files.newOutputStream(outFile));
    copy(inFile);
  }
}
