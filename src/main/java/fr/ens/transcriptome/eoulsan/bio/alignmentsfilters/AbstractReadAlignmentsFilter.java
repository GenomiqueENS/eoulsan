package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * This class define an abstract AlignmentsFilter that contains default code for
 * some methods of AlignmentFilter.
 * @author Laurent Jourdren
 */
public abstract class AbstractReadAlignmentsFilter implements ReadAlignmentsFilter {

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    throw new EoulsanException("Unknown parameter for "
        + getName() + " alignments filter: " + key);
  }

  @Override
  public void init() {
  }

}
