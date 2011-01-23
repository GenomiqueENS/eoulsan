package fr.ens.transcriptome.eoulsan.io;

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

import fr.ens.transcriptome.eoulsan.util.FileUtils;

public class FileConcatInputStreamTest {

  @Test
  public void test() throws IOException {

    final List<File> files = new ArrayList<File>();

    files.add(writeFile(1000, 255));
    files.add(writeFile(1500, 123));
    files.add(writeFile(777, 222));
    files.add(writeFile(888, 255));

    final InputStream is = new FileConcatInputStream(files);
    final BufferedReader br = FileUtils.createBufferedReader(is);
    String line = null;

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

    }

    br.close();

    for (File f : files)
      f.delete();
  }

  private File writeFile(int lines, int mod) throws IOException {

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
