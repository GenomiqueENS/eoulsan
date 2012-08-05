package fr.ens.transcriptome.eoulsan.bio.io;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import fr.ens.transcriptome.eoulsan.bio.Sequence;

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
