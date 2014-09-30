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
 * @author Laurent Jourdren
 */
public class ChangeIndexTranslator extends BasicTranslator {

  private Translator translator;
  private String field;
  private String[] fields;

  private Map<String, String> index = new HashMap<String, String>();

  /**
   * Get link information.
   * @param translatedId Translated id
   * @param field field of the id
   * @return a link for the translated id
   */
  public String getLinkInfo(final String translatedId, final String field) {

    return this.translator.getLinkInfo(translatedId, field);
  }

  /**
   * Test if the link information is available for the field
   * @param field Field to test
   * @return true if link information is available
   */
  public boolean isLinkInfo(final String field) {

    return this.translator.isLinkInfo(field);
  }

  /**
   * Get an ordered list of the translator fields
   * @return an ordered list of the translator fields.
   */
  public String[] getFields() {

    return fields;
  }

  /**
   * Get a translation for a feature
   * @param id Identifier of the feature
   * @param field the field to get
   * @return An array with the annotation of the Feature
   */
  public String translateField(final String id, final String field) {

    return this.translator.translateField(this.index.get(id), field);
  }

  private void makeIndex() {

    final String[] ids = this.translator.getIds();

    if (ids == null)
      return;

    for (int i = 0; i < ids.length; i++) {

      String id = ids[i];
      String t = this.translator.translateField(id);

      if (t != null && !this.index.containsKey(t))
        this.index.put(t, id);
    }

    String[] fs = this.translator.getFields();

    this.fields = new String[fs.length - 1];

    int count = 0;

    for (int i = 0; i < fs.length; i++)
      if (!this.field.equals(fs[i]))
        this.fields[count++] = fs[i];

  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param translator Translator to use
   * @param field Field to use for the new index
   */
  public ChangeIndexTranslator(final Translator translator, final String field) {

    if (translator == null)
      throw new NullPointerException("Translator is null");

    if (field == null)
      throw new NullPointerException("The field is null");

    if (!translator.isField(field))
      throw new NullPointerException("The field "
          + field + " doesn't exist in the translator");

    this.translator = translator;
    this.field = field;

    makeIndex();
  }

}
