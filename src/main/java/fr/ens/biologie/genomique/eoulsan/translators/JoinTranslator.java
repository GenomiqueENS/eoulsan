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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class define a translator that concat two translator. To use the field
 * of the second translator, id must be translated with the first translator.
 * translator and a array of identifiers.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class JoinTranslator extends AbstractTranslator {

  private final Translator translator1;
  private final Translator translator2;
  private String joinField;
  private final List<String> fields;
  private final Map<String, Translator> mapTranslator = new HashMap<>();
  private final boolean returnTranslation1IfNoTranslation;

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

    final Translator t = this.mapTranslator.get(field);

    if (t == null) {
      throw new NullPointerException("Associated Translator is null.");
    }
    if (t == this.translator1) {
      return this.translator1.translateField(id, field);
    }

    final String result1 = this.translator1.translateField(id, this.joinField);

    final String result = this.translator2.translateField(result1, field);

    if (result == null && this.returnTranslation1IfNoTranslation) {
      return result1;
    }

    return result;
  }

  /**
   * Test if the link information is available for the field
   * @param field Field to test
   * @return true if link information is available
   */
  @Override
  public boolean isLinkInfo(final String field) {

    final Translator t = this.mapTranslator.get(field);

    if (t == null) {
      return false;
    }
    if (t == this.translator1) {
      return this.translator1.isLinkInfo(field);
    }

    return this.translator2.isLinkInfo(field);
  }

  /**
   * Get link information.
   * @param translatedId Translated id
   * @param field field of the id
   * @return a link for the translated id
   */
  @Override
  public String getLinkInfo(final String translatedId, final String field) {

    final Translator t = this.mapTranslator.get(field);

    if (t == null) {
      return null;
    }

    if (t == this.translator1) {
      return this.translator1.getLinkInfo(translatedId, field);
    }

    return this.translator2.getLinkInfo(translatedId, field);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param translator1 First translator
   * @param translator2 Second translator
   * @param joinField The field of the join
   */
  public JoinTranslator(final Translator translator1, final String joinField,
      final Translator translator2) {

    this(translator1, joinField, translator2, false);
  }

  /**
   * Public constructor.
   * @param translator1 First translator
   * @param translator2 Second translator
   * @param joinField The field of the join
   * @param returnTranslation1IfNoTranslation true if the result must the result
   *          of translator1 if there is no result for translator2
   */
  public JoinTranslator(final Translator translator1, final String joinField,
      final Translator translator2,
      final boolean returnTranslation1IfNoTranslation) {

    if (translator1 == null) {
      throw new NullPointerException("Translator1  can't be null");
    }
    if (translator2 == null) {
      throw new NullPointerException("Translator1  can't be null");
    }
    if (joinField == null) {
      throw new NullPointerException("Join field  can't be null");
    }
    if (!translator1.isField(joinField)) {
      throw new NullPointerException("The join field isn't in translator 1");
    }

    this.translator1 = translator1;
    this.translator2 = translator2;
    this.returnTranslation1IfNoTranslation = returnTranslation1IfNoTranslation;

    final ArrayList<String> fieldList = new ArrayList<>();

    for (final String f : translator1.getFields()) {
      this.mapTranslator.put(f, translator1);
      fieldList.add(f);
    }

    for (final String f : translator2.getFields()) {
      if (!this.mapTranslator.containsKey(f)) {
        this.mapTranslator.put(f, translator2);
        fieldList.add(f);
      }
    }

    this.fields = Collections.unmodifiableList(fieldList);

  }

}
