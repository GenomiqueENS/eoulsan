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


/**
 * This class define a translator with only few of field of another translator.
 * @author Laurent Jourdren
 */
public class SelectAnnotationFieldsTranslator extends BasicTranslator {

  private Translator translator;
  private String[] fields;

  /**
   * Get an ordered list of the translator fields
   * @return an ordered list of the translator fields.
   */
  public String[] getFields() {

    return this.fields;
  }

  /**
   * Get a translation for a feature
   * @param id Identifier of the feature
   * @param field the field to get
   * @return An array with the annotation of the Feature
   */
  public String translateField(final String id, final String field) {

    return this.translator.translateField(id, field);
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
   * Get link information.
   * @param translatedId Translated id
   * @param field field of the id
   * @return a link for the translated id
   */
  public String getLinkInfo(final String translatedId, final String field) {

    return this.translator.getLinkInfo(translatedId, field);
  }

  /**
   * Get the available identifiers by the translator if possible.
   * @return a array of string with the identifiers
   */
  public String[] getIds() {

    return this.translator.getIds();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param fields fields to set for the translator
   */
  public SelectAnnotationFieldsTranslator(final String[] fields) {

    if (fields == null)
      throw new NullPointerException("Fields can't be null");

    this.fields = fields;
  }

}
