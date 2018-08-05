/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.bio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SequenceTest {

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
  public void testGetIdentifierInName() {

    Sequence s = new Sequence();
    assertEquals(null, s.getIdentifierInName());

    s.setName("");
    assertEquals("", s.getIdentifierInName());

    s.setName("titi");
    assertEquals("titi", s.getIdentifierInName());

    s.setName("titi toto");
    assertEquals("titi", s.getIdentifierInName());

    s.setName("titi ");
    assertEquals("titi", s.getIdentifierInName());

    s.setName("titi ");
    assertEquals("titi", s.getIdentifierInName());
  }

  @Test
  public void testGetDescriptionInName() {

    Sequence s = new Sequence();
    assertEquals(null, s.getDescriptionInName());

    s.setName("");
    assertEquals("", s.getDescriptionInName());

    s.setName("");
    assertEquals("", s.getDescriptionInName());

    s.setName("titi toto");
    assertEquals("toto", s.getDescriptionInName());

    s.setName(" titi toto ");
    assertEquals("toto", s.getDescriptionInName());

    s.setName(" titi  toto ");
    assertEquals("toto", s.getDescriptionInName());

    s.setName("titi");
    assertEquals("", s.getDescriptionInName());

    s.setName("titi ");
    assertEquals("", s.getDescriptionInName());

    s.setName(" titi ");
    assertEquals("", s.getDescriptionInName());
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

    Sequence s1 = new Sequence();
    assertNull(s1.getName());
    assertNull(s1.getDescription());
    assertEquals(Alphabets.AMBIGUOUS_DNA_ALPHABET, s1.getAlphabet());
    assertNull(s1.getSequence());

    Sequence s2 = new Sequence("toto", "ATGC", "test sequence");
    s2.setAlphabet(Alphabets.UNAMBIGUOUS_DNA_ALPHABET);
    assertEquals("toto", s2.getName());
    assertEquals("test sequence", s2.getDescription());
    assertEquals(Alphabets.UNAMBIGUOUS_DNA_ALPHABET, s2.getAlphabet());
    assertEquals("ATGC", s2.getSequence());

    // Test if there is no change in s1
    assertNull(s1.getName());
    assertNull(s1.getDescription());
    assertEquals(Alphabets.AMBIGUOUS_DNA_ALPHABET, s1.getAlphabet());
    assertNull(s1.getSequence());

    s1.set(s2);
    assertEquals("toto", s1.getName());
    assertEquals("test sequence", s1.getDescription());
    assertEquals(Alphabets.UNAMBIGUOUS_DNA_ALPHABET, s1.getAlphabet());
    assertEquals("ATGC", s1.getSequence());

    Sequence s3 = new Sequence(s2);
    assertEquals("toto", s3.getName());
    assertEquals("test sequence", s3.getDescription());
    assertEquals(Alphabets.UNAMBIGUOUS_DNA_ALPHABET, s3.getAlphabet());
    assertEquals("ATGC", s3.getSequence());

    try {
      s1.set(null);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    try {
      new Sequence(null);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(true);
    }
  }

  @Test
  public void testLength() {

    Sequence s = new Sequence();
    assertEquals(0, s.length());

    s.setSequence("ATGC");
    assertEquals(4, s.length());
    s.setSequence("ATGCATGC");
    assertEquals(8, s.length());
  }

  @Test
  public void testSubSequence() {

    Sequence s1 = new Sequence("toto", "ATGC");

    try {
      s1.subSequence(-1, 2);
      assertTrue(false);
    } catch (StringIndexOutOfBoundsException e) {
      assertTrue(true);
    }

    try {
      s1.subSequence(0, 5);
      assertTrue(false);
    } catch (StringIndexOutOfBoundsException e) {
      assertTrue(true);
    }

    try {
      s1.subSequence(2, 1);
      assertTrue(false);
    } catch (StringIndexOutOfBoundsException e) {
      assertTrue(true);
    }

    Sequence s2 = s1.subSequence(0, 4);
    assertEquals("ATGC", s2.getSequence());

    s2 = s1.subSequence(1, 4);
    assertEquals("TGC", s2.getSequence());
    assertEquals("toto[part]", s2.getName());

    s1.setName(null);
    s2 = s1.subSequence(1, 4);
    assertNull(s2.getName());

    s1.setSequence(null);
    assertNull(s1.subSequence(1, 4));

  }

  @Test
  public void testConcat() {

    Sequence s1 = new Sequence("toto", "AATT");
    Sequence s2 = new Sequence("titi", "GGCC");

    Sequence s3 = s1.concat(s2);
    assertEquals("AATTGGCC", s3.getSequence());
    assertEquals("toto[merged]", s3.getName());

    s1.setSequence(null);
    s3 = s1.concat(s2);
    assertEquals("GGCC", s3.getSequence());

    s1.setSequence("AATT");
    s2.setSequence(null);
    s1.setAlphabet(Alphabets.UNAMBIGUOUS_DNA_ALPHABET);
    s3 = s1.concat(s2);
    assertEquals("AATT", s3.getSequence());
    assertEquals(Alphabets.UNAMBIGUOUS_DNA_ALPHABET, s3.getAlphabet());

    s3 = s1.concat(null);
    assertEquals(s1.getName(), s3.getName());
    assertEquals(s1.getAlphabet(), s3.getAlphabet());
    assertEquals(s1.getSequence(), s3.getSequence());
    assertFalse(s1 == s3);
  }

  @Test
  public void testCountSequenceSequence() {

    Sequence s1 = new Sequence("toto", "AATTGGTT");
    Sequence s2 = new Sequence("titi", "TT");

    assertEquals(2, s1.countSequence(s2));
    s2 = new Sequence("titi", "AA");
    assertEquals(1, s1.countSequence(s2));
    s2 = new Sequence("titi", "GG");
    assertEquals(1, s1.countSequence(s2));
    s2 = new Sequence("titi", "CC");
    assertEquals(0, s1.countSequence(s2));

    assertEquals(0, s1.countSequence((Sequence) null));
  }

  @Test
  public void testCountSequenceString() {

    Sequence s = new Sequence("toto", "AATTGGTT");
    assertEquals(2, s.countSequence("A"));
    assertEquals(1, s.countSequence("AA"));
    assertEquals(4, s.countSequence("T"));
    assertEquals(2, s.countSequence("TT"));
    assertEquals(0, s.countSequence("C"));
    assertEquals(0, s.countSequence(""));
    assertEquals(0, s.countSequence((String) null));

    s = new Sequence("toto", null);
    assertEquals(0, s.countSequence("TT"));

    s = new Sequence("toto", "");
    assertEquals(0, s.countSequence("TT"));

    s = new Sequence("toto", "AATTTGGTT");
    assertEquals(2, s.countSequence("TT"));

    s = new Sequence("toto", "AATTTTGGTT");
    assertEquals(3, s.countSequence("TT"));

  }

  @Test
  public void testGettm() {

    Sequence s = new Sequence("toto", "AATTGGTT");
    assertEquals(s.getTm(50.0f, 50.0f), s.getTm(), 0.1);
  }

  @Test
  public void testGetGCPercent() {

    Sequence s = new Sequence("toto", "AATTGGTT");
    assertEquals(2.0 / 8.0, s.getGCPercent(), 0.1);

    s = new Sequence("toto", "");
    assertEquals(Double.NaN, s.getGCPercent(), 0.1);

    s = new Sequence("toto", null);
    assertEquals(Double.NaN, s.getGCPercent(), 0.1);

    s = new Sequence("toto", "AATTAATT");
    assertEquals(0.0 / 8.0, s.getGCPercent(), 0.1);

    s = new Sequence("toto", "AATTGGCC");
    assertEquals(4.0 / 8.0, s.getGCPercent(), 0.1);
  }

  @Test
  public void testReverse() {

    Sequence s = new Sequence("toto", "ATGC");
    assertEquals("ATGC", s.getSequence());
    s.reverse();
    assertEquals("CGTA", s.getSequence());

    s = new Sequence("toto", null);
    assertNull(s.getSequence());
  }

  @Test
  public void testReverseString() {

    assertNull(Sequence.reverse(null));

    assertEquals("CGTA", Sequence.reverse("ATGC"));
  }

  @Test
  public void testComplement() {

    Sequence s = new Sequence("toto", "ATGC");
    assertEquals("ATGC", s.getSequence());
    s.complement();
    assertEquals("TACG", s.getSequence());

    s = new Sequence("toto", null);
    assertNull(s.getSequence());
  }

  @Test
  public void testComplementString() {

    assertNull(Sequence.complement(null, Alphabets.AMBIGUOUS_DNA_ALPHABET));
    assertNull(Sequence.complement("ATGC", null));

    assertEquals("GCAT",
        Sequence.complement("CGTA", Alphabets.AMBIGUOUS_DNA_ALPHABET));
  }

  @Test
  public void testReverseComplement() {

    Sequence s = new Sequence("toto", "ATGC");
    assertEquals("ATGC", s.getSequence());
    s.reverseComplement();
    assertEquals("GCAT", s.getSequence());

    s = new Sequence("toto", null);
    assertNull(s.getSequence());
  }

  @Test
  public void testReverseComplementString() {

    assertNull(
        Sequence.reverseComplement(null, Alphabets.AMBIGUOUS_DNA_ALPHABET));
    assertNull(Sequence.reverseComplement("ATGC", null));

    assertEquals("GCAT",
        Sequence.reverseComplement("ATGC", Alphabets.AMBIGUOUS_DNA_ALPHABET));
  }

  @Test
  public void testToFasta() {

    Sequence s = new Sequence("toto", "ATGC");
    assertEquals(">toto\nATGC", s.toFasta());
    s = new Sequence(null, "ATGC");
    assertEquals(">\nATGC", s.toFasta());
    s = new Sequence("toto", null);
    assertEquals(">toto\n", s.toFasta());

  }

  @Test
  public void testToFastaInt() {

    Sequence s = new Sequence("toto", "ATGC");
    assertEquals(">toto\nATGC", s.toFasta(0));
    assertEquals(">toto\nATGC", s.toFasta(60));

    s = new Sequence("toto", "ATGCATGCAT");
    assertEquals(">toto\nATGCA\nTGCAT\n", s.toFasta(5));
    assertEquals(">toto\nATGCAT\nGCAT", s.toFasta(6));
  }

  @Test
  public void testParseFasta() {

    Sequence s = new Sequence();
    s.parseFasta(">toto\nATGCA\nTGCAT\n");
    assertEquals("toto", s.getName());
    assertEquals("ATGCATGCAT", s.getSequence());
    s.parseFasta(null);
    assertNull(s.getName());
    assertNull(s.getSequence());

    s.parseFasta("toto\nATGCA\nTGCAT\n");
    assertNull(s.getName());
    assertNull(s.getSequence());

    s.parseFasta(">toto");
    assertNull(s.getName());
    assertNull(s.getSequence());

    s.parseFasta(">toto\n");
    assertNull(s.getName());
    assertNull(s.getSequence());

    s.parseFasta(">toto\n ATGCA \n  \n TGCAT \n");
    assertEquals("toto", s.getName());
    assertEquals("ATGCATGCAT", s.getSequence());

    s.parseFasta("toto\nATGCA\n>TGCAT\n");
    assertNull(s.getName());
    assertNull(s.getSequence());

    s.parseFasta("");
    assertNull(s.getName());
    assertNull(s.getSequence());

    s.parseFasta(">toto\n>ATGCA\n>TGCAT\n");
    assertNull(s.getName());
    assertNull(s.getSequence());
  }

  @Test
  public void testValidate() {

    Sequence s = new Sequence("toto", "ATGC");
    assertTrue(s.validate());

    s = new Sequence("toto", "ATGC");
    assertTrue(s.validate());

    s = new Sequence("toto", "A#GC");
    assertFalse(s.validate());
    s = new Sequence("toto", null);
    assertFalse(s.validate());
    s = new Sequence(null, "ATGC");
    assertFalse(s.validate());
    s = new Sequence("toto", "");
    assertFalse(s.validate());
  }

  @Test
  public void testEqualsObject() {

    Sequence s1 = new Sequence("toto", "ATGC", "desc");
    Sequence s2 = new Sequence("toto", "ATGC", "desc");
    Sequence s3 = new Sequence("titi", "ATGC", "desc");

    assertTrue(s1.equals(s1));
    assertFalse(s1.equals(null));
    assertFalse(s1.getSequence().equals("titit"));

    assertTrue(s1.equals(s1));
    assertEquals(s1, s2);
    assertFalse(s1 == s2);
    assertNotSame(s1, s3);

    s3.setName("toto");
    assertTrue(s1.equals(s3));
    s3.setName("titi");
    assertFalse(s1.equals(s3));
    s3.setName("toto");
    assertTrue(s1.equals(s3));

    s3.setDescription("other desc");
    assertFalse(s1.equals(s3));
    assertTrue(s1.equals(s2));
    s3.setDescription("desc");
    assertTrue(s1.equals(s3));

    s2.setAlphabet(Alphabets.UNAMBIGUOUS_DNA_ALPHABET);
    assertFalse(s1.equals(s2));

    s2.setAlphabet(Alphabets.AMBIGUOUS_DNA_ALPHABET);
    assertTrue(s1.equals(s2));

    s2.setSequence("AAAA");
    assertFalse(s1.equals(s2));
    s2.setSequence("ATGC");
    assertTrue(s1.equals(s2));
  }

  @Test
  public void testHashCode() {

    Sequence s1 = new Sequence("toto", "ATGC", "desc");
    Sequence s2 = new Sequence("toto", "ATGC", "desc");
    Sequence s3 = new Sequence("titi", "ATGC", "desc");

    assertEquals(s1.hashCode(), s2.hashCode());
    assertNotSame(s1.hashCode(), s3.hashCode());

    s3.setName("toto");
    assertEquals(s1.hashCode(), s3.hashCode());

    s3.setDescription("other desc");
    assertNotSame(s1.hashCode(), s3.hashCode());
    assertEquals(s1.hashCode(), s2.hashCode());

    s2.setAlphabet(Alphabets.UNAMBIGUOUS_DNA_ALPHABET);
    assertNotSame(s1.hashCode(), s2.hashCode());

    s2.setAlphabet(Alphabets.AMBIGUOUS_DNA_ALPHABET);
    assertEquals(s1.hashCode(), s2.hashCode());

    assertNotSame(s1.hashCode(), s2.hashCode());
  }

  @Test
  public void testToString() {

    Sequence s = new Sequence("toto", "ATGC", "desc");
    assertEquals(
        "Sequence{name=toto, description=desc, alphabet=AmbiguousDNA, sequence=ATGC}",
        s.toString());

  }

}
