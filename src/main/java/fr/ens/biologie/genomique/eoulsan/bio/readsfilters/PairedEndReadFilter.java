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

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;
import fr.ens.biologie.genomique.eoulsan.log.DummyLogger;
import fr.ens.biologie.genomique.eoulsan.log.GenericLogger;

/**
 * This class define a read filter that allow to filter paired-end reads of
 * single end reads.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class PairedEndReadFilter implements ReadFilter {

  private GenericLogger logger = new DummyLogger();
  private boolean acceptPairedEnd = true;
  private final boolean acceptSingleEnd = true;

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

    return this.acceptSingleEnd;
  }

  @Override
  public boolean accept(final ReadSequence read1, final ReadSequence read2) {

    if (read1 == null || read2 == null) {
      return false;
    }

    return this.acceptPairedEnd;
  }

  @Override
  public String getName() {

    return "pairedend";
  }

  @Override
  public String getDescription() {

    return "Pair end ReadFilter";
  }

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    if (key == null || value == null) {
      return;
    }

    if ("accept.paired.end".equals(key.trim())) {
      this.acceptPairedEnd = Boolean.parseBoolean(value.trim());
    } else if ("accept.single.end".equals(key.trim())) {
      this.acceptPairedEnd = Boolean.parseBoolean(value.trim());
    } else {
      throw new EoulsanException(
          "Unknown parameter for " + getName() + " read filter: " + key);
    }

  }

  @Override
  public void init() {
  }

  @Override
  public String toString() {

    return this.getClass().getSimpleName()
        + "{acceptSingleEnd=" + this.acceptSingleEnd + ", acceptPairedEnd="
        + this.acceptPairedEnd + "}";
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public PairedEndReadFilter() {
  }

}
