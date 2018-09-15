package fr.ens.biologie.genomique.eoulsan.core;

import static com.google.common.base.CharMatcher.inRange;
import static java.util.Objects.requireNonNull;

import com.google.common.base.CharMatcher;

/**
 * This class contains methods to validate workflow object names.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class Naming {

  public static final CharMatcher ASCII_LETTER_OR_DIGIT =
      inRange('a', 'z').or(inRange('A', 'Z')).or(inRange('0', '9'));

  /**
   * Convert a string to a valid name string that can be used for step id or
   * data name.
   * @param name the name to convert
   * @return a string with only the name characters argument that are allowed by
   *         the file naming convention
   */
  public static final String toValidName(final String name) {

    requireNonNull(name, "name argument cannot be null");

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < name.length(); i++) {

      final char c = name.charAt(i);
      if (ASCII_LETTER_OR_DIGIT.matches(c)) {
        sb.append(c);
      }
    }

    return sb.toString();
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private Naming() {
  }

}
