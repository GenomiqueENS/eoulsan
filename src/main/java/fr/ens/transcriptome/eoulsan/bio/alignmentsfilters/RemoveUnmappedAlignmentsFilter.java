package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.samtools.SAMRecord;

/**
 * This alignment filter remove all the unmapped alignments.
 * @author Laurent Jourdren
 */
public class RemoveUnmappedAlignmentsFilter extends AbstractAlignmentsFilter {

  @Override
  public String getName() {

    return "removeumap";
  }

  @Override
  public String getDescription() {

    return "Remove all the unmapped alignments";
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<SAMRecord> acceptedAlignments(final List<SAMRecord> records) {

    if (records == null)
      return Collections.EMPTY_LIST;

    final List<SAMRecord> result = new ArrayList<SAMRecord>();

    for (SAMRecord r : records)
      if (!r.getReadUnmappedFlag())
        result.add(r);

    return result;
  }

}
