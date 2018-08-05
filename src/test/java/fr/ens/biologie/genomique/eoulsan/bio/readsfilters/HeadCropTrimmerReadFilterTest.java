package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

public class HeadCropTrimmerReadFilterTest {

  @Test
  public void HeadCropTirmmertest() throws EoulsanException {

    ReadFilter filter = new HeadCropTrimmerReadFilter();

    filter.setParameter("arguments", "5");
    filter.init();

    ReadSequence read =
        new ReadSequence("read1", "AGGGGGCAAA", "xwxwxxabcd");
    assertTrue(filter.accept(read));
    assertEquals("read1", read.getName());
    assertEquals("GCAAA", read.getSequence());
    assertEquals("xabcd", read.getQuality());

    assertFalse(filter.accept(null));

    assertFalse(filter.accept(new ReadSequence("read2", "AGGGG", "xxxxx")));

    read = new ReadSequence("read3", "AGGGGGCAAA", "xxxxxxxxxx");
    assertTrue(filter.accept(read));
    assertEquals("read3", read.getName());
    assertNotEquals("AGGGGGCAAA", read.getSequence());
    assertNotEquals("xxxxxxxxxx", read.getQuality());

    filter = new HeadCropTrimmerReadFilter();
    filter.setParameter("arguments", "11");
    filter.init();

    read = new ReadSequence("read4", "AGGGGGCAAA", "xxxxxxxxxx");
    assertFalse(filter.accept(read));
    assertEquals("read4", read.getName());
    assertEquals("AGGGGGCAAA", read.getSequence());
    assertEquals("xxxxxxxxxx", read.getQuality());

  }

}
