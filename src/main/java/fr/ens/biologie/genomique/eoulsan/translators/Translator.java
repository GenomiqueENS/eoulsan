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

import java.util.List;

/**
 * This interface define how retrieve annotation for a feature.
 * @since 2.0
 * @author Laurent Jourdren
 */
public interface Translator {

  /**
   * Get the default field.
   * @return default field
   */
  String getDefaultField();

  /**
   * Set the default field.
   * @param field The field to set
   */
  void setDefaultField(String field);

  /**
   * Get an ordered list of the translator fields
   * @return an ordered list of the translator fields.
   */
  List<String> getFields();

  /**
   * Get all the translation for a feature
   * @param id Identifier of the feature
   * @return An array with the annotation of the Feature
   */
  List<String> translate(String id);

  /**
   * Get a translation for a feature
   * @param id Identifier of the feature
   * @param field the field to get
   * @return An array with the annotation of the Feature
   */
  String translateField(String id, String field);

  /**
   * Get a translation for a feature. The field is the default field.
   * @param id Identifier of the feature
   * @return An array with the annotation of the Feature
   */
  String translateField(String id);

  /**
   * Get all the translations for features
   * @param ids Identifiers of the features
   * @return An array with the annotation of the Feature
   */
  List<List<String>> translate(List<String> ids);

  /**
   * Get translations for features
   * @param ids Identifiers of the features
   * @param field the field to get
   * @return An array with the annotation of the Feature
   */
  List<String> translateField(List<String> ids, String field);

  /**
   * Get translations for features. The field is the default field.
   * @param ids Identifiers of the features
   * @return An array with the annotation of the Feature
   */
  List<String> translateField(List<String> ids);

  /**
   * Test if the link information is available for the field
   * @param field Field to test
   * @return true if link information is available
   */
  boolean isLinkInfo(String field);

  /**
   * Get link information.
   * @param translatedId Translated id
   * @param field field of the id
   * @return a link for the translated id
   */
  String getLinkInfo(String translatedId, String field);

  /**
   * Get links information.
   * @param translatedIds Translated ids
   * @param field field of the id
   * @return a array of links for the translated ids
   */
  List<String> getLinkInfo(List<String> translatedIds, String field);

  /**
   * Test if the field exists.
   * @param field Field to test
   * @return true if the field exists
   */
  boolean isField(String field);

  /**
   * Get the reverse translator for this translator.
   * @return a reverse translator
   */
  Translator getReverseTranslator();

  /**
   * Get the available identifiers by the translator if possible.
   * @return a array of string with the identifiers
   */
  List<String> getIds();

}
