package fr.ens.biologie.genomique.eoulsan.modules.fastqc;

import uk.ac.babraham.FastQC.Sequence.SequenceFile;

/**
 * This interface extends SequenceFile to add a getCount() method that allow to get the count of the
 * read entries.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface CounterSequenceFile extends SequenceFile {

  /**
   * Get the count of the read entries.
   *
   * @return the count of the read entries
   */
  long getCount();
}
