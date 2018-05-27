package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

public class TrailingTrimmerReadFilterTest {
  @Test
  public void TrailingTirmmertest() throws EoulsanException {
    ReadFilter filter = new TrailingTrimmerReadFilter();

    filter.setParameter("arguments", "33");
    filter.init();
    assertFalse(filter.accept(null));
    ReadSequence read = new ReadSequence(0, "read1", "AGG", "CBA");
    assertTrue(filter.accept(read));
    assertEquals("read1", read.getName());
    assertEquals("AG", read.getSequence());
    assertEquals("CB", read.getQuality());

    filter = new TrailingTrimmerReadFilter();
    filter.setParameter("arguments", "33");
    filter.init();
    read = new ReadSequence(0, "read2", "AAGGCTT", "CABA;:9");
    assertTrue(filter.accept(read));
    assertEquals("read2", read.getName());
    assertEquals("AAG", read.getSequence());
    assertEquals("CAB", read.getQuality());
    assertFalse(filter.accept(null));
  }
}
