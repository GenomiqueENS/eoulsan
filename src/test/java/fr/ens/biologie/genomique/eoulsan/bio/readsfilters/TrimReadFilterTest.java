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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

public class TrimReadFilterTest {

  @Test
  public void testAcceptReadSequence() throws EoulsanException {

    ReadFilter filter = new TrimReadFilter();
    filter.setParameter("length.threshold", "5");
    filter.init();

    assertFalse(filter.accept(null));

    ReadSequence read = new ReadSequence();
    assertFalse(filter.accept(read));

    read.setName("read1");
    assertFalse(filter.accept(read));

    read.setQuality("xxxxxxxx");
    assertFalse(filter.accept(read));

    read = new ReadSequence(0, "toto", "ATGCATGC", "xxxxxxxx");
    assertTrue(filter.accept(read));

    read = new ReadSequence(0, "toto", "ATGCATGN", "xxxxxxxx");
    assertTrue(filter.accept(read));
    assertEquals("ATGCATGN", read.getSequence());

    read = new ReadSequence(0, "toto", "ATGCATNN", "xxxxxxxx");
    assertTrue(filter.accept(read));
    assertEquals("ATGCAT", read.getSequence());

    read = new ReadSequence(0, "toto", "ATGCANNN", "xxxxxxxx");
    assertFalse(filter.accept(read));

    read = new ReadSequence(0, "toto", "ATGCNNNN", "xxxxxxxx");
    assertFalse(filter.accept(read));
  }

}
