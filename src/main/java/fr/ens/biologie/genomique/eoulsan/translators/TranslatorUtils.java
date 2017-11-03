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

package fr.ens.biologie.genomique.eoulsan.translators;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.translators.io.MultiColumnTranslatorReader;
import fr.ens.biologie.genomique.eoulsan.translators.io.TranslatorOutputFormat;

/**
 * This class implements utility methods for translators.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class TranslatorUtils {

  /**
   * Create a file with additional annotation.
   * @param inputFile input file
   * @param fieldToTranslate field to use with translator
   * @param translator translator to use
   * @throws FileNotFoundException if a file cannot be found
   * @throws IOException if an error occurs while creating the output file
   */
  public static void addTranslatorFields(final File inputFile,
      final int fieldToTranslate, final Translator translator,
      final TranslatorOutputFormat of) throws IOException {

    addTranslatorFields(new FileInputStream(inputFile), fieldToTranslate,
        translator, of);
  }

  /**
   * Create a file with additional annotation.
   * @param is input stream of the file
   * @param fieldToTranslate field to use with translator
   * @param translator translator to use
   * @throws IOException if an error occurs while creating the output file
   */
  public static void addTranslatorFields(final InputStream is,
      final int fieldToTranslate, final Translator translator,
      final TranslatorOutputFormat of) throws IOException {

    if (is == null) {
      throw new NullPointerException("InputStream is null");
    }
    if (translator == null) {
      throw new NullPointerException("Translator is null");
    }
    if (of == null) {
      throw new NullPointerException("OutputFormat is null");
    }

    List<String> translatorFieldnames = translator.getFields();

    final BufferedReader reader =
        new BufferedReader(new InputStreamReader(is, Globals.DEFAULT_CHARSET));
    String line;
    boolean first = true;

    while ((line = reader.readLine()) != null) {

      final List<String> fields =
          new ArrayList<>(Arrays.asList(line.split("\t")));

      if (first) {

        // Write original file header
        for (String field : fields) {
          of.addHeaderField(field);
        }

        // Write original file header
        for (String translatorFieldname : translatorFieldnames) {
          of.addHeaderField(translatorFieldname);
        }

        first = false;
      } else {

        of.newLine();

        // Write original file data
        for (String field1 : fields) {
          try {
            of.writeDouble(Double.parseDouble(field1));
          } catch (NumberFormatException e) {

            of.writeText(field1);
          }
        }

        // Write annotation
        for (final String field : translatorFieldnames) {

          final String valueToTranslate = fields.get(fieldToTranslate);

          final String value;

          if (field == null) {
            value = null;
          } else {
            value = translator.translateField(valueToTranslate, field);
          }

          String link;

          if (value == null || !translator.isLinkInfo(field)) {
            link = null;
          } else {
            link = translator.getLinkInfo(value, field);
          }

          if (value == null) {
            of.writeEmpty();
          } else {

            if (link == null) {
              of.writeText(value);
            } else {
              of.writeLink(value, link);
            }

          }
        }
      }

    }

    reader.close();
    of.close();
  }

  /**
   * Create a translator that contains one column named "EnsemblID" that
   * duplicate the Id if the the is an EnsemblID
   * @return a new translator
   */
  private static Translator createDuplicatedEnsemblIdTranslator() {

    return new AbstractTranslator() {

      private static final String FIELD_NAME = "EnsemblID";
      private final Pattern pattern = Pattern.compile("ENS[A-Z]+[0-9]{11}");

      @Override
      public String translateField(final String id, final String field) {

        if (id == null || field == null) {
          return null;
        }

        if (FIELD_NAME.equals(field) && pattern.matcher(id).matches()) {
          return id;
        }

        return null;
      }

      @Override
      public List<String> getFields() {

        return Collections.singletonList(FIELD_NAME);
      }
    };

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

    checkNotNull(annotationFile, "annotationFile argument cannot be null");

    final Translator did = createDuplicatedEnsemblIdTranslator();

    final CommonLinksInfoTranslator translator =
        new CommonLinksInfoTranslator(new ConcatTranslator(did,
            new MultiColumnTranslatorReader(annotationFile.open()).read()));

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

  //
  // Constructor
  //

  private TranslatorUtils() {
  }

}
