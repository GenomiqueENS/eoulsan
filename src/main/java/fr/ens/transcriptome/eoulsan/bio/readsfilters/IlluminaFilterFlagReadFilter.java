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

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.IlluminaReadId;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

public class IlluminaFilterFlagReadFilter extends AbstractReadFilter {

  private IlluminaReadId irid;

  @Override
  public boolean accept(final ReadSequence read) {

    if (read==null)
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
