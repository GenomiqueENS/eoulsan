package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import org.usadellab.trimmomatic.trim.AbstractSingleRecordTrimmer;
import org.usadellab.trimmomatic.trim.SlidingWindowTrimmer;

/**
 * This class define a Slidingwindow trimmomatic readfilter allow paired-end and
 * single-end
 * @since 1.0
 * @author du
 */

public class SlidingWindowTrimmerReadFilter
    extends AbstractTrimmomaticReadFilter {

  @Override
  public String getName() {
    return "slidingwindow";
  }

  @Override
  protected AbstractSingleRecordTrimmer createTrimmer(String trimmerArgs) {
    return new SlidingWindowTrimmer(trimmerArgs);
  }

}
