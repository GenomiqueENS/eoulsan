package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import org.usadellab.trimmomatic.fastq.FastqRecord;
import org.usadellab.trimmomatic.trim.AbstractSingleRecordTrimmer;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

/**
 * This classe define an abstract trimmomatic ReadFilter that allow paired-end
 * and single-end.
 * @since 1.0
 * @author du
 */
public abstract class AbstractTrimmomaticReadFilter extends AbstractReadFilter {

  private AbstractSingleRecordTrimmer trimmer;

  @Override
  public String getDescription() {
    return getName() + " Trimmomatic filter";
  }

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    if ("arguments".equals(key)) {

      try {
        this.trimmer = createTrimmer(value == null ? "" : value);
      } catch (Exception e) {
        throw new EoulsanException("Invalid parameter: " + value, e);
      }

    } else {
      throw new EoulsanException(
          "Unknown parameter for " + getName() + " read filter: " + key);
    }
  }

  @Override
  public void init() {

    if (this.trimmer == null) {
      this.trimmer = createTrimmer("");
    }
  }

  /**
   * Create a Trimmer object
   * @param trimmerArgs trimmer arguments
   * @return new sequence after trimmomatic
   */
  protected abstract AbstractSingleRecordTrimmer createTrimmer(
      String trimmerArgs);

  @Override
  public boolean accept(final ReadSequence read) {

    // The sequence can not be empty
    if (read == null) {
      return false;
    }

    // Fastq record entry
    FastqRecord in = new FastqRecord(read.getName(), read.getSequence(), "",
        read.getQuality(), read.getFastqFormat().getAsciiOffset());

    // Fastq record exit
    FastqRecord out = this.trimmer.processRecord(in);

    // The fastq record exit can not be empty
    if (out == null) {
      return false;
    }

    // update sequence and their quality after trimmomatic
    read.setSequence(out.getSequence());
    read.setQuality(out.getQuality());

    return true;
  }

}
