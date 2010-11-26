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

package fr.ens.transcriptome.eoulsan.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeDebug;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.protocols.DataProtocolService;

public class DataFileTest {

  @Before
  public void setUp() throws Exception {

    EoulsanRuntimeDebug.initDebugEoulsanRuntime();
  }

  @Test
  public void testHashCode() {

    String filename = "toto.txt";
    DataFile df = new DataFile(filename);
    assertEquals(filename.hashCode(), df.hashCode());

    filename = "/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename.hashCode(), df.hashCode());

    filename = "file:///home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename.hashCode(), df.hashCode());

    filename = "file:/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename.hashCode(), df.hashCode());

    filename = "http://www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename.hashCode(), df.hashCode());

    filename = "http:/www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename.hashCode(), df.hashCode());

    filename = "ftp://ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename.hashCode(), df.hashCode());

    filename = "ftp:/ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename.hashCode(), df.hashCode());

    filename = "ftp://login:passwd@ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename.hashCode(), df.hashCode());

  }

  @Test
  public void testGetSource() {

    String filename = "toto.txt";
    DataFile df = new DataFile(filename);
    assertEquals(filename, df.getSource());

    filename = "/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename, df.getSource());

    filename = "file:///home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename, df.getSource());

    filename = "file:/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename, df.getSource());

    filename = "http://www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename, df.getSource());

    filename = "http:/www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename, df.getSource());

    filename = "ftp://ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename, df.getSource());

    filename = "ftp:/ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename, df.getSource());

    filename = "ftp://login:passwd@ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename, df.getSource());

  }

  @Test
  public void testGetName() {

    String filename = "toto.txt";
    DataFile df = new DataFile(filename);
    assertEquals("toto.txt", df.getName());

    filename = "/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("toto.txt", df.getName());

    filename = "file:///home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("toto.txt", df.getName());

    filename = "file:/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("toto.txt", df.getName());

    filename = "http://www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("toto.txt", df.getName());

    filename = "http:/www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("toto.txt", df.getName());

    filename = "ftp://ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("toto.txt", df.getName());

    filename = "ftp:/ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("toto.txt", df.getName());

    filename = "ftp://login:passwd@ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("toto.txt", df.getName());
  }

  
  @Test
  public void testGetSourceWithoutExtension() {
    
    String filename = "toto.txt";
    DataFile df = new DataFile(filename);
    assertEquals("toto", df.getSourceWithoutExtension());

    filename = "/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("/home/toto/toto", df.getSourceWithoutExtension());

    filename = "file:///home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("file:///home/toto/toto", df.getSourceWithoutExtension());

    filename = "file:/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("file:/home/toto/toto", df.getSourceWithoutExtension());

    filename = "http://www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("http://www.toto.com/home/toto/toto", df.getSourceWithoutExtension());

    filename = "http:/www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("http:/www.toto.com/home/toto/toto", df.getSourceWithoutExtension());

    filename = "ftp://ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("ftp://ftp.toto.com/home/toto/toto", df.getSourceWithoutExtension());

    filename = "ftp:/ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("ftp:/ftp.toto.com/home/toto/toto", df.getSourceWithoutExtension());

    filename = "ftp://login:passwd@ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("ftp://login:passwd@ftp.toto.com/home/toto/toto", df.getSourceWithoutExtension());
    
    filename = "ftp://login:passwd@ftp.toto.com/home/titi/toto";
    df = new DataFile(filename);
    assertEquals("ftp://login:passwd@ftp.toto.com/home/titi/toto", df.getSourceWithoutExtension());
    
    filename = "ftp://login:passwd@ftp.toto.com/home/titi/";
    df = new DataFile(filename);
    assertEquals("ftp://login:passwd@ftp.toto.com/home/titi/", df.getSourceWithoutExtension());
    
  }
  
  @Test
  public void testGetProtocol() throws IOException {

    DataProtocolService registry = DataProtocolService.getInstance();

    assertTrue(registry.isProtocol("file"));
    assertTrue(registry.isProtocol("http"));
    assertTrue(registry.isProtocol("ftp"));

    String filename = "toto.txt";
    DataFile df = new DataFile(filename);
    assertEquals(registry.getProtocol("file"), df.getProtocol());

    filename = "/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(registry.getProtocol("file"), df.getProtocol());

    filename = "file:///home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(registry.getProtocol("file"), df.getProtocol());

    filename = "file:/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(registry.getProtocol("file"), df.getProtocol());

    filename = "http://www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(registry.getProtocol("http"), df.getProtocol());

    filename = "http:/www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(registry.getProtocol("http"), df.getProtocol());

    try {
      new DataFile("toto://www.toto.com/home/toto/toto.txt").getProtocol();
      assertTrue(false);
    } catch (IOException e) {
      assertTrue(true);
    }

  }

  @Test
  public void testToString() {

    String filename = "toto.txt";
    DataFile df = new DataFile(filename);
    assertEquals(filename, df.toString());

    filename = "/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename, df.toString());

    filename = "file:///home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename, df.toString());

    filename = "file:/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename, df.toString());

    filename = "http://www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename, df.toString());

    filename = "http:/www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename, df.toString());

    filename = "ftp://ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename, df.toString());

    filename = "ftp:/ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename, df.toString());

    filename = "ftp://login:passwd@ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(filename, df.toString());
  }

  @Test
  public void testEqualsObject() {

    String filename = "toto.txt";
    DataFile df1 = new DataFile(filename);
    DataFile df2 = new DataFile(filename);
    assertEquals(df1, df2);

    filename = "/home/toto/toto.txt";
    df1 = new DataFile(filename);
    df2 = new DataFile(filename);
    assertEquals(df1, df2);

    filename = "file:///home/toto/toto.txt";
    df1 = new DataFile(filename);
    df2 = new DataFile(filename);
    assertEquals(df1, df2);

    df1 = new DataFile("file:/home/toto/toto.txt");
    df2 = new DataFile("file:///home/toto/toto.txt");
    assertNotSame(df1, df2);
    assertNotSame(df1, "file:/home/toto/toto.txt");

  }

  @Test
  public void testDataFile() {

    try {
      new DataFile(null);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    new DataFile("toto:/www.toto.com/home/toto/toto.txt");
    assertTrue(true);

  }

}
