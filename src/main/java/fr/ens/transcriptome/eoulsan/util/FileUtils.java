package fr.ens.transcriptome.eoulsan.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import fr.ens.biologie.genomique.eoulsan.util.UnSynchronizedBufferedWriter;

public class FileUtils {

  public static final BufferedReader createBufferedReader(final File file)
      throws FileNotFoundException {

    return fr.ens.biologie.genomique.eoulsan.util.FileUtils
        .createBufferedReader(file);
  }

  public static final UnSynchronizedBufferedWriter createFastBufferedWriter(
      final File file) throws IOException {

    return fr.ens.biologie.genomique.eoulsan.util.FileUtils
        .createFastBufferedWriter(file);
  }

  public static final UnSynchronizedBufferedWriter createFastBufferedWriter(
      final String filename) throws IOException {

    return fr.ens.biologie.genomique.eoulsan.util.FileUtils
        .createFastBufferedWriter(filename);
  }

  public static final UnSynchronizedBufferedWriter createFastBufferedWriter(
      final OutputStream os) throws IOException {

    return fr.ens.biologie.genomique.eoulsan.util.FileUtils
        .createFastBufferedWriter(os);
  }

}
