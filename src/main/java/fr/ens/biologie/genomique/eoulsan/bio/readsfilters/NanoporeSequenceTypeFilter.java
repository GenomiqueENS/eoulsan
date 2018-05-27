package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import java.util.Iterator;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.NanoporeReadId;
import fr.ens.biologie.genomique.eoulsan.bio.NanoporeReadId.SequenceType;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

/**
 * This class define a filter based on the Nanopore sequence type.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class NanoporeSequenceTypeFilter extends AbstractReadFilter {

  public static final String FILTER_NAME = "nanoporesequencetype";

  private final Splitter spliter = Splitter.on(' ').omitEmptyStrings();
  private NanoporeReadId.SequenceType sequenceType = SequenceType.CONSENSUS;

  @Override
  public String getName() {
    return FILTER_NAME;
  }

  @Override
  public String getDescription() {
    return "Filter nanopore reads against its type";
  }

  @Override
  public boolean accept(final ReadSequence read) {

    if (read == null) {
      return false;
    }

    Iterator<String> it = this.spliter.split(read.getName()).iterator();

    if (!it.hasNext()) {
      return false;
    }

    String sequenceName = it.next();

    switch (this.sequenceType) {

    case CONSENSUS:
      return sequenceName.indexOf('_') == -1;

    case TEMPLATE:
      return sequenceName.endsWith("_t");

    case COMPLEMENT:
      return sequenceName.endsWith("_c");

    default:
      return false;
    }
  }

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    if (key == null || value == null) {
      return;
    }

    if ("keep".equals(key.trim())) {

      SequenceType type;

      try {
        type = SequenceType.valueOf(value.toUpperCase().trim());
      }

      catch (IllegalArgumentException e) {
        throw new EoulsanException("Invalid sequence type: " + value);
      }

      this.sequenceType = type;

    } else {
      throw new EoulsanException(
          "Unknown parameter for " + getName() + " read filter: " + key);
    }
  }
}
