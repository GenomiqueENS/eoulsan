package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMRecord;

/**
 * This alignment filter remove all the unmapped alignments.
 * @author Laurent Jourdren
 */
public class RemoveUnmappedReadAlignmentsFilter extends AbstractReadAlignmentsFilter {

  private final List<SAMRecord> result = new ArrayList<SAMRecord>();

  @Override
  public String getName() {

    return "removeumap";
  }

  @Override
  public String getDescription() {

    return "Remove all the unmapped alignments";
  }

  @Override
  public void filterReadAlignments(final List<SAMRecord> records) {

    if (records == null)
      return;

    for (SAMRecord r : records)
      if (!r.getReadUnmappedFlag())
        this.result.add(r);

    records.removeAll(result);
    result.clear();
  }

}
