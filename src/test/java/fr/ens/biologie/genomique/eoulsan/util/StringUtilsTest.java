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

package fr.ens.biologie.genomique.eoulsan.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class StringUtilsTest {

  @Test
  public void testBasename() {

    assertEquals("toto", StringUtils.basename("toto.tar.gz"));
  }

  @Test
  public void testExtension() {

    assertEquals("toto", StringUtils.basename("toto.tar.gz"));
  }

  @Test
  public void testCompressionExtension() {

    assertEquals(".gz", StringUtils.compressionExtension("toto.tar.gz"));
    assertEquals(".bz2", StringUtils.compressionExtension("toto.tar.bz2"));
    assertEquals(".zip", StringUtils.compressionExtension("toto.tar.zip"));
    assertEquals(".lzo", StringUtils.compressionExtension("toto.tar.lzo"));
    assertEquals(".deflate",
        StringUtils.compressionExtension("toto.tar.deflate"));
    assertEquals("", StringUtils.compressionExtension("toto.tar"));
    assertEquals("", StringUtils.compressionExtension("toto"));
  }

  @Test
  public void testfilenameWithoutCompressionExtension() {

    assertEquals("toto.tar",
        StringUtils.filenameWithoutCompressionExtension("toto.tar.gz"));
    assertEquals("toto.tar",
        StringUtils.filenameWithoutCompressionExtension("toto.tar"));
    assertEquals("toto",
        StringUtils.filenameWithoutCompressionExtension("toto"));
  }

  // @Test
  // public void testRemoveNonAlphaAtEndOfString() {
  // fail("Not yet implemented");
  // }
  //
  // @Test
  // public void testToTimeHumanReadable() {
  // fail("Not yet implemented");
  // }
  //
  // @Test
  // public void testFastSplitStringListOfString() {
  // fail("Not yet implemented");
  // }
  //
  // @Test
  // public void testFastSplitStringStringArray() {
  // fail("Not yet implemented");
  // }

  @Test
  public void testSubStringAfterFirstTab() {
    assertEquals("titi\ttata",
        StringUtils.subStringAfterFirstTab("toto\ttiti\ttata"));
    assertEquals("toto\ttiti\ttata",
        StringUtils.subStringAfterFirstTab("\ttoto\ttiti\ttata"));
    assertEquals("toto", StringUtils.subStringAfterFirstTab("toto"));
    assertEquals("toto", StringUtils.subStringAfterFirstTab("toto"));
  }

  @Test
  public void testSubStringBeforeFirstTab() {
    assertEquals("toto",
        StringUtils.subStringBeforeFirstTab("toto\ttiti\ttata"));
    assertEquals("toto", StringUtils.subStringBeforeFirstTab("toto"));
  }

  @Test
  public void testProtectGFF() {

    assertEquals("toto", StringUtils.protectGFF("toto"));
    assertEquals("toto%09", StringUtils.protectGFF("toto\t"));
    assertEquals("toto%20", StringUtils.protectGFF("toto "));
    assertEquals("toto%20titi", StringUtils.protectGFF("toto titi"));
    assertEquals("t\\\\oto%20titi", StringUtils.protectGFF("t\\oto titi"));
    assertEquals("toto\\;titi", StringUtils.protectGFF("toto;titi"));
    assertEquals("toto\\=titi", StringUtils.protectGFF("toto=titi"));
    assertEquals("toto\\%titi", StringUtils.protectGFF("toto%titi"));
    assertEquals("toto\\&titi", StringUtils.protectGFF("toto&titi"));
    assertEquals("toto\\,titi", StringUtils.protectGFF("toto,titi"));
  }

  @Test
  public void testDeprotectGFF() {

    assertEquals("toto\t", StringUtils.deProtectGFF("toto%09"));
    assertEquals("toto ", StringUtils.deProtectGFF("toto%20"));
    assertEquals("toto titi", StringUtils.deProtectGFF("toto%20titi"));
    assertEquals("t\\oto titi", StringUtils.deProtectGFF("t\\\\oto%20titi"));
    assertEquals("toto;titi", StringUtils.deProtectGFF("toto\\;titi"));
    assertEquals("toto=titi", StringUtils.deProtectGFF("toto\\=titi"));
    assertEquals("toto%titi", StringUtils.deProtectGFF("toto\\%titi"));
    assertEquals("toto&titi", StringUtils.deProtectGFF("toto\\&titi"));
    assertEquals("toto,titi", StringUtils.deProtectGFF("toto\\,titi"));
  }

  @Test
  public void testReplacePrefix() {

    assertNull(StringUtils.replacePrefix(null, "toto", "titi"));
    assertEquals("ticoucou",
        StringUtils.replacePrefix("totocoucou", "toto", "ti"));
    assertEquals("titicoucou",
        StringUtils.replacePrefix("totocoucou", "toto", "titi"));
    assertEquals("coucou", StringUtils.replacePrefix("totocoucou", "toto", ""));
    assertEquals("tititotocoucou",
        StringUtils.replacePrefix("totocoucou", "", "titi"));
    assertEquals("s3n://sgdb-test/titi.txt",
        StringUtils.replacePrefix("s3://sgdb-test/titi.txt", "s3:/", "s3n:/"));
  }

  @Test
  public void testSerializeStringArray() {

    assertEquals("[]",
        StringUtils.serializeStringArray(Collections.emptyList()));
    assertEquals("[]",
        StringUtils.serializeStringArray(Collections.singletonList("")));
    assertEquals("[toto]",
        StringUtils.serializeStringArray(Collections.singletonList("toto")));
    assertEquals("[toto,titi]",
        StringUtils.serializeStringArray(Arrays.asList("toto", "titi")));
    assertEquals("[to\\,to]",
        StringUtils.serializeStringArray(Collections.singletonList("to,to")));
    assertEquals("[to\\\\to]",
        StringUtils.serializeStringArray(Collections.singletonList("to\\to")));
  }

  @Test
  public void testDeserializeStringArray() {

    List<String> r = StringUtils.deserializeStringArray("[]");
    assertEquals(1, r.size());
    assertEquals("", r.get(0));

    r = StringUtils.deserializeStringArray(" []   ");
    assertEquals(1, r.size());
    assertEquals("", r.get(0));

    r = StringUtils.deserializeStringArray("[toto]");
    assertEquals(1, r.size());
    assertEquals("toto", r.get(0));

    r = StringUtils.deserializeStringArray("[toto,]");
    assertEquals(2, r.size());
    assertEquals("toto", r.get(0));
    assertEquals("", r.get(1));

    r = StringUtils.deserializeStringArray("[,toto]");
    assertEquals(2, r.size());
    assertEquals("", r.get(0));
    assertEquals("toto", r.get(1));

    r = StringUtils.deserializeStringArray("[tata,titi,toto]");
    assertEquals(3, r.size());
    assertEquals("tata", r.get(0));
    assertEquals("titi", r.get(1));
    assertEquals("toto", r.get(2));

    r = StringUtils.deserializeStringArray("[tata\\,,ti\\,ti,to\\to]");
    assertEquals(3, r.size());
    assertEquals("tata,", r.get(0));
    assertEquals("ti,ti", r.get(1));
    assertEquals("to\\to", r.get(2));
  }

  @Test
  public void testToLetter() {

    assertEquals('-', StringUtils.toLetter(-1));
    assertEquals('a', StringUtils.toLetter(0));
    assertEquals('b', StringUtils.toLetter(1));
    assertEquals('c', StringUtils.toLetter(2));
    assertEquals('z', StringUtils.toLetter(25));
    assertEquals('-', StringUtils.toLetter(26));
  }

  @Test
  public void testSplitStringIterator() {

    String s = "12345678901234567890";

    for (String split : StringUtils.splitStringIterator(s, 30)) {
      assertEquals("12345678901234567890", split);
    }

    for (String split : StringUtils.splitStringIterator(s, 10)) {
      assertEquals("1234567890", split);
    }

    int i = 0;
    for (String split : StringUtils.splitStringIterator(s, 11)) {

      if (i == 0) {
        assertEquals("12345678901", split);
      } else if (i == 1) {
        assertEquals("234567890", split);
      }

      i++;
    }

    i = 0;
    for (String split : StringUtils.splitStringIterator(s, 5)) {

      if (i == 0 || i == 2) {
        assertEquals("12345", split);
      } else if (i == 1 || i == 3) {
        assertEquals("67890", split);
      }

      i++;
    }

    i = 0;
    for (String split : StringUtils.splitStringIterator(s, 3)) {

      if (i == 0) {
        assertEquals("123", split);
      } else if (i == 1) {
        assertEquals("456", split);
      } else if (i == 2) {
        assertEquals("789", split);
      } else if (i == 3) {
        assertEquals("012", split);
      } else if (i == 4) {
        assertEquals("345", split);
      } else if (i == 5) {
        assertEquals("678", split);
      } else if (i == 6) {
        assertEquals("90", split);
      } else {
        fail();
      }

      i++;
    }

  }

  @Test
  public void testSplitShellCommandLine() {

    assertNull(StringUtils.splitShellCommandLine(null));
    assertTrue(StringUtils.splitShellCommandLine("").isEmpty());
    assertEquals(asList("titi", "toto", "tata"),
        StringUtils.splitShellCommandLine("titi toto tata"));
    assertEquals(asList("titi", "toto", "tata"),
        StringUtils.splitShellCommandLine(" titi  toto  tata "));
    assertEquals(asList("titi", "toto", "tata"),
        StringUtils.splitShellCommandLine(" titi  \"toto\"  tata "));
    assertEquals(asList("titi", "toto tata"),
        StringUtils.splitShellCommandLine(" titi  \"toto tata\""));
    assertEquals(asList("titi", " toto ", "tata"),
        StringUtils.splitShellCommandLine(" titi  \" toto \" tata\""));
    assertEquals(asList("titi", "toto", "tata"),
        StringUtils.splitShellCommandLine(" titi  toto  \"tata"));
    assertEquals(asList("titi", "toto", "tata"),
        StringUtils.splitShellCommandLine(" titi  toto  \"tata\""));
    assertEquals(asList("titi", "toto", "tata"),
        StringUtils.splitShellCommandLine("\"titi\"  toto  \"tata\""));
    assertEquals(asList("titi", "toto", "tata"),
        StringUtils.splitShellCommandLine("\'titi\'  toto  \'tata\'"));
    assertEquals(asList("titi", "toto tata"),
        StringUtils.splitShellCommandLine(" titi  \'toto tata\'"));
    assertEquals(asList("titi", "toto\"tata"),
        StringUtils.splitShellCommandLine("titi  \'toto\"tata\'"));
    assertEquals(asList("titi", "toto\'tata"),
        StringUtils.splitShellCommandLine("titi  \"toto\'tata\""));
    assertEquals(asList("titi", "to\"to", "ta\'ta"),
        StringUtils.splitShellCommandLine(" titi  \'to\"to\'  \"ta\'ta\" "));
  }

  public void testDoubleQuotes() {

    assertNull(StringUtils.doubleQuotes(null));
    assertEquals("\"\"", StringUtils.doubleQuotes(""));
    assertEquals("\"toto\"", StringUtils.doubleQuotes("toto"));
  }

  @Test
  public void testUnDoubleQuotes() {

    assertNull(StringUtils.unDoubleQuotes(null));
    assertEquals("", StringUtils.unDoubleQuotes("\"\""));
    assertEquals("toto", StringUtils.unDoubleQuotes("\"toto\""));
    assertEquals("toto\"", StringUtils.unDoubleQuotes("toto\""));
    assertEquals("\"toto", StringUtils.unDoubleQuotes("\"toto"));
    assertEquals("", StringUtils.unDoubleQuotes(""));
    assertEquals("a", StringUtils.unDoubleQuotes("a"));
    assertEquals("ab", StringUtils.unDoubleQuotes("ab"));
    assertEquals("abc", StringUtils.unDoubleQuotes("abc"));
  }

}
