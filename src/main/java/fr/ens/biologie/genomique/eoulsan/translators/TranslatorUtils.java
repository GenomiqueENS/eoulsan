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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.translators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.Globals;
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
      final TranslatorOutputFormat of)
          throws FileNotFoundException, IOException {

    addTranslatorFields(new FileInputStream(inputFile), fieldToTranslate,
        translator, of);
  }

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

  //
  // Constructor
  //

  private TranslatorUtils() {
  }

}
