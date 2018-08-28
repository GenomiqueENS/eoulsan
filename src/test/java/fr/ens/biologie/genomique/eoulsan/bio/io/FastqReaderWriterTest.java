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

import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

public class FastqReaderWriterTest {

  @Test
  public void testReadWrite() throws IOException, NoSuchAlgorithmException {

    testFile("/illumina_1_8.fastq");
    testFile("/fast5/alexander_PC_20161027_R9-4_1D_template.fastq");
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
        ReadSequenceReader reader = new FastqReader(dis);
        ReadSequenceWriter writer = new FastqWriter(dos)) {

      for (ReadSequence s : reader) {
        writer.write(s);
      }
    }

    assertEquals(md5DigestToString(mdi), md5DigestToString(mdo));
  }

}
