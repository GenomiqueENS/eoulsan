package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

public class PatternReadFilterTest {
  @Test
  public void testAcceptForbiddenReadSequence() throws EoulsanException {

    ReadFilter filter = new PatternReadFilter();

    // test regular expression
    filter.setParameter("forbidden.regex", "(A.GC){2}");
    filter.init();

    // Null case
    assertFalse(filter.accept(null));

    // Not illumina id case
    ReadSequence read =
        new ReadSequence(0, "read1", "AGGGGGCAAAAAATGCATGCAATCT", "wxy");
    assertFalse(filter.accept(read));

    assertTrue(filter.accept(new ReadSequence(0, "", "ATGATGC", "")));
    assertFalse(filter.accept(new ReadSequence(0, "", "ATGCACGC", "")));
    assertFalse(filter.accept(new ReadSequence(0, "", "AAATGCGATGCATGC", "")));
    assertTrue(filter.accept(new ReadSequence(0, "", "ATGCCGATGC", "")));
    assertTrue(filter.accept(new ReadSequence(0, "", "AGCT", "")));

    filter = new PatternReadFilter();
    filter.setParameter("forbidden.regex", "A*GC");
    filter.init();

    // Not illumina id case
    assertFalse(filter.accept(read));

    assertFalse(filter.accept(new ReadSequence(0, "", "AGC", "")));
    assertFalse(filter.accept(new ReadSequence(0, "", "AGGGGGC", "")));

    filter = new PatternReadFilter();
    filter.setParameter("forbidden.regex", "AG+C");
    filter.init();

    // Not illumina id case
    assertFalse(filter.accept(read));

    assertFalse(filter.accept(new ReadSequence(0, "", "AGGGGGC", "")));

    filter = new PatternReadFilter();
    filter.setParameter("forbidden.regex", "AT?GC");
    filter.init();

    // Not illumina id case
    assertFalse(filter.accept(read));

    assertFalse(filter.accept(new ReadSequence(0, "", "AGC", "")));
    assertFalse(filter.accept(new ReadSequence(0, "", "ATGC", "")));

    // test of pattern count
    filter = new PatternReadFilter();
    filter.setParameter("forbidden.regex", "(AA){2}");
    filter.init();

    // Not illumina id case
    assertFalse(filter.accept(read));

    assertFalse(filter.accept(new ReadSequence(0, "", "AAAAAA", "")));
    assertTrue(filter.accept(new ReadSequence(0, "", "AAA", "")));
    assertFalse(filter.accept(new ReadSequence(0, "", "AAAA", "")));

    filter = new PatternReadFilter();
    filter.setParameter("forbidden.regex", "(AA){3}");
    filter.init();

    // Not illumina id case
    assertFalse(filter.accept(read));

    assertFalse(filter.accept(new ReadSequence(0, "", "AAAAAA", "")));
    assertTrue(filter.accept(new ReadSequence(0, "", "AAA", "")));
    assertTrue(filter.accept(new ReadSequence(0, "", "AAAA", "")));

    filter = new PatternReadFilter();
    filter.setParameter("forbidden.regex", "ATGC");
    filter.init();

    // Not illumina id case
    assertFalse(filter.accept(read));

    assertTrue(filter.accept(new ReadSequence(0, "", "ATG", "")));
    assertFalse(filter.accept(new ReadSequence(0, "", "ATGC", "")));
    assertFalse(filter.accept(new ReadSequence(0, "", "AAATGCG", "")));
    assertTrue(filter.accept(new ReadSequence(0, "", "AGCT", "")));

    filter = new PatternReadFilter();
    filter.setParameter("allowed.regex", "ATGC");
    filter.init();

    assertTrue(filter.accept(read));

    assertFalse(filter.accept(new ReadSequence(0, "", "ATG", "")));
    assertTrue(filter.accept(new ReadSequence(0, "", "CATGCAA", "")));
    assertTrue(filter.accept(new ReadSequence(0, "", "ATGC", "")));
    assertTrue(filter.accept(new ReadSequence(0, "", "GCATGCAA", "")));

    filter = new PatternReadFilter();
    filter.setParameter("allowed.regex", "A.GC");
    filter.init();

    assertTrue(filter.accept(read));

    assertFalse(filter.accept(new ReadSequence(0, "", "AGCT", "")));
    assertTrue(filter.accept(new ReadSequence(0, "", "CATGCAA", "")));
    assertTrue(filter.accept(new ReadSequence(0, "", "ATGC", "")));
    assertTrue(filter.accept(new ReadSequence(0, "", "AAATGCG", "")));
  }
}
