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
 * This class define a translator that add the identifier to translations.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class AddIdentifierTranslator extends BasicTranslator {

  private static final String DEFAULT_FIELD = "OriginalId";

  private String[] fields;
  private String newFieldName = DEFAULT_FIELD;

  private final Translator translator;

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

    if (this.newFieldName.equals(field)) {
      return id;
    }

    return this.translator.translateField(id, field);
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

  /**
   * Update the fields from the input translator.
   */
  public void updateFields() {

    String[] tFields = this.translator.getFields();

    if (tFields == null) {
      this.fields = new String[] { this.newFieldName };
    } else {

      this.fields = new String[tFields.length + 1];
      this.fields[0] = this.newFieldName;
      System.arraycopy(tFields, 0, this.fields, 1, tFields.length);
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
   * Get the available identifiers by the translator if possible.
   * @return a array of string with the identifiers
   */
  @Override
  public String[] getIds() {

    return this.translator.getIds();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param translator Translator to use
   */
  public AddIdentifierTranslator(final Translator translator) {

    this(translator, null);
  }

  /**
   * Public constructor.
   * @param translator Translator to use
   * @param newFieldName the name of new field
   */
  public AddIdentifierTranslator(final Translator translator,
      final String newFieldName) {

    if (translator == null) {
      throw new NullPointerException("Translator can't be null");
    }

    this.translator = translator;

    setNewFieldName(newFieldName);
    updateFields();
  }

}
