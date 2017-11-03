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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class define a translator that add commons links information.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class CommonLinksInfoTranslator extends AbstractTranslator {

  private final Translator translator;
  private final Map<String, String> mapLinks = new HashMap<>();

  /**
   * Get an ordered list of the translator fields
   * @return an ordered list of the translator fields.
   */
  @Override
  public List<String> getFields() {

    return this.translator.getFields();
  }

  /**
   * Get a translation for a feature
   * @param id Identifier of the feature
   * @param field the field to get
   * @return An array with the annotation of the Feature
   */
  @Override
  public String translateField(final String id, final String field) {

    return this.translator.translateField(id, field);
  }

  /**
   * Test if the link information is available for the field
   * @param field Field to test
   * @return true if link information is available
   */
  @Override
  public boolean isLinkInfo(final String field) {

    return mapLinks.containsKey(field);
  }

  /**
   * Get link information.
   * @param translatedId Translated id
   * @param field field of the id
   * @return a link for the translated id
   */
  @Override
  public String getLinkInfo(final String translatedId, final String field) {

    if (translatedId == null || field == null) {
      throw new NullPointerException(
          "field and translateId arguments can't be null.");
    }

    if (mapLinks.containsKey(field)) {
      String EncodedTranslatedId = translatedId;
      try {
        EncodedTranslatedId =
            URLEncoder.encode(translatedId, StandardCharsets.UTF_8.toString());
      } catch (UnsupportedEncodingException e) {
        return null;
      }

      return mapLinks.get(field).replace("${ID}", EncodedTranslatedId);
    }

    return null;

  }

  /**
   * Get the available identifiers by the translator if possible.
   * @return a array of string with the identifiers
   */
  @Override
  public List<String> getIds() {

    return this.translator.getIds();
  }

  /**
   * Add a new link to a field
   * @param field to link
   * @param link for the field
   */
  public void add(final String field, final String link) {

    if (link == null || field == null) {
      throw new NullPointerException("field and link arguments can't be null.");
    }
    this.mapLinks.put(field, link);
  }

  /**
   * Remove the link of a field
   * @param field to remove
   */
  public void remove(final String field) {

    if (field == null) {
      throw new NullPointerException("field argument can't be null.");
    }
    this.mapLinks.remove(field);
  }

  /**
   * Clear the links of all fields.
   */
  public void clear() {
    this.mapLinks.clear();
  }

  /**
   * Load the field and links from a file
   * @param in File to load
   */
  public void load(final File in) throws IOException {

    if (in == null) {
      throw new NullPointerException("file argument can't be null.");
    }
    load(new FileInputStream(in));

  }

  /**
   * Load the field and links from a file
   * @param in File to load
   */
  public void load(final InputStream in) throws IOException {

    if (in == null) {
      throw new NullPointerException("in argument can't be null.");
    }
    load(new InputStreamReader(in));
  }

  /**
   * Load the field and links from a file
   * @param in File to load
   */
  public void load(final Reader in) throws IOException {

    if (in == null) {
      throw new NullPointerException("in argument can't be null.");
    }
    BufferedReader reader = new BufferedReader(in);

    // Iterate through properties file; An empty link is used to remove an entry
    // in map link instead of ignoring it.
    String line = "";
    while ((line = reader.readLine()) != null) {
      int pos = line.indexOf('=');

      if (pos != -1) {
        String field = line.substring(0, pos).trim();
        String link = line.substring(pos + 1).trim();
        if (link.isEmpty()) {
          remove(field);
        } else {
          add(field, link);
        }
      }
    }

  }
  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param translator Translator to use
   */
  public CommonLinksInfoTranslator(final Translator translator) {

    if (translator == null) {
      throw new NullPointerException("Translator can't be null");
    }

    try {
      load(this.getClass()
          .getResourceAsStream("/META-INF/commonlinks.properties"));
    } catch (IOException e) {
      // Do nothing.
    }
    this.translator = translator;
  }
}
