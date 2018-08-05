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

import fr.ens.biologie.genomique.eoulsan.bio.BEDEntry;

public class BEDReaderWriterTest {

  @Test
  public void testReadWrite() throws IOException, NoSuchAlgorithmException {

    testFile("/bed3.bed", 3);
    testFile("/bed9.bed", 9);
    testFile("/Unigene.unsorted1.bed", 12);
  }

  private void testFile(final String resourcePath, final int fieldCount)
      throws NoSuchAlgorithmException, IOException {

    MessageDigest mdi = MessageDigest.getInstance("MD5");
    MessageDigest mdo = MessageDigest.getInstance("MD5");

    try (InputStream is = this.getClass().getResourceAsStream(resourcePath);
        OutputStream os = ByteStreams.nullOutputStream();
        DigestInputStream dis = new DigestInputStream(is, mdi);
        DigestOutputStream dos = new DigestOutputStream(os, mdo);
        BEDReader reader = new BEDReader(dis);
        BEDWriter writer = new BEDWriter(dos, fieldCount)) {

      for (BEDEntry e : reader) {
        writer.write(e);
      }
    }

    assertEquals(md5DigestToString(mdi), md5DigestToString(mdo));
  }

}
