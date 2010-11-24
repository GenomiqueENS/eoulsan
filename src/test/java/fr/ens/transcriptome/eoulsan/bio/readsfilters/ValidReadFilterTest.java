package fr.ens.transcriptome.eoulsan.bio.readsfilters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

public class ValidReadFilterTest {

  @Test
  public void testAcceptReadSequence() throws EoulsanException {

    ReadFilter filter = new ValidReadFilter();

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

    read.setSequence("ATGC");
    assertFalse(filter.accept(read));

    read.setQuality("xxxx");
    assertTrue(filter.accept(read));

    read.setSequence("atgc");
    assertFalse(filter.accept(read));

    read.setSequence("ATGC");
    assertTrue(filter.accept(read));

    read.setQuality("xx x");
    assertFalse(filter.accept(read));

  }

}
