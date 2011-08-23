package fr.ens.transcriptome.eoulsan.bio;

import java.util.Arrays;

/**
 * This abstract class define an alphabet.
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
  public abstract char[] getLetters();

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
  public boolean isLetterValid(char letter) {

    final char l = isLowerCaseValid() ? Character.toLowerCase(letter) : letter;

    return Arrays.binarySearch(getLetters(), l) != -1;
  }

}
