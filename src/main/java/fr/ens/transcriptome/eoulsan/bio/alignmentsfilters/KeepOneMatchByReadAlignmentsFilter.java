package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import java.util.Collections;
import java.util.List;

import net.sf.samtools.SAMRecord;

/**
 * This alignments filter keep only one alignment for a read. This filter is
 * useful to count the number of reads that can match on the genome.
 * @author Laurent Jourdren
 */
public class KeepOneMatchByReadAlignmentsFilter extends
    AbstractAlignmentsFilter {

  @Override
  public String getName() {

    return "keeponematches";
  }

  @Override
  public String getDescription() {

    return "After this filter only one alignment is keeped by read";
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<SAMRecord> acceptedAlignments(final List<SAMRecord> records) {

    if (records == null)
      return Collections.EMPTY_LIST;

    if (records.size() == 1)
      return records;

    return Collections.singletonList(records.get(0));
  }

}
