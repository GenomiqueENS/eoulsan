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

package fr.ens.biologie.genomique.eoulsan.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.*;

public class ByteCountInputStreamTest {

  @Test
  public void test() {

    try {

      testString1("Il est beau le soleil.");
      testString2("Il est beau le soleil.");

      final Random rand = new Random(System.currentTimeMillis());
      final StringBuilder sb = new StringBuilder();

      for (int i = 0; i < 100; i++) {

        sb.setLength(0);
        final int count = rand.nextInt(100000);
        for (int y = 0; y < count; y++) {
          sb.append('1');
        }

        final String s = sb.toString();
        testString1(s);
        testString2(s);

      }

    } catch (IOException e) {
        fail();
    }

  }

  private void testString1(final String s) throws IOException {

    final byte[] bytes = s.getBytes();

    final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

    final ByteCountInputStream bcis = new ByteCountInputStream(bais);

    while (bcis.read() != -1) {
    }

    bcis.close();

    assertEquals(bcis.getBytesRead(), bytes.length);
  }

  private void testString2(final String s) throws IOException {

    final byte[] bytes = s.getBytes();

    final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

    final ByteCountInputStream bcis =
        new ByteCountInputStream(bais, bytes.length);

    while (bcis.read() != -1) {
    }

    bcis.close();

  }

}
