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
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
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

    assertEquals("toto\t", StringUtils.deprotectGFF("toto%09"));
    assertEquals("toto ", StringUtils.deprotectGFF("toto%20"));
    assertEquals("toto titi", StringUtils.deprotectGFF("toto%20titi"));
    assertEquals("t\\oto titi", StringUtils.deprotectGFF("t\\\\oto%20titi"));
    assertEquals("toto;titi", StringUtils.deprotectGFF("toto\\;titi"));
    assertEquals("toto=titi", StringUtils.deprotectGFF("toto\\=titi"));
    assertEquals("toto%titi", StringUtils.deprotectGFF("toto\\%titi"));
    assertEquals("toto&titi", StringUtils.deprotectGFF("toto\\&titi"));
    assertEquals("toto,titi", StringUtils.deprotectGFF("toto\\,titi"));
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
        StringUtils.serializeStringArray(Arrays.asList(new String[] {})));
    assertEquals("[]",
        StringUtils.serializeStringArray(Arrays.asList(new String[] {""})));
    assertEquals("[toto]",
        StringUtils.serializeStringArray(Arrays.asList(new String[] {"toto"})));
    assertEquals("[toto,titi]",
        StringUtils.serializeStringArray(Arrays.asList(new String[] {"toto","titi"})));
    assertEquals("[to\\,to]",
        StringUtils.serializeStringArray(Arrays.asList(new String[] {"to,to"})));
    assertEquals("[to\\\\to]",
        StringUtils.serializeStringArray(Arrays.asList(new String[] {"to\\to"})));    
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

}
