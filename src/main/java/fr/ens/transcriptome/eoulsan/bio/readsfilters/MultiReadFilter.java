/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.bio.readsfilters;

import java.util.List;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

public class MultiReadFilter implements ReadFilter {

  private final List<ReadFilter> list = Lists.newArrayList();
  private final ReporterIncrementer incrementer;
  private final String counterGroup;

  @Override
  public boolean accept(final ReadSequence read) {

    for (ReadFilter rf : this.list) {

      if (!rf.accept(read)) {

        if (incrementer != null) {
          this.incrementer
              .incrCounter(counterGroup, "reads rejected by " + rf.getName() + " filter", 1);
        }
        return false;
      }

    }

    return true;
  }

  @Override
  public boolean accept(final ReadSequence read1, final ReadSequence read2) {

    for (ReadFilter rf : this.list) {

      if (!rf.accept(read1, read2)) {

        if (incrementer != null) {
          this.incrementer
              .incrCounter(counterGroup, "reads rejected by " + rf.getName() + " filter", 1);
        }
        return false;
      }

    }

    return true;
  }

  /**
   * Add a filter to the multi filter.
   * @param filter filter to add
   */
  public void addFilter(final ReadFilter filter) {

    if (filter != null) {
      this.list.add(filter);
    }

  }

  @Override
  public String getName() {

    return "Multi ReadFilter";
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   */
  public MultiReadFilter() {

    this((ReporterIncrementer) null, null);
  }

  /**
   * Public constructor.
   * @param incrementer incrementer to use
   * @param counterGroup counter group for the incrementer
   */
  public MultiReadFilter(final ReporterIncrementer incrementer,
      final String counterGroup) {

    this.incrementer = incrementer;
    this.counterGroup = counterGroup;
  }

  /**
   * Public constructor.
   * @param filters filters to add
   */
  public MultiReadFilter(final ReadFilter... filters) {

    this(null, null, filters);
  }

  /**
   * Public constructor.
   * @param incrementer incrementer to use
   * @param counterGroup counter group for the incrementer
   * @param filters filters to add
   */
  public MultiReadFilter(final ReporterIncrementer incrementer,
      final String counterGroup, final ReadFilter... filters) {

    this.incrementer = incrementer;
    this.counterGroup = counterGroup;

    if (filters != null) {

      for (ReadFilter filter : filters) {
        addFilter(filter);
      }
    }
  }

}
