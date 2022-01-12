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

package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;
import fr.ens.biologie.genomique.eoulsan.log.DummyLogger;
import fr.ens.biologie.genomique.eoulsan.log.GenericLogger;
import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;

/**
 * This class define a read filter that calls successively a list of read
 * filters.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class MultiReadFilter implements ReadFilter {

  private GenericLogger logger = new DummyLogger();
  private final List<ReadFilter> list = new ArrayList<>();
  private final ReporterIncrementer incrementer;
  private final String counterGroup;

  @Override
  public void setLogger(GenericLogger logger) {

    requireNonNull(logger);
    this.logger = logger;
  }

  @Override
  public GenericLogger getLogger() {

    return this.logger;
  }

  @Override
  public boolean accept(final ReadSequence read) {

    if (read == null) {
      return false;
    }

    for (ReadFilter rf : this.list) {

      if (!rf.accept(read)) {

        if (this.incrementer != null) {
          this.incrementer.incrCounter(this.counterGroup,
              "reads rejected by " + rf.getName() + " filter", 1);
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

        if (this.incrementer != null) {
          this.incrementer.incrCounter(this.counterGroup,
              "reads rejected by " + rf.getName() + " filter", 1);
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

    return "MultiReadFilter";
  }

  @Override
  public String getDescription() {

    return "Multi read filter";
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
    for (ReadFilter f : this.list) {
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
  public MultiReadFilter() {

    this(null, null);
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
  public MultiReadFilter(final List<ReadFilter> filters) {

    this(null, null, filters);
  }

  /**
   * Public constructor.
   * @param incrementer incrementer to use
   * @param counterGroup counter group for the incrementer
   * @param filters filters to add
   */
  public MultiReadFilter(final ReporterIncrementer incrementer,
      final String counterGroup, final List<ReadFilter> filters) {

    this.incrementer = incrementer;
    this.counterGroup = counterGroup;

    if (filters != null) {

      for (ReadFilter filter : filters) {
        addFilter(filter);
      }
    }
  }

}
