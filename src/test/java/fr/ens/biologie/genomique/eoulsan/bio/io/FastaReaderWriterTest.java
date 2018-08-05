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

package fr.ens.biologie.genomique.eoulsan.bio.io;

import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.md5DigestToString;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import com.google.common.io.ByteStreams;

import fr.ens.biologie.genomique.eoulsan.bio.Sequence;

public class FastaReaderWriterTest {

  @Test
  public void testFastaReaderInputStream() throws IOException {

    final InputStream is = this.getClass().getResourceAsStream("/phix.fasta");

    final SequenceReader reader = new FastaReader(is);

    for (Sequence s : reader) {

      assertEquals(
          "gi|9626372|ref|NC_001422.1| Enterobacteria phage phiX174, complete genome",
          s.getName());
      assertEquals(5386, s.length());
    }
    reader.close();
    reader.throwException();
  }

  @Test
  public void testReadWrite() throws IOException, NoSuchAlgorithmException {

    MessageDigest mdi = MessageDigest.getInstance("MD5");
    MessageDigest mdo = MessageDigest.getInstance("MD5");

    try (InputStream is = this.getClass().getResourceAsStream("/phix.fasta");
        OutputStream os = ByteStreams.nullOutputStream();
        DigestInputStream dis = new DigestInputStream(is, mdi);
        DigestOutputStream dos = new DigestOutputStream(os, mdo);
        SequenceReader reader = new FastaReader(dis);
        SequenceWriter writer = new FastaWriter(dos, 70)) {

      for (Sequence s : reader) {
        writer.write(s);
      }
    }

    assertEquals(md5DigestToString(mdi), md5DigestToString(mdo));
  }

}
