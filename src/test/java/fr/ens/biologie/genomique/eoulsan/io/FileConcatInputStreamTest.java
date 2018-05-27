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

package fr.ens.biologie.genomique.eoulsan.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.base.Strings;

import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

public class FileConcatInputStreamTest {

  @Test
  public void test() throws IOException {

    final List<File> files = new ArrayList<>();

    files.add(writeFile(1000, 255));
    files.add(writeFile(1500, 123));
    files.add(writeFile(777, 222));
    files.add(writeFile(888, 255));

    final int total = 1000 + 1500 + 777 + 888;

    final InputStream is = new FileConcatInputStream(files);
    final BufferedReader br = FileUtils.createBufferedReader(is);
    String line = null;
    int count = 0;

    while ((line = br.readLine()) != null) {

      final String[] fields = line.split("\t");
      assertNotNull(fields);

      final int r = Integer.parseInt(fields[0]);

      if (r == 0) {
        assertEquals(1, fields.length);
      } else {
        assertEquals(2, fields.length);

        assertEquals(r, fields[1].length());
      }
      count++;
    }

    assertEquals(total, count);

    br.close();

    for (File f : files) {
      f.delete();
    }
  }

  private File writeFile(final int lines, final int mod) throws IOException {

    File f = File.createTempFile("junit-", ".txt");

    Writer writer = new FileWriter(f);

    for (int i = 0; i < lines; i++) {
      final int r = i % mod;
      writer.write(r + "\t" + Strings.repeat("*", r) + "\n");
    }

    writer.close();
    return f;
  }

}
