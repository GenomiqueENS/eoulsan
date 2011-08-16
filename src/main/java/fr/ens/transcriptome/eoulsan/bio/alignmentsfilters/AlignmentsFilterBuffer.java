package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMRecord;

/**
 * This class define a buffer that store all the alignments with the same read
 * name before to apply an alignment filter. This class only works with
 * alignment data where all the alignments for a read name are in straight.
 * @author Laurent Jourdren
 */
public class AlignmentsFilterBuffer {

  private final AlignmentsFilter filter;
  private final List<SAMRecord> list = new ArrayList<SAMRecord>();
  private SAMRecord firstNewList;
  private String currentName;

  public boolean addAlignment(final SAMRecord alignment) {

    if (alignment == null)
      return false;

    final String name = alignment.getReadName();

    // Special case for the first alignment
    if (currentName == null) {
      currentName = name;
      this.list.add(alignment);
      return false;
    }

    // Last alignment has a new read name
    if (firstNewList != null) {
      this.list.clear();
      this.list.add(this.firstNewList);
      this.firstNewList = null;
    }

    // New alignment with a new read name
    if (!currentName.equals(name)) {
      firstNewList = alignment;
      return true;
    }

    // The read name is equals to the previous
    this.list.add(alignment);
    return false;

  }

  /**
   * Get the list of the alignments that pass the tests of the filter with the
   * same read name.records
   * @return a list of SAM record
   */
  public List<SAMRecord> getFilteredAlignments() {

    return this.filter.acceptedAlignments(this.list);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param filter the filter to use with this buffer
   */
  public AlignmentsFilterBuffer(final AlignmentsFilter filter) {

    if (filter == null)
      throw new NullPointerException("The alignment filter is null");

    this.filter = filter;
  }

}
