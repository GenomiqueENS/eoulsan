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

package fr.ens.biologie.genomique.eoulsan.bio.io;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.bio.Sequence;

public class FastaReaderTest {

  @Test
  public void testFastaReaderInputStream() throws IOException {

    final InputStream is = this.getClass().getResourceAsStream("/phix.fasta");

    final SequenceReader reader = new FastaReader(is);

    for (Sequence s : reader) {

      assertEquals(0, s.getId());
      assertEquals(
          "gi|9626372|ref|NC_001422.1| Enterobacteria phage phiX174, complete genome",
          s.getName());
      assertEquals(5386, s.length());
    }
    reader.close();
    reader.throwException();

  }

}
