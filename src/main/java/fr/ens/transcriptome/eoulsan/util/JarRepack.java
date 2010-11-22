package fr.ens.transcriptome.eoulsan.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class JarRepack {

  /** The default size of the buffer. */
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

  private ZipOutputStream zos;

  private void copy(final File file) throws IOException {

    final ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
    final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

    ZipEntry entry = zin.getNextEntry();
    // while (entry != null) {
    do {

      final String entryName = entry.getName();
      this.zos.putNextEntry(new ZipEntry(entryName));

      long count = 0;
      int n = 0;
      while ((n = zin.read(buffer)) != -1) {
        this.zos.write(buffer, 0, n);
        count += n;
      }

      if (entry.getSize() != count) {
        throw new IOException("Copied size of zip entry "
            + count + " is not as excepted: " + entry.getSize());
      }

    } while ((entry = zin.getNextEntry()) != null);

    zin.close();
  }

  public void addFile(final File file, final String destDir) throws IOException {

    if (file == null) {
      return;
    }

    BufferedInputStream origin = null;

    final byte data[] = new byte[DEFAULT_BUFFER_SIZE];

    this.zos.putNextEntry(new ZipEntry(destDir + file.getName()));
    final FileInputStream fis = new FileInputStream(file);

    origin = new BufferedInputStream(fis, DEFAULT_BUFFER_SIZE);

    long count = 0;
    int n;
    while ((n = origin.read(data, 0, DEFAULT_BUFFER_SIZE)) != -1) {
      this.zos.write(data, 0, n);
      count += n;
    }

    if (file.length() != count) {
      throw new IOException("Copied size of zip entry "
          + count + " is not as excepted: " + file.length());
    }

    origin.close();

  }

  public void close() throws IOException {

    this.zos.close();
  }

  //
  // Constructor
  //

  public JarRepack(final File inFile, final File outFile) throws IOException {

    if (inFile == null) {
      throw new NullPointerException("the inFile argument is null.");
    }

    if (outFile == null) {
      throw new NullPointerException("the outFile argument is null.");
    }

    this.zos = new ZipOutputStream(new FileOutputStream(outFile));
    copy(inFile);
  }

  //
  // Main method
  //

  public static void main(String[] args) throws IOException {

    final File inputJarFile =
        new File(
            "/Users/jourdren/Documents/workspace/eoulsan/target/eoulsan-0.5-SNAPSHOT.jar");
    final File outputDir = new File("/tmp");
    final File jarsToAddDir =
        new File(
            "/Users/jourdren/Documents/workspace/eoulsan/target/dist/eoulsan-0.5-SNAPSHOT/lib");

    final File outputJarFile = new File(outputDir, inputJarFile.getName());

    if (outputJarFile.exists())
      outputJarFile.delete();

    FileUtils.copyFile(inputJarFile, outputJarFile);

    File[] files = jarsToAddDir.listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File dir, String name) {

        return name.toLowerCase().endsWith(".jar");
      }
    });

    JarRepack jr = new JarRepack(inputJarFile, outputJarFile);

    for (File f : files) {
      System.out.println(f);
      jr.addFile(f, "lib/");
    }

    jr.close();

  }
}
