package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

/**
 * Define a filter that check the length of the reads.
 * @since 2.0
 * @author Runxin DU
 */
public class MaxLengthReadFilter extends AbstractReadFilter {
  public static final String FILTER_NAME = "maxlength";

  private int maximumLengthThreshold;

  @Override
  public boolean accept(final ReadSequence read) {

    if (read == null) {
      return false;
    }

    return read.length() < this.maximumLengthThreshold;
  }

  @Override
  public String getName() {

    return FILTER_NAME;
  }

  @Override
  public String getDescription() {

    return "Max Length ReadFilter";
  }

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    if (key == null || value == null) {
      return;
    }

    if ("maximum.length.threshold".equals(key.trim())) {

      try {
        this.maximumLengthThreshold = Integer.parseInt(value.trim());
      } catch (NumberFormatException e) {
        return;
      }

      if (this.maximumLengthThreshold < 1) {
        throw new EoulsanException(
            "Invalid length threshold: " + this.maximumLengthThreshold);
      }
    } else {
      throw new EoulsanException(
          "Unknown parameter for " + getName() + " read filter: " + key);
    }

  }

  @Override
  public void init() {

    if (this.maximumLengthThreshold < 1) {
      throw new IllegalArgumentException(
          "Maximum length threshold is not set for "
              + getName() + " read filter.");
    }
  }
}
