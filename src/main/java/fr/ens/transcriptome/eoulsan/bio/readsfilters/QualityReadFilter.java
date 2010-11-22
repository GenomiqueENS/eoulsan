package fr.ens.transcriptome.eoulsan.bio.readsfilters;

import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

/**
 * This class define a filter based on mean quality of a read.
 * @author Maria Bernard
 * @author Laurent Jourdren
 */
public class QualityReadFilter extends AbstractReadFilter {

  private double qualityThreshold;

  @Override
  public boolean accept(final ReadSequence read) {

    if (read == null)
      throw new NullPointerException("The read is null");

    return read.meanQuality() > qualityThreshold;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param qualityThreshold The minimal threshold for mean quality of reads
   */
  public QualityReadFilter(final double qualityThreshold) {

    if (qualityThreshold < 0.0)
      throw new IllegalArgumentException("Invalid qualityThreshold: "
          + qualityThreshold);

    this.qualityThreshold = qualityThreshold;

  }

}
