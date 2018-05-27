package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import org.usadellab.trimmomatic.trim.AbstractSingleRecordTrimmer;
import org.usadellab.trimmomatic.trim.HeadCropTrimmer;

/**
 * This class define a Headcrop trimmomatic readfilter allow paired-end and
 * single-end
 * @since 1.0
 * @author du
 */
public class HeadCropTrimmerReadFilter extends AbstractTrimmomaticReadFilter {

  @Override
  public String getName() {
    return "headcrop";
  }

  @Override
  protected AbstractSingleRecordTrimmer createTrimmer(String trimmerArgs) {
    return new HeadCropTrimmer(trimmerArgs);
  }

}
