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

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;

/**
 * This class implements a translator for multicolumn annotation. The first
 * column is the identifier.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class MultiColumnTranslator extends AbstractTranslator {

  private final Map<String, Map<String, String>> annotations = new HashMap<>();
  private List<String> fieldNames;

  /**
   * Add data to the translator. The first value of the array data is the unique
   * id for the translator.
   * @param rowData data to add
   */
  public void addRow(final List<String> rowData) {

    if (rowData == null || rowData.size() == 0 || rowData.size() == 1) {
      return;
    }

    final List<String> dataArray = arrayWithoutFirstElement(rowData);

    addRow(rowData.get(0), dataArray);
  }

  /**
   * Add data to the translator. The first value of the array data is the unique
   * id for the translator.
   * @param rowData data to add
   */
  public void addRow(final String... rowData) {

    if (rowData == null) {
      return;
    }

    addRow(Arrays.asList(rowData));
  }

  /**
   * Add data to the translator.
   * @param id id for the translator.
   * @param rowData data to add
   */
  public void addRow(final String id, final List<String> rowData) {

    if (id == null || rowData == null) {
      return;
    }

    Map<String, String> dataMap = new HashMap<>();

    final int sizeData = rowData.size();
    final int sizeFields = this.fieldNames.size();

    final int size = Math.min(sizeData, sizeFields);

    for (int i = 0; i < size; i++) {
      dataMap.put(this.fieldNames.get(i), rowData.get(i));
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
  public List<String> getFields() {

    if (this.fieldNames == null) {
      return null;
    }
    return Collections.unmodifiableList(this.fieldNames);

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

  private List<String> arrayWithoutFirstElement(final List<String> data) {

    if (data == null) {
      return null;
    }

    // data.remove(0);
    // final int size = data.size();
    // String[] result = new String[size - 1];
    // ArrayList<String> result = new ArrayList<>();
    return Collections.unmodifiableList(data.subList(1, data.size()));
    // System.arraycopy(data, 1, result, 0, size - 1);
    // return result;
  }

  /**
   * Get the available identifiers by the translator if possible.
   * @return a array of string with the identifiers
   */
  @Override
  public List<String> getIds() {

    return new ArrayList<>(this.annotations.keySet());
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param fieldNames Field names of the annotation
   */
  public MultiColumnTranslator(final List<String> fieldNames) {

    this(fieldNames, true);
  }

  /**
   * Public constructor.
   * @param fieldNames Field names of the annotation
   * @param fieldNamesWithId false if the first element of the fieldname array
   *          is the key for the translator (must be ignored)
   */
  public MultiColumnTranslator(final List<String> fieldNames,
      final boolean fieldNamesWithId) {

    if (fieldNames == null) {
      throw new NullPointerException("fieldnames is null");
    }

    if (fieldNamesWithId && fieldNames.size() < 2) {
      throw new EoulsanRuntimeException(
          "fieldNames must have at least 2 fields");
    }

    if (!fieldNamesWithId && fieldNames.size() < 1) {
      throw new EoulsanRuntimeException(
          "fieldNames must have at least one fields");
    }

    if (fieldNamesWithId) {
      this.fieldNames = arrayWithoutFirstElement(fieldNames);
      setDefaultField(fieldNames.get(1));
    } else {
      this.fieldNames = fieldNames;
      setDefaultField(fieldNames.get(0));
    }
  }

  public MultiColumnTranslator(String... fieldNames) {

    this(Arrays.asList(fieldNames));
  }

}
