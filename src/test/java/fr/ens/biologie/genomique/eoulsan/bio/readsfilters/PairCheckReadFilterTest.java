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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

public class PairCheckReadFilterTest {

  @Test
  public void testAcceptReadSequence() throws EoulsanException {

    ReadFilter filter = new PairCheckReadFilter();

    assertFalse(filter.accept(null));

    ReadSequence read1 = new ReadSequence("read/1", "ATG", "wxy");
    ReadSequence read2 = new ReadSequence("read/2", "ATG", "wxy");

    assertTrue(filter.accept(read1, read2));

    assertFalse(filter.accept(null, read2));

    assertFalse(filter.accept(read1, null));

    assertFalse(filter.accept(null, null));

    read1.setName("read1");
    assertFalse(filter.accept(read1, read2));
    read1.setName("read/1");

    read1.setName("read/2");
    assertFalse(filter.accept(read1, read2));
    read1.setName("read/1");

    read1.setName("READ/1");
    assertFalse(filter.accept(read1, read2));
    read1.setName("read/1");

  }

}
