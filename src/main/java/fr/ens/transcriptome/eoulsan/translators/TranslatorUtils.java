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

package fr.ens.transcriptome.eoulsan.translators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.translators.io.TranslatorOutputFormat;

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
      int fieldToTranslate, final Translator translator,
      final TranslatorOutputFormat of) throws FileNotFoundException,
      IOException {

    addTranslatorFields(new FileInputStream(inputFile), fieldToTranslate,
        translator, of);
  }

  public static void addTranslatorFields(final InputStream is,
      int fieldToTranslate, final Translator translator,
      final TranslatorOutputFormat of) throws IOException {

    if (is == null)
      throw new NullPointerException("InputStream is null");
    if (translator == null)
      throw new NullPointerException("Translator is null");
    if (of == null)
      throw new NullPointerException("OutputFormat is null");

    String[] translatorFieldnames = translator.getFields();
    final int n = translatorFieldnames.length;

    final BufferedReader reader =
        new BufferedReader(new InputStreamReader(is, Globals.DEFAULT_CHARSET));
    String line;
    boolean first = true;

    while ((line = reader.readLine()) != null) {

      final String[] fields = line.split("\t");

      if (first) {

        // Write original file header
        for (int i = 0; i < fields.length; i++)
          of.addHeaderField(fields[i]);

        // Write original file header
        for (int i = 0; i < n; i++)
          of.addHeaderField(translatorFieldnames[i]);

        first = false;
      } else {

        of.newLine();

        // Write orignal file data
        for (int i = 0; i < fields.length; i++)
          try {
            of.writeDouble(Double.parseDouble(fields[i]));
          } catch (NumberFormatException e) {

            of.writeText(fields[i]);
          }

        // Write annotation
        for (int i = 0; i < n; i++) {

          final String field = translatorFieldnames[i];
          final String valueToTranslate = fields[fieldToTranslate];
          final String value;

          if (field == null)
            value = null;
          else
            value = translator.translateField(valueToTranslate, field);

          String link;

          if (value == null || !translator.isLinkInfo(field))
            link = null;
          else
            link = translator.getLinkInfo(value, field);

          if (value == null)
            of.writeEmpty();
          else {

            if (link == null)
              of.writeText(value);
            else
              of.writeLink(value, link);

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
