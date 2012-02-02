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

package fr.ens.transcriptome.eoulsan.bio.readsfilters;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.IlluminaReadId;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

/**
 * This class define a read filter that remove all the reads that not pass the
 * Illumina filter.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class IlluminaFilterFlagReadFilter extends AbstractReadFilter {

  private IlluminaReadId irid;

  @Override
  public boolean accept(final ReadSequence read) {

    if (read == null)
      return false;

    try {

      if (this.irid == null)
        this.irid = new IlluminaReadId(read.getName());
      else
        this.irid.parse(read.getName());

      return !this.irid.isFiltered();

    } catch (EoulsanException e) {
      return false;
    }
  }

  @Override
  public String getName() {

    return "illuminaid";
  }

  @Override
  public String getDescription() {

    return "Filter read with illumina id";
  }

}
