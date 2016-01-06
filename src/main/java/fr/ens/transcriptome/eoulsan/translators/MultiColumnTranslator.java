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

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;

/**
 * This class implements a translator for multicolumn annotation. The first
 * column is the identifier.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class MultiColumnTranslator extends AbstractTranslator {

  private final Map<String, Map<String, String>> annotations = new HashMap<>();
  private String[] fieldNames;

  /**
   * Add data to the translator. The first value of the array data is the unique
   * id for the translator.
   * @param rowData data to add
   */
  public void addRow(final String[] rowData) {

    if (rowData == null || rowData.length == 0 || rowData.length == 1) {
      return;
    }

    final String[] dataArray = arrayWithoutFirstElement(rowData);

    addRow(rowData[0], dataArray);
  }

  /**
   * Add data to the translator.
   * @param id id for the translator.
   * @param rowData data to add
   */
  public void addRow(final String id, final String[] rowData) {

    if (id == null || rowData == null) {
      return;
    }

    Map<String, String> dataMap = new HashMap<>();

    final int sizeData = rowData.length;
    final int sizeFields = this.fieldNames.length;

    final int size = Math.min(sizeData, sizeFields);

    for (int i = 0; i < size; i++) {
      dataMap.put(this.fieldNames[i], rowData[i]);
    }

    this.annotations.put(id, dataMap);
  }

  //
  // Method for the Translator
  //

  /**
   * Get an ordered list of the annotations fields
   * @return an ordered list of the annotations fields.
   */
  @Override
  public String[] getFields() {

    if (this.fieldNames == null) {
      return null;
    }

    final String[] result = this.fieldNames.clone();

    return result;
  }

  /**
   * Get an annotation for an feature
   * @param id Identifier of the feature
   * @param fieldName Field to get
   * @return A String with the request annotation of the Feature
   */
  @Override
  public String translateField(final String id, final String fieldName) {

    if (id == null) {
      return null;
    }

    final String field;

    if (fieldName == null) {
      field = getDefaultField();
    } else {
      field = fieldName;
    }

    final Map<String, String> map = this.annotations.get(id);
    if (map == null) {
      return null;
    }

    return map.get(field);
  }

  /**
   * Clear the descriptions of the features.
   */
  public void clear() {

    this.annotations.clear();
  }

  private String[] arrayWithoutFirstElement(final String[] data) {

    if (data == null) {
      return null;
    }

    final int size = data.length;

    String[] result = new String[size - 1];

    System.arraycopy(data, 1, result, 0, size - 1);

    return result;
  }

  /**
   * Get the available identifiers by the translator if possible.
   * @return a array of string with the identifiers
   */
  @Override
  public String[] getIds() {

    return this.annotations.keySet()
        .toArray(new String[this.annotations.size()]);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param fieldNames Field names of the annotation
   */
  public MultiColumnTranslator(final String[] fieldNames) {

    this(fieldNames, true);
  }

  /**
   * Public constructor.
   * @param fieldNames Field names of the annotation
   * @param fieldNamesWithId false if the first element of the fieldname array
   *          is the key for the translator (must be ignored)
   */
  public MultiColumnTranslator(final String[] fieldNames,
      final boolean fieldNamesWithId) {

    if (fieldNames == null) {
      throw new NullPointerException("fieldnames is null");
    }

    if (fieldNamesWithId && fieldNames.length < 2) {
      throw new EoulsanRuntimeException(
          "fieldNames must have at least 2 fields");
    }

    if (!fieldNamesWithId && fieldNames.length < 1) {
      throw new EoulsanRuntimeException(
          "fieldNames must have at least one fields");
    }

    if (fieldNamesWithId) {
      this.fieldNames = arrayWithoutFirstElement(fieldNames);
      setDefaultField(fieldNames[1]);
    } else {
      this.fieldNames = fieldNames;
      setDefaultField(fieldNames[0]);
    }
  }

}
