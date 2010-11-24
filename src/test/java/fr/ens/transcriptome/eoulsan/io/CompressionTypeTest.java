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

package fr.ens.transcriptome.eoulsan.io;

import static org.junit.Assert.*;

import org.junit.Test;

public class CompressionTypeTest {

  @Test
  public void testGetContentEncoding() {

    assertEquals("gzip", CompressionType.GZIP.getContentEncoding());
    assertEquals("bzip2", CompressionType.BZIP2.getContentEncoding());
    assertEquals("", CompressionType.NONE.getContentEncoding());
  }

  @Test
  public void testGetExtension() {

    assertEquals(".gz", CompressionType.GZIP.getExtension());
    assertEquals(".bz2", CompressionType.BZIP2.getExtension());
    assertEquals("", CompressionType.NONE.getExtension());
  }

  @Test
  public void testGetCompressionTypeByContentEncoding() {

    assertEquals(CompressionType.GZIP, CompressionType
        .getCompressionTypeByContentEncoding("gzip"));
    assertEquals(CompressionType.BZIP2, CompressionType
        .getCompressionTypeByContentEncoding("bzip2"));
    assertEquals(CompressionType.NONE, CompressionType
        .getCompressionTypeByContentEncoding(""));
    assertEquals(null, CompressionType
        .getCompressionTypeByContentEncoding(null));
  }

  @Test
  public void testGetCompressionTypeByExtension() {

    assertEquals(CompressionType.GZIP, CompressionType
        .getCompressionTypeByExtension(".gz"));
    assertEquals(CompressionType.BZIP2, CompressionType
        .getCompressionTypeByExtension(".bz2"));
    assertEquals(CompressionType.NONE, CompressionType
        .getCompressionTypeByExtension(""));
    assertEquals(null, CompressionType.getCompressionTypeByExtension(null));
  }

  @Test
  public void testGetCompressionTypeByFilename() {

    assertEquals(CompressionType.GZIP, CompressionType
        .getCompressionTypeByFilename("/home/toto/toto.gz"));
    assertEquals(CompressionType.GZIP, CompressionType
        .getCompressionTypeByFilename("toto.gz"));
    assertEquals(CompressionType.GZIP, CompressionType
        .getCompressionTypeByFilename("toto.txt.gz"));
    assertEquals(CompressionType.BZIP2, CompressionType
        .getCompressionTypeByFilename("titi.txt.bz2"));
    assertEquals(CompressionType.NONE, CompressionType
        .getCompressionTypeByFilename("titi.txt"));
    assertEquals(CompressionType.NONE, CompressionType
        .getCompressionTypeByFilename(""));
    assertEquals(null, CompressionType.getCompressionTypeByFilename(null));
  }

}
