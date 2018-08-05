package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

public class MaxLengthReadFilterTest {

  @Test
  public void testAcceptLengthReadFilter() throws EoulsanException {

    ReadFilter filter = new MaxLengthReadFilter();
    filter.setParameter("maximum.length.threshold", "9");
    filter.init();

    assertFalse(filter.accept(null));

    ReadSequence read = new ReadSequence();
    assertTrue(filter.accept(read));

    read.setName("read1");
    assertTrue(filter.accept(read));

    read.setQuality("xxxxxxxx");
    assertTrue(filter.accept(read));

    read = new ReadSequence("toto", "ATGCATGC", "xxxxxxxx");
    assertTrue(filter.accept(read));

    read = new ReadSequence("toto", "ATGCATGCA", "xxxxxxxxx");
    assertFalse(filter.accept(read));

    read = new ReadSequence("toto", "ATGCATGCAT", "xxxxxxxxxx");
    assertFalse(filter.accept(read));

    read = new ReadSequence("toto", "ATGCATGCATG", "xxxxxxxxxxx");
    assertFalse(filter.accept(read));

    read = new ReadSequence("toto", "ATGCATGCATGC", "xxxxxxxxxxxx");
    assertFalse(filter.accept(read));
  }
}
