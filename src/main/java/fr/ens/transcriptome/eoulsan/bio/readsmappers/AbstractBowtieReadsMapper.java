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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define a wrapper on the Bowtie mapper.
 * @since 2.0
 * @author Laurent Jourdren
 */
public abstract class AbstractBowtieReadsMapper extends
    AbstractSequenceReadsMapper {

  protected static final String SYNC = AbstractBowtieReadsMapper.class
      .getName();

  abstract protected String getExtensionIndexFile();

  abstract protected String[] getMapperExecutables();

  @Override
  abstract protected String getIndexerExecutable();

  abstract protected String getDefaultArguments();

  @Override
  public boolean isSplitsAllowed() {
    return true;
  }

  @Override
  public String getMapperVersion() {

    try {
      final String bowtiePath;

      synchronized (SYNC) {
        bowtiePath = install(getMapperExecutables());
      }

      final String cmd = bowtiePath + " --version";

      final String s = ProcessUtils.execToString(cmd);
      final String[] lines = s.split("\n");
      if (lines.length == 0) {
        return null;
      }

      final String[] tokens = lines[0].split(" version ");
      if (tokens.length > 1) {
        return tokens[1].trim();
      }

      return null;

    } catch (IOException e) {

      return null;
    }
  }

  @Override
  protected List<String> getIndexerCommand(final String indexerPathname,
      final String genomePathname) {

    List<String> cmd = new ArrayList<>();

    cmd.add(indexerPathname);
    cmd.add(genomePathname);
    cmd.add("genome");

    return cmd;
  }

  protected String bowtieQualityArgument() {
    return BowtieReadsMapper.getBowtieQualityArgument(getFastqFormat());
  }

  //
  // Map in file mode
  //

  @Override
  protected InputStream internalMapSE(final File readsFile,
      final File archiveIndexDir, final GenomeDescription genomeDescription)
      throws IOException {

    final String bowtiePath;

    synchronized (SYNC) {
      bowtiePath = install(getMapperExecutables());
    }

    // Get index argument
    final String index = getIndexArgument(archiveIndexDir);

    final MapperProcess mapperProcess =
        new MapperProcess(this, true, false, false) {

          @Override
          protected List<List<String>> createCommandLines() {

            // Build the command line
            final List<String> cmd = new ArrayList<>();

            // Add common arguments
            cmd.addAll(createCommonArgs(bowtiePath, index));

            // Input FASTQ file
            cmd.add(readsFile.getAbsolutePath());

            getLogger().info(cmd.toString());

            return Collections.singletonList(cmd);
          }

          @Override
          protected File executionDirectory() {

            return archiveIndexDir;
          }

        };

    return mapperProcess.getStout();
  }

  @Override
  protected InputStream internalMapPE(final File readsFile1,
      final File readsFile2, final File archiveIndexDir,
      final GenomeDescription genomeDescription) throws IOException {

    final String bowtiePath;

    synchronized (SYNC) {
      bowtiePath = install(getMapperExecutables());
    }

    // Get index argument
    final String index = getIndexArgument(archiveIndexDir);

    final MapperProcess mapperProcess =
        new MapperProcess(this, true, false, false) {

          @Override
          protected List<List<String>> createCommandLines() {
            // Build the command line
            final List<String> cmd = new ArrayList<>();

            // Add common arguments
            cmd.addAll(createCommonArgs(bowtiePath, index));

            // First end input FASTQ file
            cmd.add("-1");
            cmd.add(readsFile1.getAbsolutePath());

            // Second end input FASTQ file
            cmd.add("-2");
            cmd.add(readsFile2.getAbsolutePath());

            getLogger().info("Command line executed: " + cmd.toString());

            return Collections.singletonList(cmd);
          }

          @Override
          protected File executionDirectory() {

            return archiveIndexDir;
          }

        };

    return mapperProcess.getStout();

  }

  //
  // Map in streaming mode
  //

  @Override
  protected MapperProcess internalMapSE(final File archiveIndexDir,
      final GenomeDescription genomeDescription) throws IOException {

    final String bowtiePath;

    synchronized (SYNC) {
      bowtiePath = install(getMapperExecutables());
    }

    // Get index argument
    final String index = getIndexArgument(archiveIndexDir);

    // TODO Warning streaming mode not currently enabled
    return new MapperProcess(this, false, false, false) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line
        final List<String> cmd = new ArrayList<>();

        // TODO enable memory mapped in streaming mode
        // Add common arguments
        cmd.addAll(createCommonArgs(bowtiePath, index, false, false));

        // TODO Enable this in streaming mode
        // Input from stdin
        // cmd.add("-");

        // TODO Remove this when streaming mode will be enabled
        // Input from temporary FASTQ file
        cmd.add(getTmpInputFile1().getAbsolutePath());

        getLogger().info(cmd.toString());

        return Collections.singletonList(cmd);
      }

      @Override
      protected File executionDirectory() {

        return archiveIndexDir;
      }

    };
  }

  @Override
  protected MapperProcess internalMapPE(final File archiveIndexDir,
      final GenomeDescription genomeDescription) throws IOException {

    final String bowtiePath;

    synchronized (SYNC) {
      bowtiePath = install(getMapperExecutables());
    }

    // Get index argument
    final String index = getIndexArgument(archiveIndexDir);

    // TODO Warning streaming mode not currently enabled
    return new MapperProcess(this, false, false, true) {

      @Override
      public void writeEntry(final String name1, final String sequence1,
          final String quality1, final String name2, final String sequence2,
          final String quality2) throws IOException {

        // TODO Write reads in Crossbow format when streaming mode will be
        // enabled
        super
            .writeEntry(name1, sequence1, quality1, name2, sequence2, quality2);
      }

      @Override
      protected List<List<String>> createCommandLines() {
        // Build the command line
        final List<String> cmd = new ArrayList<>();

        // TODO enable memory mapped in streaming mode
        // Add common arguments
        cmd.addAll(createCommonArgs(bowtiePath, index, true, false));

        // TODO enable this in streaming mode
        // Read input from stdin (streaming mode)
        // cmd.add("-12");
        // cmd.add("-");

        // TODO Remove this when streaming mode will be enabled
        // First end input FASTQ file
        cmd.add("-1");
        cmd.add(getTmpInputFile1().getAbsolutePath());

        // TODO Remove this when streaming mode will be enabled
        // Second end input FASTQ file
        cmd.add("-2");
        cmd.add(getTmpInputFile2().getAbsolutePath());

        return Collections.singletonList(cmd);
      }

      @Override
      protected File executionDirectory() {

        return archiveIndexDir;
      }

    };

  }

  /**
   * Get the index argument for bowtie from the archive index directory path
   * @param archiveIndexDir archive index directory
   * @return the Bowtie index argument
   * @throws IOException if an error occurs when getting directory path
   */
  private String getIndexArgument(final File archiveIndexDir)
      throws IOException {

    final String extensionIndexFile = getExtensionIndexFile();

    return new File(getIndexPath(archiveIndexDir, extensionIndexFile,
        extensionIndexFile.length())).getName();
  }

  protected List<String> createCommonArgs(final String bowtiePath,
      final String index) {

    return createCommonArgs(bowtiePath, index, false, false);
  }

  protected abstract List<String> createCommonArgs(final String bowtiePath,
      final String index, final boolean inputCrossbowFormat,
      final boolean memoryMappedIndex);

  //
  // Init
  //

  @Override
  public void init(final File archiveIndexFile, final File archiveIndexDir,
      final ReporterIncrementer incrementer, final String counterGroup)
      throws IOException {

    super.init(archiveIndexFile, archiveIndexDir, incrementer, counterGroup);
    setMapperArguments(getDefaultArguments());
  }

}
