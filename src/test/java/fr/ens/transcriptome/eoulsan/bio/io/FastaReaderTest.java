package fr.ens.transcriptome.eoulsan.bio.io;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import fr.ens.transcriptome.eoulsan.bio.Sequence;

public class FastaReaderTest {

  @Test
  public void testFastaReaderInputStream() throws IOException {

    // final InputStream is =
    // FastaReaderTest.class.getResourceAsStream("/files/phix.fasta");
    final InputStream is =
        new FileInputStream(
            "/Users/jourdren/dev/eoulsan/src/test/java/files/phix.fasta");

    final SequenceReader reader = new FastaReader(is);

    for (Sequence s : reader) {

      System.out.println(s);
      assertEquals(0, s.getId());
      assertEquals(
          "gi|9626372|ref|NC_001422.1| Enterobacteria phage phiX174, complete genome",
          s.getName());
      assertEquals(5386, s.length());
    }
    reader.throwException();

    throw new IOException();
  }

}
