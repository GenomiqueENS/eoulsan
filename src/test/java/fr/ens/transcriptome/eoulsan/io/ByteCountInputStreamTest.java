package fr.ens.transcriptome.eoulsan.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;

import org.junit.Test;

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
        for (int y = 0; y < count; y++)
          sb.append('1');

        final String s = sb.toString();
        testString1(s);
        testString2(s);

      }

    } catch (IOException e) {
      assertTrue(false);
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
