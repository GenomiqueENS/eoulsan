package fr.ens.transcriptome.eoulsan.bio;

import static org.junit.Assert.*;

import org.junit.Test;

public class SequenceTest {

  @Test
  public void testGetSetId() {

    Sequence s = new Sequence();
    assertEquals(0, s.getId());

    s.setId(1);
    assertEquals(1, s.getId());

    s.setId(30);
    assertEquals(30, s.getId());
  }

  @Test
  public void testGetSetName() {

    Sequence s = new Sequence();
    assertEquals(null, s.getName());

    s.setName("toto");
    assertEquals("toto", s.getName());

    s.setName("");
    assertEquals("", s.getName());

    s.setName("titi");
    assertEquals("titi", s.getName());

    s.setName(null);
    assertEquals(null, s.getName());

    assertTrue(s.setNameWithValidation("toto"));
    assertEquals("toto", s.getName());

    assertFalse(s.setNameWithValidation(""));
    assertEquals("", s.getName());

    assertTrue(s.setNameWithValidation("titi"));
    assertEquals("titi", s.getName());

    assertFalse(s.setNameWithValidation(null));
    assertEquals(null, s.getName());
  }

  @Test
  public void testGetSetDescription() {

    Sequence s = new Sequence();
    assertNull(s.getDescription());

    s.setDescription("My description");
    assertEquals("My description", s.getDescription());

    s.setDescription("");
    assertEquals("", s.getDescription());

    s.setDescription("My description2");
    assertEquals("My description2", s.getDescription());

    s.setDescription(null);
    assertNull(s.getDescription());
  }

  @Test
  public void testGetSetAlphabet() {

    Sequence s = new Sequence();
    assertNotNull(s.getAlphabet());
    assertEquals(Alphabets.AMBIGUOUS_DNA_ALPHABET, s.getAlphabet());

    s.setAlphabet(Alphabets.UNAMBIGUOUS_DNA_ALPHABET);
    assertEquals(Alphabets.UNAMBIGUOUS_DNA_ALPHABET, s.getAlphabet());

    try {
      s.setAlphabet(null);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(true);
    }
    assertEquals(Alphabets.UNAMBIGUOUS_DNA_ALPHABET, s.getAlphabet());
    
    // Test alphabets
    
//    'G', 'A', 'T', 'C', 'R', 'Y', 'W', 'S', 'M', 'K', 'H',
//    'B', 'V', 'D', 'N'
    
    // 'G', 'A', 'T', 'C'
    
    // 'G', 'A', 'U', 'C', 'R', 'Y', 'W', 'S', 'M', 'K', 'H',
    // 'B', 'V', 'D', 'N'
    
    // 'G', 'A', 'U', 'C'
    
    // 'G', 'A', 'T', 'C', 'N'

  }

  @Test
  public void testGetSequence() {

    Sequence s = new Sequence();
    assertNull(s.getSequence());

    s.setSequence("ATGC");
    assertEquals("ATGC", s.getSequence());

    s.setSequence("");
    assertEquals("", s.getSequence());

    s.setSequence("===");
    assertEquals("===", s.getSequence());

    s.setSequence("ATGCTT");
    assertEquals("ATGCTT", s.getSequence());

    s.setSequence(null);
    assertNull(s.getSequence());

    assertTrue(s.setSequenceWithValidation("ATGC"));
    assertEquals("ATGC", s.getSequence());

    assertFalse(s.setSequenceWithValidation(""));
    assertEquals("", s.getSequence());

    assertFalse(s.setSequenceWithValidation("==="));
    assertEquals("===", s.getSequence());

    assertTrue(s.setSequenceWithValidation("ATGCTT"));
    assertEquals("ATGCTT", s.getSequence());

    assertFalse(s.setSequenceWithValidation(null));
    assertNull(s.getSequence());    
  }

  @Test
  public void testSet() {
    fail("Not yet implemented");
  }

  @Test
  public void testLength() {
    fail("Not yet implemented");
  }

  @Test
  public void testSubSequence() {
    fail("Not yet implemented");
  }

  @Test
  public void testConcat() {
    fail("Not yet implemented");
  }

  @Test
  public void testCountSequenceSequence() {
    fail("Not yet implemented");
  }

  @Test
  public void testCountSequenceString() {
    fail("Not yet implemented");
  }

  @Test
  public void testReverseComplement() {
    fail("Not yet implemented");
  }

  @Test
  public void testReverseComplementString() {
    fail("Not yet implemented");
  }

  @Test
  public void testToFasta() {
    fail("Not yet implemented");
  }

  @Test
  public void testToFastaInt() {
    fail("Not yet implemented");
  }

  @Test
  public void testParseFasta() {
    fail("Not yet implemented");
  }

  @Test
  public void testValidate() {
    fail("Not yet implemented");
  }

  @Test
  public void testEqualsObject() {
    fail("Not yet implemented");
  }

  @Test
  public void testToString() {
    fail("Not yet implemented");
  }

}
