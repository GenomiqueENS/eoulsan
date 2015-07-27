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

import java.util.HashMap;
import java.util.Map;

/**
 * This class define a translator that call another translator but with a
 * different index field.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class ChangeIndexTranslator extends BasicTranslator {

  private final Translator translator;
  private final String field;
  private String[] fields;

  private final Map<String, String> index = new HashMap<>();

  /**
   * Get link information.
   * @param translatedId Translated id
   * @param field field of the id
   * @return a link for the translated id
   */
  @Override
  public String getLinkInfo(final String translatedId, final String field) {

    return this.translator.getLinkInfo(translatedId, field);
  }

  /**
   * Test if the link information is available for the field
   * @param field Field to test
   * @return true if link information is available
   */
  @Override
  public boolean isLinkInfo(final String field) {

    return this.translator.isLinkInfo(field);
  }

  /**
   * Get an ordered list of the translator fields
   * @return an ordered list of the translator fields.
   */
  @Override
  public String[] getFields() {

    return this.fields;
  }

  /**
   * Get a translation for a feature
   * @param id Identifier of the feature
   * @param field the field to get
   * @return An array with the annotation of the Feature
   */
  @Override
  public String translateField(final String id, final String field) {

    return this.translator.translateField(this.index.get(id), field);
  }

  private void makeIndex() {

    final String[] ids = this.translator.getIds();

    if (ids == null) {
      return;
    }

    for (String id : ids) {

      String t = this.translator.translateField(id);

      if (t != null && !this.index.containsKey(t)) {
        this.index.put(t, id);
      }
    }

    String[] fs = this.translator.getFields();

    this.fields = new String[fs.length - 1];

    int count = 0;

    for (String f : fs) {
      if (!this.field.equals(f)) {
        this.fields[count++] = f;
      }
    }

  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param translator Translator to use
   * @param field Field to use for the new index
   */
  public ChangeIndexTranslator(final Translator translator,
      final String field) {

    if (translator == null) {
      throw new NullPointerException("Translator is null");
    }

    if (field == null) {
      throw new NullPointerException("The field is null");
    }

    if (!translator.isField(field)) {
      throw new NullPointerException(
          "The field " + field + " doesn't exist in the translator");
    }

    this.translator = translator;
    this.field = field;

    makeIndex();
  }

}
