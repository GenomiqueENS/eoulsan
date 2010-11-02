package fr.ens.transcriptome.eoulsan.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Random;

import org.junit.Test;

public class ByteCountOutputStreamTest {

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

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    final ByteCountOutputStream bcos = new ByteCountOutputStream(baos);

    bcos.write(bytes);

    bcos.close();

    assertEquals(s, baos.toString());
    assertEquals(s.getBytes().length, bcos.getBytesNumberWritten());
  }

  private void testString2(final String s) throws IOException {

    final byte[] bytes = s.getBytes();

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    final ByteCountOutputStream bcos =
        new ByteCountOutputStream(baos, bytes.length);

    Writer writer = new OutputStreamWriter(bcos);

    writer.write(s);
    writer.close();
  }

}
