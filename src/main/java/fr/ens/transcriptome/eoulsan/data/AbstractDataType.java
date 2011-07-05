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

package fr.ens.transcriptome.eoulsan.data;

/**
 * Define an abstract DataType.
 * @author Laurent Jourdren
 */
public abstract class AbstractDataType implements DataType {

  @Override
  public String getDescription() {

    return getName();
  }

  @Override
  public boolean isOneFilePerAnalysis() {

    return false;
  }

  @Override
  public boolean isDataTypeFromDesignFile() {

    return false;
  }

  @Override
  public String getDesignFieldName() {

    return null;
  }

  @Override
  public boolean equals(final Object o) {

    if (!(o instanceof DataType))
      return false;

    final DataType dt = (DataType) o;

    return eq(getName(), dt.getName())
        && eq(getDescription(), dt.getDescription())
        && eq(getPrefix(), dt.getPrefix())
        && isOneFilePerAnalysis() == dt.isOneFilePerAnalysis()
        && isDataTypeFromDesignFile() == dt.isDataTypeFromDesignFile()
        && eq(getDesignFieldName(), dt.getDesignFieldName());
  }

  /**
   * Check equality between 2 objects even if there are null.
   * @param a first object to compare
   * @param b seconde object to compare
   * @return true if the two object are equals
   */
  private static boolean eq(final Object a, final Object b) {

    if (a == b)
      return true;

    return a.equals(b);
  }

  @Override
  public String toString() {
    return getName();
  }

}
