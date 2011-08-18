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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

public class IlluminaFilterFlagReadFilterTest {

  @Test
  public void testAcceptReadSequence() throws EoulsanException {

    ReadFilter filter = new IlluminaFilterFlagReadFilter();
    filter.init();

    // Null case
    assertFalse(filter.accept(null));

    // Not illumina id case
    ReadSequence read = new ReadSequence(0, "read1", "ATG", "wxy");
    assertFalse(filter.accept(read));

    // Good id
    read =
        new ReadSequence(0, "AEGIR:25:B0866ABXX:8:1101:1193:2125 1:N:0:CGATGT",
            "CCGAAGCAGAAGTCTAGAGGCGGGGACTGAAGCAGAAGACAGGAGAAGTGT",
            "@?@DDDD?CBFFDEHCF<FHGGHFB##########################");

    assertTrue(filter.accept(read));

    // Bad id
    read =
        new ReadSequence(0,
            "@AEGIR:25:B0866ABXX:8:1101:1176:2126 1:Y:0:CGATGT",
            "TGGAGNCAGGAGTCTGGGGGGGGGGGGGGTGGTGCAAAACTGGGGGGACGC",
            "###################################################");
    assertFalse(filter.accept(read));

  }

}
