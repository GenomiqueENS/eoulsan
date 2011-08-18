package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import java.util.List;

import fr.ens.transcriptome.eoulsan.EoulsanException;

import net.sf.samtools.SAMRecord;

/**
 * This interface define a filter for alignments.
 * @author Laurent Jourdren
 */
public interface AlignmentsFilter {

  /**
   * Filter a list of alignments from a list of alignment of one unique read.
   * All the read id in the records are the same. The input list is modified
   * after the call of the method.
   * @param records
   */
  void filterReadAlignments(List<SAMRecord> records);

  /**
   * Get the name of the filter.
   * @return the name of the filter
   */
  String getName();

  /**
   * Get the description of the filter.
   * @return the description of the filter
   */
  String getDescription();

  /**
   * Set a parameter of the filter.
   * @param key name of the parameter to set
   * @param value value of the parameter to set
   * @throws EoulsanException if the parameter is invalid
   */
  void setParameter(String key, String value) throws EoulsanException;

  /**
   * Initialize the filter.
   * @throws EoulsanException an error occurs while initialize the filter
   */
  void init() throws EoulsanException;;

}
