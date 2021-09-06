package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import static fr.ens.biologie.genomique.eoulsan.bio.readsfilters.PolyATailReadFilter.polyATailLength;
import static fr.ens.biologie.genomique.eoulsan.bio.readsfilters.PolyATailReadFilter.polyTTailLength;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

public class PolyATailReadFilterTest {

  @Test
  public void testPolyATailLength() {

    assertEquals(0, polyATailLength("", 5, .1));
    assertEquals(0, polyATailLength("T", 5, .1));
    assertEquals(0, polyATailLength("G", 5, .1));
    assertEquals(0, polyATailLength("C", 5, .1));
    assertEquals(1, polyATailLength("A", 5, .1));
    assertEquals(1, polyATailLength("CA", 5, .1));
    assertEquals(2, polyATailLength("AA", 5, .1));
    assertEquals(2, polyATailLength("CAA", 5, .1));
    assertEquals(3, polyATailLength("AAA", 5, .1));

    assertEquals(5, polyATailLength(
        "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTCAAAAACTTTTTATTTATAAAGACAAA", 5, .1));

    assertEquals(45, polyATailLength(
        "GTTTAGTTTTTTCCAATTAAAGAAGCAATTGGAGAGGGAAAAAAAAAAAAAAAAAAAAAAAGAAAAAAAAAAAAAAAAA",
        5, .1));
    assertEquals(22, polyATailLength(
        "CATGAGCAATGGGAAAAGTACCTCATTCTTATATTCTTTACTTATTCTTCAAAAAAAAAAAAAAAAAAAAAA",
        5, .1));
    assertEquals(4,
        polyATailLength("TATACGTCTCCATTTATTGATGAGGATCTTAAAGCTGAAAA", 5, .1));
    assertEquals(25, polyATailLength(
        "TGCTTATGCCATGATAGCTTTCTACACTGTATTACAAAAAAAAAAAAAAAAAAAAAAA", 5, .1));
    assertEquals(25, polyATailLength(
        "GGGGAAGAAGATGACTACTATATCTTTTTAGGCATAGTATATAGCCTTTAATATTCTTAAAGCAAATGGCAGTTGAGGGCATTCTATAGTTAAAAAAAAAAAAAAAAAAAAAAAAA",
        5, .1));
  }

  @Test
  public void testPolyTTailLength() {

    assertEquals(0, polyTTailLength("", 5, .1));
    assertEquals(0, polyTTailLength("A", 5, .1));
    assertEquals(0, polyTTailLength("G", 5, .1));
    assertEquals(0, polyTTailLength("C", 5, .1));
    assertEquals(1, polyTTailLength("T", 5, .1));
    assertEquals(1, polyTTailLength("TC", 5, .1));
    assertEquals(2, polyTTailLength("TT", 5, .1));
    assertEquals(2, polyTTailLength("TTC", 5, .1));
    assertEquals(3, polyTTailLength("TTT", 5, .1));

    assertEquals(30, polyTTailLength(
        "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTCAAAAACTTTTTATTTATAAAGACAAA", 5, .1));
    assertEquals(22, polyTTailLength(
        "TTTTTTTTTTTTTTTTTTTTTTGGAAAAAAAGCGCTTATTGTCCCTAGCTGGAGGAGGC", 5, .1));
    assertEquals(17, polyTTailLength(
        "TTTTTTTTTTTTTTTTTCGGTTGACAGTTTATTAAATACACTACATTTATACCTT", 5, .1));
    assertEquals(24, polyTTailLength(
        "TTTTTTTTTTTTTTTTTTTGTTTTACTTTAAGATTTACTTAGGAAAAATAAGGTGC", 5, .1));
    assertEquals(21, polyTTailLength(
        "TTTTTTTTTTTTTTTTTTTTTGGAAGAATAAGTAAAGAATATAAAGTGAGGCAATTTTCC", 5, .1));
    assertEquals(0,
        polyTTailLength(
            "GGGAGTTTTCCAGGTCCGAACTTGGGCTGCTGCATTGAACTCCGACCAATTCTAGCGGCGATGCA",
            5, .1));
    assertEquals(0,
        polyTTailLength("GGGCCTGGTCCAGCACAGTGAGCTCCAGCCTAGCACATGGT", 5, .1));

  }

  @Test
  public void testAcceptReadSequenceReadSequence() {

    ReadFilter filter = new PolyATailReadFilter();

    ReadSequence r = new ReadSequence("96f7d3fa-263e-465d-b2e9-bbe2998426f4",
        "ATGC", "!!!!");

    assertTrue(filter.accept(r));
    assertEquals("96f7d3fa-263e-465d-b2e9-bbe2998426f4 tail_type=invalid",
        r.getName());

    r = new ReadSequence("96f7d3fa-263e-465d-b2e9-bbe2998426f4",
        "GTTTAGTTTTTTCCAATTAAAGAAGCAATTGGAGAGGGAAAAAAAAAAAAAAAAA",
        "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

    assertTrue(filter.accept(r));
    assertEquals("96f7d3fa-263e-465d-b2e9-bbe2998426f4 tail_type=polyA",
        r.getName());

    r = new ReadSequence("96f7d3fa-263e-465d-b2e9-bbe2998426f4",
        "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTCAAAAACTTTTTATTTATAAAGACAAA",
        "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

    assertTrue(filter.accept(r));
    assertEquals("96f7d3fa-263e-465d-b2e9-bbe2998426f4 tail_type=polyT",
        r.getName());

    r = new ReadSequence("96f7d3fa-263e-465d-b2e9-bbe2998426f4",
        "CTGTCATGGAATTTTTGTTAAGACTTAAATAGATAATCCCCGTCTTGCAAACTGATTGCAGGAAGATGCCCGTTAGAGGTAGAAAGTAATTAAGCAGCTAGG",
        "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

    assertTrue(filter.accept(r));
    assertEquals("96f7d3fa-263e-465d-b2e9-bbe2998426f4 tail_type=invalid",
        r.getName());

    r = new ReadSequence("96f7d3fa-263e-465d-b2e9-bbe2998426f4",
        "TTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAA",
        "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

    assertTrue(filter.accept(r));
    assertEquals("96f7d3fa-263e-465d-b2e9-bbe2998426f4 tail_type=ambiguous",
        r.getName());

  }

}
