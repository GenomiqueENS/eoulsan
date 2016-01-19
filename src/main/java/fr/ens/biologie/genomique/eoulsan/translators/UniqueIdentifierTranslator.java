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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class define a translator with create unique identifier from another
 * translator and a array of identifiers.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class UniqueIdentifierTranslator extends AbstractTranslator {

  private static final String DEFAULT_FIELD = "UniqueId";

  private final Map<String, String> mapUniqueId = new HashMap<>();
  private final Map<String, String> reverseMapUniqueId = new HashMap<>();
  private List<String> fields;
  private String newFieldName = DEFAULT_FIELD;

  private final Translator translator;

  /**
   * Get an ordered list of the translator fields
   * @return an ordered list of the translator fields.
   */
  @Override
  public List<String> getFields() {

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

    final String translatedId = this.mapUniqueId.get(id);

    if (this.newFieldName.equals(field)) {
      return translatedId;
    }

    return this.translator.translateField(translatedId, field);
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
   * Get link information.
   * @param translatedId Translated id
   * @param field field of the id
   * @return a link for the translated id
   */
  @Override
  public String getLinkInfo(final String translatedId, final String field) {

    return this.translator.getLinkInfo(translatedId, field);
  }

  //
  // Other methods
  //

  private void translateIds(final List<String> ids, final String field) {

    final Translator translator = this.translator;

    Map<String, String> translation = new HashMap<>();
    Map<String, Integer> translationCount = new HashMap<>();

    final String fieldName =
        field == null ? translator.getDefaultField() : field;

    for (String row : ids) {

      String t = translator.translateField(row, fieldName);

      if (t == null || "".equals(t)) {
        t = row;
      }

      translation.put(row, t);
      if (translationCount.containsKey(t)) {
        int count = translationCount.get(t);
        translationCount.put(t, ++count);

      } else {
        translationCount.put(t, 1);
      }

    }

    Map<String, Integer> translationCurrentCount = new HashMap<>();

    // for (String row : translation.keySet()) {
    for (Map.Entry<String, String> e : translation.entrySet()) {

      // String t = translation.get(row);
      final String row = e.getKey();
      final String t = e.getValue();
      // System.out.println(row+"\t"+t);
      int count = translationCount.get(t);

      if (count > 1) {

        String postfix;

        if (translationCurrentCount.containsKey(t)) {

          int currentCount = translationCurrentCount.get(t);
          currentCount++;
          translationCurrentCount.put(t, currentCount);
          postfix = "#" + currentCount;
        } else {
          translationCurrentCount.put(t, 1);
          postfix = "#1";
        }

        translation.put(row, t + postfix);
      }

    }

    for (final String id : ids) {

      final String newId = translation.get(id);

      // System.out.println(id + "\t" + newId);

      this.mapUniqueId.put(newId, id);
      this.reverseMapUniqueId.put(id, newId);

    }

  }

  /**
   * Update the fields from the input translator.
   */
  public void updateFields() {

    List<String> tFields = this.translator.getFields();

    if (tFields == null) {
      this.fields = Collections.singletonList(this.newFieldName);
    } else {

      // this.fields = new String[tFields.length + 1];
      this.fields = new ArrayList<>();
      this.fields.add(this.newFieldName);
      // System.arraycopy(tFields, 0, this.fields, 1, tFields.size());
      this.fields.addAll(tFields);
    }

  }

  /**
   * Set the name of the new field of the translator.
   * @param newFieldName the name of new field
   */
  public void setNewFieldName(final String newFieldName) {

    if (newFieldName == null) {
      this.newFieldName = DEFAULT_FIELD;
    } else {
      this.newFieldName = newFieldName;
    }
  }

  /**
   * Get the reverse translator for this translator.
   * @return a reverse translator
   */
  @Override
  public Translator getReverseTranslator() {

    return new AbstractTranslator() {

      @Override
      public List<String> getFields() {

        return Collections
            .singletonList(UniqueIdentifierTranslator.this.newFieldName);
      }

      @Override
      public String translateField(final String id, final String field) {
        if (UniqueIdentifierTranslator.this.newFieldName.equals(field)) {
          return UniqueIdentifierTranslator.this.reverseMapUniqueId.get(id);
        }

        return null;
      }

    };

  }

  /**
   * Get the default field of a translator.
   * @param translator a translator
   * @return the default field or null if the translator is null or if there is
   *         no default field for the translator
   */
  private static final String getTranslatorDefaultField(
      final Translator translator) {

    if (translator == null) {
      throw new NullPointerException("Translator argument can't is null.");
    }

    return translator.getDefaultField();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param ids Identifier to set unique
   * @param translator Translator to use
   */
  public UniqueIdentifierTranslator(final List<String> ids,
      final Translator translator) {

    this(ids, translator, getTranslatorDefaultField(translator), null);
  }

  /**
   * Public constructor.
   * @param ids Identifier to set unique
   * @param translator Translator to use
   * @param translatorField field of the translator to use
   */
  public UniqueIdentifierTranslator(final List<String> ids,
      final Translator translator, final String translatorField) {

    this(ids, translator, translatorField, null);
  }

  /**
   * Public constructor.
   * @param ids Identifier to set unique
   * @param translator Translator to use
   * @param translatorField field of the translator to use
   * @param newFieldName the name of new field
   */
  public UniqueIdentifierTranslator(final List<String> ids,
      final Translator translator, final String translatorField,
      final String newFieldName) {

    if (ids == null) {
      throw new NullPointerException("Identifiers can't be null");
    }

    if (translator == null) {
      throw new NullPointerException("Translator can't be null");
    }

    this.translator = translator;

    translateIds(Collections.unmodifiableList(ids), translatorField);

    setNewFieldName(newFieldName);
    updateFields();
  }

  public UniqueIdentifierTranslator(String[] ids, final Translator translator) {

    this(Arrays.asList(ids), translator);
  }

}
