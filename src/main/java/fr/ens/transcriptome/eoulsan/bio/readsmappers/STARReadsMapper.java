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

package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;

/**
 * This class define a wrapper on the STAR mapper.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class STARReadsMapper extends AbstractSequenceReadsMapper {

  private static final String DEFAULT_PACKAGE_VERSION = "2.4.0e";
  private static final String MAPPER_EXECUTABLE = "STARstatic";
  private static final String INDEXER_EXECUTABLE = MAPPER_EXECUTABLE;

  public static final String DEFAULT_ARGUMENTS = "";

  private static final String SYNC = STARReadsMapper.class.getName();
  private static final String MAPPER_NAME = "STAR";

  @Override
  public String getMapperName() {

    return MAPPER_NAME;
  }

  @Override
  public String getMapperVersion() {

    try {
      final String execPath;

      synchronized (SYNC) {
        execPath = install(MAPPER_EXECUTABLE);
      }

      // Create temporary directory
      final File tempDir = File.createTempFile("STAR-get-version-", ".tmp");
      tempDir.delete();
      tempDir.mkdir();

      // Execute STAR with no argument
      final ProcessBuilder pb = new ProcessBuilder(execPath);
      pb.directory(tempDir);
      final Process p = pb.start();
      p.waitFor();

      final File logFile = new File(tempDir, "Log.out");

      // Read STAR version from STAR log file
      String version = null;
      try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
        final String line = reader.readLine();

        if (line != null && line.indexOf('=') != -1) {
          version = line.substring(line.indexOf('=') + 1).trim();
          if (version.startsWith("STAR_")) {
            version = version.substring("STAR_".length());
          }
        }

      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }

      // Delete temporary files
      new File(tempDir, "Log.progress.out").delete();
      new File(tempDir, "Aligned.out.sam").delete();
      new File(tempDir, "_tmp").delete();
      logFile.delete();
      tempDir.delete();

      return version;
    } catch (IOException e) {

      e.printStackTrace();

      return null;
    } catch (InterruptedException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public boolean isSplitsAllowed() {

    return true;
  }

  @Override
  public DataFormat getArchiveFormat() {

    return DataFormats.STAR_INDEX_ZIP;
  }

  @Override
  protected String getPackageVersion() {

    return DEFAULT_PACKAGE_VERSION;
  }

  @Override
  protected String getIndexerExecutable() {

    return INDEXER_EXECUTABLE;
  }

  @Override
  protected List<String> getIndexerCommand(String indexerPathname,
      String genomePathname) {

    final File genomeFile = new File(genomePathname);
    List<String> cmd = new ArrayList<String>();
    cmd.add(indexerPathname);
    cmd.add("--runThreadN");
    cmd.add("" + getThreadsNumber());
    cmd.add("--runMode");
    cmd.add("genomeGenerate");
    cmd.add("--genomeDir");
    cmd.add(genomeFile.getParentFile().getAbsolutePath());
    cmd.add("--genomeFastaFiles");
    cmd.add(genomePathname);

    return cmd;
  }

  @Override
  protected InputStream internalMapPE(File readsFile1, File readsFile2,
      File archiveIndex, GenomeDescription gd) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected InputStream internalMapSE(File readsFile, File archiveIndex,
      GenomeDescription gd) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected MapperProcess internalMapPE(File archiveIndex, GenomeDescription gd)
      throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected MapperProcess internalMapSE(File archiveIndex, GenomeDescription gd)
      throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

}
