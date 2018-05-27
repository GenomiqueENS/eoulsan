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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters;

import java.util.ArrayList;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;
import htsjdk.samtools.SAMRecord;

/**
 * This class define an alignments filter that calls successively a list of
 * alignments filters.
 * @since 1.1
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
public class MultiReadAlignmentsFilter implements ReadAlignmentsFilter {

  private final List<ReadAlignmentsFilter> list = new ArrayList<>();
  private final ReporterIncrementer incrementer;
  private final String counterGroup;

  @Override
  public void filterReadAlignments(final List<SAMRecord> records) {

    boolean pairedEnd = false;

    if (records == null || records.isEmpty()) {
      return;
    }

    if (records.get(0).getReadPairedFlag()) {
      pairedEnd = true;
    }

    for (ReadAlignmentsFilter af : this.list) {

      final int sizeBefore = records.size();
      af.filterReadAlignments(records);

      final int sizeAfter = records.size();
      final int diff = sizeBefore - sizeAfter;

      if (diff > 0 && this.incrementer != null) {
        // paired-end mode
        if (pairedEnd) {
          this.incrementer.incrCounter(this.counterGroup,
              "alignments rejected by " + af.getName() + " filter", diff / 2);
        }
        // single-end mode
        else {
          this.incrementer.incrCounter(this.counterGroup,
              "alignments rejected by " + af.getName() + " filter", diff);
        }
      }

      if (sizeAfter == 0) {
        return;
      }
    }
  }

  /**
   * Add a filter to the multi filter.
   * @param filter filter to add
   */
  public void addFilter(final ReadAlignmentsFilter filter) {

    if (filter != null) {
      this.list.add(filter);
    }

  }

  @Override
  public String getName() {

    return "MultiAlignmentsFilter";
  }

  @Override
  public String getDescription() {

    return "Multi alignments filter";
  }

  @Override
  public void setParameter(final String key, final String value) {
    // This filter has no parameter
  }

  @Override
  public void init() {
  }

  /**
   * Get the name of the filters.
   * @return a list with the names of the filters
   */
  public List<String> getFilterNames() {

    final List<String> result = new ArrayList<>();
    for (ReadAlignmentsFilter f : this.list) {
      result.add(f.getName());
    }

    return result;
  }

  @Override
  public String toString() {

    return this.getClass().getSimpleName()
        + "{incrementer=" + this.incrementer + ",counterGroup="
        + this.counterGroup + " , list=" + this.list + "}";
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   */
  public MultiReadAlignmentsFilter() {

    this(null, null);
  }

  /**
   * Public constructor.
   * @param incrementer incrementer to use
   * @param counterGroup counter group for the incrementer
   */
  public MultiReadAlignmentsFilter(final ReporterIncrementer incrementer,
      final String counterGroup) {

    this.incrementer = incrementer;
    this.counterGroup = counterGroup;
  }

  /**
   * Public constructor.
   * @param filters filters to add
   */
  public MultiReadAlignmentsFilter(final List<ReadAlignmentsFilter> filters) {

    this(null, null, filters);
  }

  /**
   * Public constructor.
   * @param incrementer incrementer to use
   * @param counterGroup counter group for the incrementer
   * @param filters filters to add
   */
  public MultiReadAlignmentsFilter(final ReporterIncrementer incrementer,
      final String counterGroup, final List<ReadAlignmentsFilter> filters) {

    this.incrementer = incrementer;
    this.counterGroup = counterGroup;

    if (filters != null) {

      for (ReadAlignmentsFilter filter : filters) {
        addFilter(filter);
      }
    }
  }

}
