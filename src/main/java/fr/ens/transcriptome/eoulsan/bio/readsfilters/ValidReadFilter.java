package fr.ens.transcriptome.eoulsan.bio.readsfilters;

import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

/**
 * Define a filter that check if a read is valid.
 * @author Laurent Jourdren
 */
public class ValidReadFilter extends AbstractReadFilter {

  @Override
  public boolean accept(final ReadSequence read) {

    if (read == null)
      throw new NullPointerException("The read is null");

    return read.check();
  }

}
