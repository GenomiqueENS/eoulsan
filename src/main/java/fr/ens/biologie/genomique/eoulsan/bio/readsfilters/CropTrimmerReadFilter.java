package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import org.usadellab.trimmomatic.trim.AbstractSingleRecordTrimmer;
import org.usadellab.trimmomatic.trim.CropTrimmer;

/**
 * This class define a Crop trimmomatic readfilter allow paired-end and
 * single-end
 * @since 1.0
 * @author du
 */

public class CropTrimmerReadFilter extends AbstractTrimmomaticReadFilter {

  @Override
  public String getName() {
    return "crop";
  }

  @Override
  protected AbstractSingleRecordTrimmer createTrimmer(String trimmerArgs) {
    return new CropTrimmer(trimmerArgs);
  }

}
