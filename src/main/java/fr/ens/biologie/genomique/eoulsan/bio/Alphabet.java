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

package fr.ens.biologie.genomique.eoulsan.bio;

import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.util.Utils;

/**
 * This abstract class define an alphabet.
 * @since 1.1
 * @author Laurent Jourdren
 */
public abstract class Alphabet {

  /**
   * Get the name of the alphabet.
   * @return the name of the alphabet
   */
  public abstract String getName();

  /**
   * Get the letters of the alphabet.
   * @return an array of char with the letters of the alphabet
   */
  public abstract Set<Character> getLetters();

  /**
   * Test if lower case of letter is valid for this alphabet
   * @return true if lower case of letter is valid for this alphabet
   */
  protected abstract boolean isLowerCaseValid();

  /**
   * Get the complement for this letter
   * @param letter input letter
   * @return the complement for this letter or the letter if no complement
   *         exists for this letter
   */
  protected abstract char getComplement(char letter);

  /**
   * Test if a letter if valid.
   * @param letter the letter to test
   * @return true if the letter is valid
   */
  public boolean isLetterValid(final char letter) {

    final char l = isLowerCaseValid() ? Character.toUpperCase(letter) : letter;

    return getLetters().contains(l);
  }

  @Override
  public final String toString() {
    return getName();
  }

  @Override
  public int hashCode() {

    return Utils.hashCode(getName(), getLetters(), isLowerCaseValid());
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof Alphabet)) {
      return false;
    }

    final Alphabet that = (Alphabet) o;

    return Utils.equal(this.getName(), that.getName())
        && this.getLetters().equals(that.getLetters())
        && this.isLowerCaseValid() == that.isLowerCaseValid();
  }

}
