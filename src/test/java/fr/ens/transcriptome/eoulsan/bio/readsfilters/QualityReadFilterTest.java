package fr.ens.transcriptome.eoulsan.bio.readsfilters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.NullArgumentException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

public class QualityReadFilterTest {

  @Test
  public void testAcceptReadSequence() throws EoulsanException {

    ReadFilter filter = new QualityReadFilter(50);

    try {
      filter.accept(null);
      assertTrue(false);
    } catch (NullArgumentException e) {
      assertTrue(true);
    }

    ReadSequence read = new ReadSequence(0, "read1", "ATG", "wxy");

    assertEquals('x' - 64.0, read.meanQuality(), 0.0);
    assertTrue(filter.accept(read));

    filter = new QualityReadFilter(60);
    assertFalse(filter.accept(read));
  }

}
