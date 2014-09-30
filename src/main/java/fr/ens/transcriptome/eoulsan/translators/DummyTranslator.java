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
 * This translator is only for tests.
 * @author Laurent Jourdren
 */
public class DummyTranslator extends BasicTranslator {

  private int fieldCount;

  public String[] getFields() {

    final String array[] = new String[this.fieldCount];

    for (int i = 0; i < array.length; i++)
      array[i] = "field#" + i;

    return array;
  }

  public String translateField(String id, String field) {

    if (id == null || field == null)
      return null;

    return id + "_" + field;
  }

  //
  // Translator
  //

  public DummyTranslator(final int fieldCount) {

    this.fieldCount = fieldCount;
  }

}
