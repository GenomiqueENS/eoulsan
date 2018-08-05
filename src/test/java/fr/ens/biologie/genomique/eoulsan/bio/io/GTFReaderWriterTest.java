package fr.ens.biologie.genomique.eoulsan.bio.io;

import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.md5DigestToString;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import com.google.common.io.ByteStreams;

import fr.ens.biologie.genomique.eoulsan.bio.GFFEntry;

public class GTFReaderWriterTest {

  @Test
  public void testReadWrite() throws IOException, NoSuchAlgorithmException {

    testFile("/htseq-count/Saccharomyces_cerevisiae.SGD1.01.56-fixed.gtf");
  }

  private void testFile(final String resourcePath)
      throws NoSuchAlgorithmException, IOException {

    MessageDigest mdi = MessageDigest.getInstance("MD5");
    MessageDigest mdo = MessageDigest.getInstance("MD5");

    InputStream resourceStream =
        this.getClass().getResourceAsStream(resourcePath);

    if (resourceStream == null) {
      throw new IOException("resource not found: " + resourcePath);
    }

    try (InputStream is = resourceStream;
        OutputStream os = ByteStreams.nullOutputStream();
        DigestInputStream dis = new DigestInputStream(is, mdi);
        DigestOutputStream dos = new DigestOutputStream(os, mdo);
        GTFReader reader = new GTFReader(dis);
        GTFWriter writer = new GTFWriter(dos)) {

      for (GFFEntry e : reader) {
        writer.write(e);
      }
    }

    assertEquals(md5DigestToString(mdi), md5DigestToString(mdo));
  }

}
