package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

public class CropTrimmerReadFilterTest {
  @Test
  public void CropTirmmertest() throws EoulsanException {
    ReadFilter filter = new CropTrimmerReadFilter();

    filter.setParameter("arguments", "5");
    filter.init();

    ReadSequence read = new ReadSequence("read1", "AGGGGGCAAA", "xwxwxxabcd");
    assertTrue(filter.accept(read));
    assertEquals("read1", read.getName());
    assertEquals("AGGGG", read.getSequence());
    assertEquals("xwxwx", read.getQuality());
    assertFalse(filter.accept(null));

  }
}
