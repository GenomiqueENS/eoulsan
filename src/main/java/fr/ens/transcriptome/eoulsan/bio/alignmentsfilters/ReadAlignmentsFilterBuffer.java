/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMRecord;

/**
 * This class define a buffer that store all the alignments with the same read
 * name before to apply an alignment filter. This class only works with
 * alignment data where all the alignments for a read name are in straight.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class ReadAlignmentsFilterBuffer {

  private final ReadAlignmentsFilter filter;
  private final List<SAMRecord> list = new ArrayList<SAMRecord>();
  private SAMRecord firstNewList;
  private String currentName;
  private boolean reuseResultList;

  /**
   * Add the provided alignment to a list of SAMRecord objects if this
   * alignment has the same read name as the other alignments of the list.
   * @param alignment
   * @return true if the alignment provides is stored, i.e. if it has the same
   * read name as the other alignments already stored.
   */
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

    // The read name is equal to the previous
    this.list.add(alignment);
    return false;

  }

  /**
   * Get the list of the alignments that pass the tests of the filter with the
   * same read name.records. Warning if reuseResultList argument in the
   * constructor is set to true, this method will always returns the same
   * object.
   * @return a list of SAM record
   */
  public List<SAMRecord> getFilteredAlignments() {

    // Filter alignment
    this.filter.filterReadAlignments(this.list);

    // Return the list of filtered alignment
    if (this.reuseResultList)
      return this.list;
    
    final List<SAMRecord> result = new ArrayList<SAMRecord>(this.list);
    this.list.clear();
    
    return result;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param filter the filter to use with this buffer.
   */
  public ReadAlignmentsFilterBuffer(final ReadAlignmentsFilter filter) {

    this(filter, false);
  }

  /**
   * Public constructor.
   * @param filter the filter to use with this buffer
   * @param reuseResultList true if the getFilteredAlignments() method must
   *          return always the same internal list
   */
  public ReadAlignmentsFilterBuffer(final ReadAlignmentsFilter filter,
      final boolean reuseResultList) {

    if (filter == null)
      throw new NullPointerException("The alignment filter is null");

    this.filter = filter;
    this.reuseResultList = reuseResultList;
  }

}
