package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

/**
 * This filter will remove all the reads without a GGG head sequence.
 * @since 2.4
 * @author Laurent Jourdren
 */
public class RequireGGGHeadReadFilter extends AbstractReadFilter {

  public static final String FILTER_NAME = "requireggghead";

  private static final int ADDITIONAL_BASE_COUNT = 3;

  private boolean mismatch = true;

  @Override
  public String getName() {

    return FILTER_NAME;
  }

  @Override
  public String getDescription() {

    return "additional GGG identifier";
  }

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    switch (key) {

    case "allow.mismatch":
      this.mismatch = Boolean.parseBoolean(value);
      break;

    default:
      throw new EoulsanException(
          "Unknown parameter for " + getName() + " read filter: " + key);
    }

  }

  @Override
  public boolean accept(ReadSequence read) {

    String sequence = read.getSequence();
    int length = read.length();

    if (length < 3) {
      return false;
    }

    String start =
        sequence.substring(0, Math.min(ADDITIONAL_BASE_COUNT, length));

    return count(start, 'G') >= (this.mismatch ? 2 : 3);
  }

  private static int count(String s, char c) {

    int result = 0;

    for (int i = 0; i < s.length(); i++) {

      if (s.charAt(i) == c) {
        result++;
      }
    }
    return result;
  }

}
