package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

public class LeadingTrimmerReadFilterTest {
  @Test
  public void LeadingTirmmertest() throws EoulsanException {
    ReadFilter filter = new LeadingTrimmerReadFilter();

    filter.setParameter("arguments", "33");
    filter.init();

    ReadSequence read = new ReadSequence(0, "read1", "AGG", "ABC");
    assertTrue(filter.accept(read));
    assertEquals("read1", read.getName());
    assertEquals("GG", read.getSequence());
    assertEquals("BC", read.getQuality());
    assertFalse(filter.accept(null));

    filter = new LeadingTrimmerReadFilter();
    filter.setParameter("arguments", "34");
    filter.init();
    assertTrue(filter.accept(read));
    assertEquals("read1", read.getName());
    assertEquals("G", read.getSequence());
    assertEquals("C", read.getQuality());
    assertFalse(filter.accept(null));

    filter = new LeadingTrimmerReadFilter();
    filter.setParameter("arguments", "35");
    filter.init();
    assertFalse(filter.accept(read));
    assertEquals("read1", read.getName());
    assertEquals("G", read.getSequence());
    assertEquals("C", read.getQuality());
    assertFalse(filter.accept(null));

    filter = new LeadingTrimmerReadFilter();
    filter.setParameter("arguments", "34");
    filter.init();
    read = new ReadSequence(0, "read2", "AGAGTTA", "CABABAA");
    assertTrue(filter.accept(read));
    assertEquals("read2", read.getName());
    assertEquals("AGAGTTA", read.getSequence());
    assertEquals("CABABAA", read.getQuality());

    read = new ReadSequence(0, "read3", "AGAGT", "ABABC");
    assertTrue(filter.accept(read));
    assertEquals("read3", read.getName());
    assertEquals("T", read.getSequence());
    assertEquals("C", read.getQuality());

  }
}
