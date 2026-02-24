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

import static fr.ens.biologie.genomique.kenetre.translator.TranslatorUtils.createDuplicatedEnsemblIdTranslator;
import static java.util.Objects.requireNonNull;

import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.kenetre.bio.AnnotationMatrix;
import fr.ens.biologie.genomique.kenetre.bio.io.TSVAnnotationMatrixReader;
import fr.ens.biologie.genomique.kenetre.translator.AnnotationMatrixTranslator;
import fr.ens.biologie.genomique.kenetre.translator.CommonLinksInfoTranslator;
import fr.ens.biologie.genomique.kenetre.translator.ConcatTranslator;
import fr.ens.biologie.genomique.kenetre.translator.Translator;

/**
 * This class define Kenetre translator utility glue methods for Eoulsan.
 * @since 2.6
 * @author Laurent Jourdren
 */
public class EoulsanTranslatorUtils {

  /**
   * Create a translator annotation from an additional annotation file and a
   * link file.
   * @param annotationFile the annotation file to use
   * @param linksFile the additional annotation hypertext links file
   * @return a Translator object with the additional annotation
   * @throws IOException if an error occurs while reading additional annotation
   */
  public static Translator loadTranslator(final DataFile annotationFile,
      final DataFile linksFile) throws IOException {

    requireNonNull(annotationFile, "annotationFile argument cannot be null");

    final Translator did = createDuplicatedEnsemblIdTranslator();

    AnnotationMatrix matrix;
    try (TSVAnnotationMatrixReader reader =
        new TSVAnnotationMatrixReader(annotationFile.open())) {
      matrix = reader.read();
    }

    final CommonLinksInfoTranslator translator = new CommonLinksInfoTranslator(
        new ConcatTranslator(did, new AnnotationMatrixTranslator(matrix)));

    // Load hypertext links
    updateLinks(translator, linksFile);

    return translator;
  }

  /**
   * Create a translator annotation from a link file.
   * @param linksFile the additional annotation hypertext links file
   * @return a Translator object with the additional annotation
   * @throws IOException if an error occurs while reading additional annotation
   */
  public static Translator loadTranslator(final DataFile linksFile)
      throws IOException {

    final CommonLinksInfoTranslator translator =
        new CommonLinksInfoTranslator(createDuplicatedEnsemblIdTranslator());

    // Load hypertext links
    updateLinks(translator, linksFile);

    return translator;
  }

  /**
   * Get the links file from the settings.
   * @param settings the settings object
   * @return a DataFile with the path to the link file or null if the link file
   *         has not been defined in the settings
   */
  public static DataFile getLinksFileFromSettings(final Settings settings) {

    if (settings == null) {
      throw new NullPointerException("settings argument cannot be null");
    }

    final String value = settings.getAdditionalAnnotationHypertextLinksPath();

    if (value == null) {
      return null;
    }

    return new DataFile(value);
  }

  /**
   * Update the links of a CommonLinksInfoTranslator from the content of a file.
   * @param translator the translator to update
   * @param linksFile the links file that can be null
   * @throws IOException if an error occurs while reading the link file
   */
  private static void updateLinks(CommonLinksInfoTranslator translator,
      final DataFile linksFile) throws IOException {

    // Load annotation hypertext links
    if (linksFile != null) {

      // Check if the file exists
      if (!linksFile.exists()) {
        throw new IOException(linksFile + " doesn't exists.");
      }

      // Load the file
      translator.load(linksFile.open());
    }
  }

  //
  // Constructor
  //

  private EoulsanTranslatorUtils() {

    throw new IllegalStateException();
  }

}
