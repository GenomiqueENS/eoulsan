package fr.ens.biologie.genomique.eoulsan.modules.multiqc;

import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFiles;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;

/**
 * This class define a preprocessor for FastQC reports.
 * @since 2.2
 * @author Laurent Jourdren
 */
public class FastQCInputPreprocessor implements InputPreprocessor {

  public static final String REPORT_NAME = "fastqc";

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public DataFormat getDataFormat() {
    return DataFormats.FASTQC_REPORT_ZIP;
  }

  @Override
  public void preprocess(final TaskContext context, final Data data,
      final File multiQCInputDirectory) throws IOException {

    // Get data name
    String name = data.getName();

    int fileCount = data.getDataFileCount();

    for (int i = 0; i < fileCount; i++) {

      // Define symbolic link path
      DataFile outputFile = new DataFile(multiQCInputDirectory,
          name + (fileCount <= 1 ? "" : "_read" + i) + "_fastqc.zip");

      // Define target log file
      DataFile fastQCReportFile = data.getDataFile(i);

      // Create output file
      if (!outputFile.exists()) {
        DataFiles.copy(fastQCReportFile, outputFile);
      }

      // Update report with the data name
      updateFastqcResultZip(outputFile.toFile().toPath(), name);
    }

  }

  /**
   * Update fastqc_data.txt file inside the zip with the name of sample.
   * @param reportfile the zip file
   * @param name the name of the sample
   * @throws IOException if an error occurs while updating the file
   */
  private static void updateFastqcResultZip(Path reportfile, String name)
      throws IOException {

    requireNonNull(reportfile);
    requireNonNull(name);

    String pathInZip = dataPathInZip(reportfile);

    try (FileSystem fs = FileSystems.newFileSystem(reportfile, (ClassLoader) null)) {
      Path source = fs.getPath(pathInZip);
      Path temp = fs.getPath("/___fastqc_data___.txt");
      Files.move(source, temp);
      updateReport(temp, source, name);
      Files.delete(temp);
    }
  }

  /**
   * Look for the path of fastqc_data.txt file inside the zip file
   * @param reportfile the zip file
   * @return the path of the fastqc_data.txt file inside the zip file
   * @throws IOException if an error occurs while opening the zip file
   */
  private static String dataPathInZip(Path reportfile) throws IOException {

    try (ZipInputStream zipIn =
        new ZipInputStream(Files.newInputStream(reportfile))) {
      ZipEntry entry = zipIn.getNextEntry();

      // iterates over entries in the zip file
      while (entry != null) {
        if (entry.getName().endsWith("/fastqc_data.txt")) {
          return entry.getName();
        }

        zipIn.closeEntry();
        entry = zipIn.getNextEntry();
      }
    }

    throw new IOException("No fastqc_data.txt found in " + reportfile);
  }

  /**
   * Update the fastqc_data.txt file.
   * @param src input file
   * @param dst output file
   * @param name name of the report
   * @throws IOException if an error occurs while updating the file
   */
  private static void updateReport(Path src, Path dst, String name)
      throws IOException {
    try (
        BufferedReader br = new BufferedReader(
            new InputStreamReader(Files.newInputStream(src)));
        BufferedWriter bw = new BufferedWriter(
            new OutputStreamWriter(Files.newOutputStream(dst)))) {

      String line;
      while ((line = br.readLine()) != null) {

        if (line.startsWith("Filename\t")) {
          line = "Filename\t" + name + ".fastq.gz";
        }

        bw.write(line);
        bw.newLine();
      }
    }
  }

}
