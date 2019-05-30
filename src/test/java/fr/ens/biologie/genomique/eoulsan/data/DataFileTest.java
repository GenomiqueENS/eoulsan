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

package fr.ens.biologie.genomique.eoulsan.data;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeDebug;
import fr.ens.biologie.genomique.eoulsan.data.protocols.DataProtocolService;

import static org.junit.Assert.*;

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

    filename = "/home/toto/toto.txt";
    df = new DataFile(URI.create(filename));
    assertEquals(filename, df.getSource());

    filename = "file:///home/toto/toto.txt";
    df = new DataFile(URI.create(filename));
    assertEquals(filename, df.getSource());

    filename = "file:/home/toto/toto.txt";
    df = new DataFile(URI.create(filename));
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
  public void testGetParent() throws IOException {

    String filename = "toto.txt";
    DataFile df = new DataFile(filename);
    assertEquals("", df.getParent().getSource());

    filename = "/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("/home/toto", df.getParent().getSource());

    filename = "file:///home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("file:///home/toto", df.getParent().getSource());

    filename = "file:/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("file:/home/toto", df.getParent().getSource());

    filename = "http://www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("http://www.toto.com/home/toto", df.getParent().getSource());

    filename = "http:/www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("http:/www.toto.com/home/toto", df.getParent().getSource());

    filename = "ftp://ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("ftp://ftp.toto.com/home/toto", df.getParent().getSource());

    filename = "ftp:/ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("ftp:/ftp.toto.com/home/toto", df.getParent().getSource());

    filename = "ftp://login:passwd@ftp.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals("ftp://login:passwd@ftp.toto.com/home/toto",
        df.getParent().getSource());
  }

  @Test
  public void testGetProtocol() throws IOException {

    DataProtocolService registry = DataProtocolService.getInstance();

    assertTrue(registry.isService("file"));
    assertTrue(registry.isService("http"));
    assertTrue(registry.isService("ftp"));

    String filename = "toto.txt";
    DataFile df = new DataFile(filename);
    assertEquals(registry.newService("file"), df.getProtocol());

    filename = "/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(registry.newService("file"), df.getProtocol());

    filename = "file:///home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(registry.newService("file"), df.getProtocol());

    filename = "file:/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(registry.newService("file"), df.getProtocol());

    filename = "http://www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(registry.newService("http"), df.getProtocol());

    filename = "http:/www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(registry.newService("http"), df.getProtocol());

    try {
      new DataFile("toto://www.toto.com/home/toto/toto.txt").getProtocol();
      fail();
    } catch (IOException e) {
      assertTrue(true);
    }

  }

  @Test
  public void testToFile() throws IOException, URISyntaxException {

    String filename = "toto.txt";
    DataFile df = new DataFile(filename);
    assertEquals(new File(filename), df.toFile());

    filename = "/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(new File(filename), df.toFile());

    filename = "file:///home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(new File(new URI(filename)), df.toFile());

    filename = "file:/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(new File(new URI(filename)), df.toFile());
  }

  @Test
  public void testToUri() throws IOException, URISyntaxException {

    DataProtocolService registry = DataProtocolService.getInstance();

    assertTrue(registry.isService("file"));
    assertTrue(registry.isService("http"));
    assertTrue(registry.isService("ftp"));

    String filename = "toto.txt";
    DataFile df = new DataFile(filename);
    assertEquals(new URI(filename), df.toUri());

    filename = "/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(new URI(filename), df.toUri());

    filename = "file:///home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(new URI(filename), df.toUri());

    filename = "file:/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(new URI(filename), df.toUri());

    filename = "http://www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(new URI(filename), df.toUri());

    filename = "http:/www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertEquals(new URI(filename), df.toUri());

    filename = ":/www.toto.com/home/toto/toto.txt";
    df = new DataFile(filename);
    assertNull(df.toUri());
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
      new DataFile((String) null);
      fail();
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    new DataFile("toto:/www.toto.com/home/toto/toto.txt");
    assertTrue(true);

  }

}
