package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This interface define a genome index storage.
 * @author Laurent Jourdren
 */
public interface GenomeIndexStorage {

  /**
   * Get the DataFile that corresponds to a mapper and a genome
   * @param mapper mapper
   * @param genome genome description object for the genome
   * @return a DataFile that contains the path to the index or null if the index
   *         has not yet been computed
   */
  DataFile get(SequenceReadsMapper mapper, GenomeDescription genome);

  /**
   * Put the index archive in the storage.
   * @param mapper mapper
   * @param genome genome description object
   * @param indexArchive the DataFile that contains the index
   */
  void put(SequenceReadsMapper mapper, GenomeDescription genome,
      DataFile indexArchive);

}
