/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.util;

import static org.junit.Assert.*;

import org.junit.Test;

import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class StringUtilsTest {

  @Test
  public void testBasename() {
    
    assertEquals("toto", StringUtils.basename("toto.tar.gz"));
  }

  @Test
  public void testExtension() {
    
    assertEquals("toto", StringUtils.basename("toto.tar.gz"));
  }

//  @Test
//  public void testRemoveNonAlphaAtEndOfString() {
//    fail("Not yet implemented");
//  }
//
//  @Test
//  public void testToTimeHumanReadable() {
//    fail("Not yet implemented");
//  }
//
//  @Test
//  public void testFastSplitStringListOfString() {
//    fail("Not yet implemented");
//  }
//
//  @Test
//  public void testFastSplitStringStringArray() {
//    fail("Not yet implemented");
//  }

  @Test
  public void testSubStringAfterFirstTab() {
    assertEquals("titi\ttata", StringUtils.subStringAfterFirstTab("toto\ttiti\ttata"));
    assertEquals("toto\ttiti\ttata", StringUtils.subStringAfterFirstTab("\ttoto\ttiti\ttata"));
    assertEquals("toto", StringUtils.subStringAfterFirstTab("toto"));
    assertEquals("toto", StringUtils.subStringAfterFirstTab("toto"));
  }

  @Test
  public void testSubStringBeforeFirstTab() {
    assertEquals("toto", StringUtils.subStringBeforeFirstTab("toto\ttiti\ttata"));
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
    
    System.out.println((char) Integer.parseInt("" + '5' + 'F', 16));
    
    assertEquals("toto", StringUtils.deprotectGFF("toto"));
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
  

}
