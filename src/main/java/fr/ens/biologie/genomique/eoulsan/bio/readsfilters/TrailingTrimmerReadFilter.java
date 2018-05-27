package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import org.usadellab.trimmomatic.trim.AbstractSingleRecordTrimmer;
import org.usadellab.trimmomatic.trim.TrailingTrimmer;

/**
 * This class define a Trailing trimmomatic readfilter allow paired-end and
 * single-end
 * @since 1.0
 * @author du
 */

public class TrailingTrimmerReadFilter extends AbstractTrimmomaticReadFilter {

  @Override
  public String getName() {
    return "trailing";
  }

  @Override
  protected AbstractSingleRecordTrimmer createTrimmer(String trimmerArgs) {
    return new TrailingTrimmer(trimmerArgs);
  }
}
