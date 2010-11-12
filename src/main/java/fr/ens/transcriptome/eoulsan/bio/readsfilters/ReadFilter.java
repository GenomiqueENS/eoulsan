package fr.ens.transcriptome.eoulsan.bio.readsfilters;

import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

/**
 * This interface define a filter for reads.
 * @author jourdren
 */
public interface ReadFilter {

  /**
   * Tests if a specified read should be keep.
   * @param read read to test
   */
  boolean accept(ReadSequence read);

  /**
   * Tests if the specified reads should be keep.
   * @param read1 first read to test
   * @param read2 first read to test
   */
  boolean accept(ReadSequence read1, ReadSequence read2);

}
