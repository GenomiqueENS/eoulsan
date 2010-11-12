package fr.ens.transcriptome.eoulsan.bio.readsfilters;

import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

/**
 * This class define an abstract ReadFilter that allow simple Pair-end and
 * Mate-pair filter handling.
 * @author Laurent Jourdren
 */
public abstract class AbstractReadFilter implements ReadFilter {

  @Override
  public boolean accept(ReadSequence read1, ReadSequence read2) {

    return accept(read1) && accept(read2);
  }

}
