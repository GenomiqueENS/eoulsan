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

package fr.ens.biologie.genomique.eoulsan.util.hadoop;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.util.JarRepack;

/**
 * This class allow to repackage the application.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class HadoopJarRepackager {

  private static final String DIR_IN_JAR = "lib/";

  private final List<Path> jarFiles = new ArrayList<>();
  private Path srcJar;

  /**
   * Proceed to the repackaging
   * @param destJarFile path to the repackaged jar file
   * @throws IOException if an error occurs while repackage the application
   */
  private void doIt(final Path destJarFile) throws IOException {

    if (this.srcJar == null) {
      throw new IOException(
          "No source jar found in the paths of libraries to repack.");
    }

    getLogger().info("Repackage " + this.srcJar + " in " + destJarFile);

    final JarRepack jarRepack = new JarRepack(this.srcJar, destJarFile);
    for (Path file : this.jarFiles) {
      jarRepack.addFile(file, DIR_IN_JAR);
      getLogger().fine("Add in repackaged jar file: " + file.getFileName());
    }

    jarRepack.close();
  }

  //
  // Static methods
  //

  /**
   * Repackage the jar application.
   * @param destJarFile path to the repackaged jar file
   * @throws IOException if an error occurs while repackage the application
   */
  public static void repack(final Path destJarFile) throws IOException {

    final HadoopJarRepackager hjr = new HadoopJarRepackager(
        System.getProperty(Globals.LIBS_TO_HADOOP_REPACK_PROPERTY),
        Globals.APP_NAME_LOWER_CASE
            + '-' + Globals.APP_VERSION_STRING + ".jar");

    hjr.doIt(destJarFile);
  }

  /**
   * Repackage the jar application only if the repackaged file does not exist.
   * @return the File repackaged jar file
   * @throws IOException if an error occurs while repackage the application
   */
  public static Path repack() throws IOException {

    Path result = Path.of(createRepackagedJarName());

    if (!Files.exists(result)) {
      repack(result);
    }

    return result;
  }

  private static String createRepackagedJarName() {

    return Globals.APP_NAME_LOWER_CASE
        + "-" + Globals.APP_VERSION_STRING
        + (Globals.DEBUG ? "-" + Globals.APP_BUILD_NUMBER : "") + "-"
        + Integer.toHexString(
            System.getProperty(Globals.LIBS_TO_HADOOP_REPACK_PROPERTY).trim()
                .hashCode())
        + ".jar";
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   * @param libPaths a String with all the paths of the file to repackage
   * @param jarName the name of the jar file to repackage. This jar file must be
   *          in the libPaths
   */
  private HadoopJarRepackager(final String libPaths, final String jarName) {

    if (libPaths == null) {
      throw new IllegalArgumentException(
          "The paths of libraries to repack is null.");
    }

    if (jarName == null) {
      throw new IllegalArgumentException(
          "The name of the jar to repack is null.");
    }

    for (String filename : Splitter.on(':').split(libPaths)) {

      final Path file = Path.of(filename.trim());
      if (Files.exists(file) && Files.isRegularFile(file)) {

        if (jarName.equals(file.getFileName().toString())) {
          this.srcJar = file;
        } else {
          this.jarFiles.add(file);
        }
      }
    }
  }

}
