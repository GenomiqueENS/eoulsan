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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class FlatFasta {

  private static final String FLAT_FASTA_EXT = ".flatfasta";
  private static final Charset CHARSET = Charset.forName("ISO-8859-1");

  private File flatFilesDir;
  private String lastChr;
  private int chrSize;
  private byte[] buffer;
  private FileChannel fc;
  private MappedByteBuffer mapByteBuffer;

  public String getSequence(final String chr, final int start, final int end)
      throws IOException {

    if (lastChr == null || !chr.equals(this.lastChr)) {

      // Create a file object for the chromosome file
      final File f = new File(this.flatFilesDir, chr + FLAT_FASTA_EXT);

      // Return null if the chromosome is unknown
      if (!f.exists())
        return null;

      // Close previous reader
      if (this.fc != null)
        this.fc.close();

      // Create new Reader
      // this.reader = FileUtils.createBufferedReader(f);

      this.fc = new FileInputStream(f).getChannel();
      this.mapByteBuffer =
          this.fc.map(FileChannel.MapMode.READ_ONLY, 0, this.fc.size());

      this.chrSize = (int) f.length();
      this.lastChr = chr;
    }

    final int posStart;
    final int posEnd;

    if (start < 1)
      posStart = 0;
    else
      posStart = start - 1;

    if (end > this.chrSize - 1)
      posEnd = this.chrSize - 1;
    else
      posEnd = end - 1;

    final int len = posEnd - posStart;

    if (this.buffer == null || this.buffer.length < len)
      this.buffer = new byte[len];

    this.mapByteBuffer.position(posStart);
    this.mapByteBuffer.get(buffer, 0, len);

    return new String(this.buffer, 0, len, CHARSET);
  }

  /**
   * Close the reader.
   * @throws IOException if an error occurs while closing the reader
   */
  public void close() throws IOException {

    if (this.fc != null)
      this.fc.close();
  }

  /**
   * Create flat files
   * @param fastaFile fasta file to explode
   * @throws IOException if an error occurs while creating flat fasta files
   */
  private void createFlatFastas(final File fastaFile) throws IOException {

    if (fastaFile == null)
      throw new NullPointerException("The fasta file is null");

    final BufferedReader br = FileUtils.createBufferedReader(fastaFile);

    String line = null;

    UnSynchronizedBufferedWriter writer = null;

    while ((line = br.readLine()) != null) {

      line = line.trim();
      if ("".equals(line))
        continue;

      if (line.startsWith(">")) {

        final String chrName = line.substring(1).split(" ")[0];

        if (writer != null)
          writer.close();
        writer =
            FileUtils.createFastBufferedWriter(new File(this.flatFilesDir, chrName
                + FLAT_FASTA_EXT));
      } else {

        if (writer == null)
          throw new IOException("No fasta header found in "
              + fastaFile.getName() + " file");
        writer.write(line);
      }

    }
    br.close();
    writer.close();

  }

  /**
   * Public constructor
   * @param fastaFile fasta file to flat
   * @param flatFilesDir output directory for flat fasta files
   * @throws IOException if an error occurs while creating flat files
   */
  public FlatFasta(final File fastaFile, final File flatFilesDir)
      throws IOException {

    if (flatFilesDir == null)
      throw new NullPointerException("The flat fasta file is null");

    if (!flatFilesDir.exists())
      if (!flatFilesDir.mkdirs())
        throw new IOException("Unable to create directory for flat fasta files");

    this.flatFilesDir = flatFilesDir;

    createFlatFastas(fastaFile);
  }

}
