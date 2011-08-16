package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import java.util.Collections;
import java.util.List;

import net.sf.samtools.SAMRecord;

/**
 * This alignments filter remove all the alignments if more there is more one
 * alignments for a read.
 * @author Laurent Jourdren
 */
public class RemoveMultiMatchesAlignmentsFilter extends
    AbstractAlignmentsFilter {

  @Override
  public String getName() {

    return "removemultimatches";
  }

  @Override
  public String getDescription() {

    return "Remove all the alignments with several matches";
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<SAMRecord> acceptedAlignments(final List<SAMRecord> records) {

    if (records == null)
      return Collections.EMPTY_LIST;

    if (records.size() > 1)
      return Collections.EMPTY_LIST;

    return records;
  }

}
