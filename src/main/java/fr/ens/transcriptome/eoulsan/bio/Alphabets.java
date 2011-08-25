package fr.ens.transcriptome.eoulsan.bio;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class define common alphabets.
 * @author Laurent Jourdren
 */
public final class Alphabets {

  private static final Set<Character> AMBIGUOUS_DNA_ALPHABET_LETTERS =
      toUnmodifiableSet(new char[] {'G', 'A', 'T', 'C', 'R', 'Y', 'W', 'S',
          'M', 'K', 'H', 'B', 'V', 'D', 'N'});

  private static final Set<Character> UNAMBIGUOUS_DNA_ALPHABET_LETTERS =
      toUnmodifiableSet(new char[] {'G', 'A', 'T', 'C'});

  private static final Set<Character> AMBIGUOUS_RNA_ALPHABET_LETTERS =
      toUnmodifiableSet(new char[] {'G', 'A', 'U', 'C', 'R', 'Y', 'W', 'S',
          'M', 'K', 'H', 'B', 'V', 'D', 'N'});

  private static final Set<Character> UNAMBIGUOUS_RNA_ALPHABET_LETTERS =
      toUnmodifiableSet(new char[] {'G', 'A', 'U', 'C'});

  private static final Set<Character> READ_DNA_ALPHABET_LETTERS =
      toUnmodifiableSet(new char[] {'G', 'A', 'T', 'C', 'N'});

  //
  // Utility method
  //

  /**
   * Transform an array of char to an unmodifable set of Characters.
   * @param array array to transform
   * @return a unmodifiable Set with the elements of the input array
   */
  private static final Set<Character> toUnmodifiableSet(final char[] array) {

    if (array == null)
      return null;

    final Set<Character> result = new HashSet<Character>();

    for (char c : array)
      result.add(c);

    return Collections.unmodifiableSet(result);
  }

  //
  // Classes
  //

  /**
   * This class define an ambiguous DNA alphabet.
   * @author Laurent Jourdren
   */
  public static final Alphabet AMBIGUOUS_DNA_ALPHABET = new Alphabet() {

    @Override
    public final String getName() {

      return "AmbiguousDNA";
    }

    @Override
    public final Set<Character> getLetters() {

      return AMBIGUOUS_DNA_ALPHABET_LETTERS;
    }

    @Override
    protected final boolean isLowerCaseValid() {

      return true;
    }

    @Override
    protected final char getComplement(final char letter) {

      switch (letter) {

      case 'A':
        return 'T';
      case 'C':
        return 'G';
      case 'G':
        return 'C';
      case 'T':
        return 'A';
      case 'M':
        return 'K';
      case 'R':
        return 'Y';
      case 'W':
        return 'W';
      case 'S':
        return 'S';
      case 'Y':
        return 'R';
      case 'K':
        return 'M';
      case 'V':
        return 'B';
      case 'H':
        return 'D';
      case 'D':
        return 'H';
      case 'B':
        return 'V';
      case 'X':
        return 'X';
      case 'N':
        return 'N';

      case 'a':
        return 't';
      case 'c':
        return 'g';
      case 'g':
        return 'c';
      case 't':
        return 'a';
      case 'm':
        return 'k';
      case 'r':
        return 'y';
      case 'w':
        return 'w';
      case 's':
        return 's';
      case 'y':
        return 'r';
      case 'k':
        return 'm';
      case 'v':
        return 'b';
      case 'h':
        return 'd';
      case 'd':
        return 'h';
      case 'b':
        return 'v';
      case 'x':
        return 'x';
      case 'n':
        return 'n';

      default:
        return letter;
      }
    }
  };

  /**
   * This class define an unambiguous DNA alphabet.
   * @author Laurent Jourdren
   */
  public static final Alphabet UNAMBIGUOUS_DNA_ALPHABET = new Alphabet() {

    @Override
    public final String getName() {

      return "UnAmbiguousDNA";
    }

    @Override
    public final Set<Character> getLetters() {

      return UNAMBIGUOUS_DNA_ALPHABET_LETTERS;
    }

    @Override
    protected final boolean isLowerCaseValid() {

      return true;
    }

    @Override
    protected final char getComplement(final char letter) {

      switch (letter) {

      case 'A':
        return 'T';
      case 'C':
        return 'G';
      case 'G':
        return 'C';
      case 'T':
        return 'A';

      case 'a':
        return 't';
      case 'c':
        return 'g';
      case 'g':
        return 'c';
      case 't':
        return 'a';

      default:
        return letter;
      }
    }
  };

  /**
   * This class define an ambiguous RNA alphabet.
   * @author Laurent Jourdren
   */
  public static final Alphabet AMBIGUOUS_RNA_ALPHABET = new Alphabet() {

    @Override
    public final String getName() {

      return "AmbiguousRNA";
    }

    @Override
    public final Set<Character> getLetters() {

      return AMBIGUOUS_RNA_ALPHABET_LETTERS;
    }

    @Override
    protected final boolean isLowerCaseValid() {

      return true;
    }

    @Override
    protected final char getComplement(final char letter) {

      switch (letter) {

      case 'A':
        return 'U';
      case 'C':
        return 'G';
      case 'G':
        return 'C';
      case 'U':
        return 'A';
      case 'M':
        return 'K';
      case 'R':
        return 'Y';
      case 'W':
        return 'W';
      case 'S':
        return 'S';
      case 'Y':
        return 'R';
      case 'K':
        return 'M';
      case 'V':
        return 'B';
      case 'H':
        return 'D';
      case 'D':
        return 'H';
      case 'B':
        return 'V';
      case 'X':
        return 'X';
      case 'N':
        return 'N';

      case 'a':
        return 'u';
      case 'c':
        return 'g';
      case 'g':
        return 'c';
      case 'u':
        return 'a';
      case 'm':
        return 'k';
      case 'r':
        return 'y';
      case 'w':
        return 'w';
      case 's':
        return 's';
      case 'y':
        return 'r';
      case 'k':
        return 'm';
      case 'v':
        return 'b';
      case 'h':
        return 'd';
      case 'd':
        return 'h';
      case 'b':
        return 'v';
      case 'x':
        return 'x';
      case 'n':
        return 'n';

      default:
        return letter;
      }
    }
  };

  /**
   * This class define an unambiguous RNA alphabet
   * @author Laurent Jourdren
   */
  public static final Alphabet UNAMBIGUOUS_RNA_ALPHABET = new Alphabet() {

    @Override
    public final String getName() {

      return "UnAmbiguousRNA";
    }

    @Override
    public final Set<Character> getLetters() {

      return UNAMBIGUOUS_RNA_ALPHABET_LETTERS;
    }

    @Override
    protected final boolean isLowerCaseValid() {

      return true;
    }

    @Override
    protected final char getComplement(final char letter) {

      switch (letter) {

      case 'A':
        return 'U';
      case 'C':
        return 'G';
      case 'G':
        return 'C';
      case 'U':
        return 'A';

      case 'a':
        return 'u';
      case 'c':
        return 'g';
      case 'g':
        return 'c';
      case 'u':
        return 'a';

      default:
        return letter;
      }
    }
  };

  /**
   * This class define an alphabet for reads.
   * @author Laurent Jourdren
   */
  public static final Alphabet READ_DNA_ALPHABET = new Alphabet() {

    @Override
    public final String getName() {

      return "UnAmbiguousRNA";
    }

    @Override
    public final Set<Character> getLetters() {

      return READ_DNA_ALPHABET_LETTERS;
    }

    @Override
    protected final boolean isLowerCaseValid() {

      return false;
    }

    @Override
    protected final char getComplement(final char letter) {

      switch (letter) {

      case 'A':
        return 'T';
      case 'C':
        return 'G';
      case 'G':
        return 'C';
      case 'T':
        return 'A';
      case 'N':
        return 'N';

      default:
        return letter;
      }
    }

    @Override
    public final boolean isLetterValid(char letter) {

      // This method is fastest than the default implementation

      switch (letter) {

      case 'A':
      case 'C':
      case 'G':
      case 'T':
      case 'N':
        return true;

      default:
        return false;
      }

    }

  };

}
