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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define a wrapper on the STAR mapper.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class STARReadsMapper extends AbstractSequenceReadsMapper {

  public static final String MAPPER_NAME = "STAR";
  private static final String DEFAULT_PACKAGE_VERSION = "2.4.0k";

  private static final String MAPPER_STANDARD_EXECUTABLE = "STARstatic";
  private static final String MAPPER_LARGE_INDEX_EXECUTABLE = "STARlong";

  public static final String DEFAULT_ARGUMENTS = "--outSAMunmapped Within";

  private static final String SYNC = STARReadsMapper.class.getName();

  @Override
  public String getMapperName() {

    return MAPPER_NAME;
  }

  @Override
  public String internalGetMapperVersion() {

    try {
      final String execPath;

      synchronized (SYNC) {
        execPath = install(flavoredBinary());
      }

      // Create temporary directory
      final File tempDir = File.createTempFile("STAR-get-version-", ".tmp",
          EoulsanRuntime.getSettings().getTempDirectoryFile());

      if (!(tempDir.delete() && tempDir.mkdir())) {
        EoulsanLogger.getLogger()
            .warning("Cannot create temporary directory for STAR: " + tempDir);
        return null;
      }

      // Execute STAR with no argument
      getExecutor()
          .execute(Lists.newArrayList(execPath), tempDir, false, false, tempDir)
          .waitFor();

      final File logFile = new File(tempDir, "Log.out");

      // Read STAR version from STAR log file
      String version = null;
      try (BufferedReader reader =
          Files.newReader(logFile, Globals.DEFAULT_CHARSET)) {
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
      deleteFile(new File(tempDir, "Log.progress.out"));
      deleteFile(new File(tempDir, "Aligned.out.sam"));
      deleteFile(new File(tempDir, "_tmp"));
      deleteFile(new File(tempDir, "_STARtmp"));
      deleteFile(logFile);
      deleteFile(tempDir);

      return version;
    } catch (IOException e) {

      e.printStackTrace();

      return null;
    }
  }

  /**
   * Remove a file and log a warning if file cannot be removed.
   * @param file file to remove
   */
  private void deleteFile(final File file) {

    // Remove the file
    if (!file.delete()) {
      EoulsanLogger.getLogger().warning("Cannot remove file: " + file);
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
  protected String getDefaultPackageVersion() {

    return DEFAULT_PACKAGE_VERSION;
  }

  @Override
  protected String getIndexerExecutable() {

    return flavoredBinary();
  }

  @Override
  protected String getDefaultMapperArguments() {

    return DEFAULT_ARGUMENTS;
  }

  @Override
  protected boolean checkIfFlavorExists() {

    final String flavor = getMapperFlavorToUse();

    if (flavor == null) {
      return true;
    }

    switch (flavor.trim().toLowerCase()) {
    case "":
    case SHORT_INDEX_FLAVOR:
      setFlavor(SHORT_INDEX_FLAVOR);
      return true;

    case LARGE_INDEX_FLAVOR:
      setFlavor(LARGE_INDEX_FLAVOR);
      return true;

    default:
      return false;
    }
  }

  /**
   * Get the name of the flavored binary.
   * @return the flavored binary name
   */
  private String flavoredBinary() {

    final String flavor = getMapperFlavorToUse();

    if (flavor != null
        && LARGE_INDEX_FLAVOR.equals(flavor.trim().toLowerCase())) {
      return MAPPER_LARGE_INDEX_EXECUTABLE;
    }
    return MAPPER_STANDARD_EXECUTABLE;

  }

  @Override
  protected List<String> getIndexerCommand(final String indexerPathname,
      final String genomePathname) {

    final File genomeFile = new File(genomePathname);
    List<String> cmd = new ArrayList<>();
    cmd.add(indexerPathname);
    cmd.add("--runThreadN");
    cmd.add("" + getThreadsNumber());
    cmd.add("--runMode");
    cmd.add("genomeGenerate");
    cmd.add("--genomeDir");
    cmd.add(genomeFile.getParentFile().getAbsolutePath());
    cmd.add("--genomeFastaFiles");
    cmd.add(genomePathname);

    cmd.addAll(getListIndexerArguments());

    return cmd;
  }

  @Override
  protected MapperProcess internalMapSE(final File archiveIndex)
      throws IOException {

    final String starPath;

    synchronized (SYNC) {
      starPath = install(flavoredBinary());
    }

    return createMapperProcessSE(starPath, archiveIndex.getAbsolutePath());
  }

  @Override
  protected MapperProcess internalMapPE(final File archiveIndex)
      throws IOException {

    final String starPath;

    synchronized (SYNC) {
      starPath = install(flavoredBinary());
    }

    return createMapperProcessPE(starPath, archiveIndex.getAbsolutePath());
  }

  private MapperProcess createMapperProcessSE(final String starPath,
      final String archivePath) throws IOException {

    return new MapperProcess(this, false) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line
        final List<String> cmd = new ArrayList<>();
        cmd.add(starPath);
        cmd.add("--runThreadN");
        cmd.add("" + getThreadsNumber());
        cmd.add("--genomeDir");
        cmd.add(archivePath);
        cmd.add("--outStd");
        cmd.add("SAM");

        cmd.addAll(getListMapperArguments());

        cmd.add("--readFilesIn");
        cmd.add(getNamedPipeFile1().getAbsolutePath());

        return Collections.singletonList(cmd);
      }

    };
  }

  private MapperProcess createMapperProcessPE(final String starPath,
      final String archivePath) throws IOException {

    return new MapperProcess(this, true, true) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line
        final List<String> cmd = new ArrayList<>();
        cmd.add(starPath);
        cmd.add("--runThreadN");
        cmd.add("" + getThreadsNumber());
        cmd.add("--genomeDir");
        cmd.add(archivePath);
        cmd.add("--outStd");
        cmd.add("SAM");

        cmd.addAll(getListMapperArguments());

        cmd.add("--readFilesIn");
        cmd.add(getNamedPipeFile1().getAbsolutePath());
        cmd.add(getNamedPipeFile2().getAbsolutePath());

        return Collections.singletonList(cmd);
      }

    };
  }

  //
  // Init
  //

  @Override
  public void init(final File archiveIndexFile, final File archiveIndexDir,
      final ReporterIncrementer incrementer, final String counterGroup)
          throws IOException {

    super.init(archiveIndexFile, archiveIndexDir, incrementer, counterGroup);
  }

}
