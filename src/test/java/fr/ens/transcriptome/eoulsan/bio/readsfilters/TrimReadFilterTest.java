package fr.ens.transcriptome.eoulsan.bio.readsfilters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

public class TrimReadFilterTest {

  @Test
  public void testAcceptReadSequence() {

    ReadFilter filter = new TrimReadFilter(5);

    try {
      filter.accept(null);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    ReadSequence read = new ReadSequence();
    assertFalse(filter.accept(read));

    read.setName("read1");
    assertFalse(filter.accept(read));

    read.setQuality("xxxxxxxx");
    assertFalse(filter.accept(read));

    read.setSequence("ATGCATGC");
    assertTrue(filter.accept(read));

    read.setSequence("ATGCATGN");
    assertTrue(filter.accept(read));
    assertEquals("ATGCATGN", read.getSequence());

    read.setSequence("ATGCATNN");
    assertTrue(filter.accept(read));
    assertEquals("ATGCAT", read.getSequence());

    read.setSequence("ATGCANNN");
    assertFalse(filter.accept(read));

    read.setSequence("ATGCNNNN");
    assertFalse(filter.accept(read));

  }

}
