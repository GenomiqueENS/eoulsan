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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define a wrapper on the Bowtie mapper.
 * @since 1.3
 * @author Laurent Jourdren
 */
public abstract class AbstractBowtieReadsMapper extends
    AbstractSequenceReadsMapper {

  private static final String SYNC = AbstractBowtieReadsMapper.class.getName();

  abstract protected String getExtensionIndexFile();

  abstract protected String[] getMapperExecutables();

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
      if (lines.length == 0)
        return null;

      final String[] tokens = lines[0].split(" version ");
      if (tokens.length > 1)
        return tokens[1].trim();

      return null;

    } catch (IOException e) {

      return null;
    }
  }

  @Override
  protected List<String> getIndexerCommand(String indexerPathname,
      String genomePathname) {

    List<String> cmd = new ArrayList<String>();

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

    final String extensionIndexFile = getExtensionIndexFile();

    final String index =
        new File(getIndexPath(archiveIndexDir, extensionIndexFile,
            extensionIndexFile.length())).getName();

    final MapperProcess mapperProcess =
        new MapperProcess(getMapperName(), true, false, false) {

          @Override
          protected List<List<String>> createCommandLines() {

            // Build the command line
            final List<String> cmd = new ArrayList<String>();

            // Bowtie Executable path
            cmd.add(bowtiePath);

            // User options
            if (getListMapperArguments() != null)
              cmd.addAll(getListMapperArguments());

            // Set the number of threads
            cmd.add("-p");
            cmd.add(getThreadsNumber() + "");

            // Input in FASTQ format
            cmd.add("-q");

            // Set the quality format
            cmd.add(bowtieQualityArgument());

            // Output in SAM format
            cmd.add("-S");

            // Genome index name
            cmd.add(index);

            // Input FASTQ file
            cmd.add(readsFile.getAbsolutePath());

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

    final String extensionIndexFile = getExtensionIndexFile();

    final String index =
        new File(getIndexPath(archiveIndexDir, extensionIndexFile,
            extensionIndexFile.length())).getName();

    final MapperProcess mapperProcess =
        new MapperProcess(getMapperName(), true, false, false) {

          @Override
          protected List<List<String>> createCommandLines() {
            // Build the command line
            final List<String> cmd = new ArrayList<String>();

            // Bowtie Executable path
            cmd.add(bowtiePath);

            // Set user options
            if (getListMapperArguments() != null)
              cmd.addAll(getListMapperArguments());

            // Set the number of threads
            cmd.add("-p");
            cmd.add(getThreadsNumber() + "");

            // Input in FASTQ format
            cmd.add("-q");

            // Set the quality format
            cmd.add(bowtieQualityArgument());

            // Output in SAM format
            cmd.add("-S");

            // Genome index name
            cmd.add(index);

            // First end input FASTQ file
            cmd.add("-1");
            cmd.add(readsFile1.getAbsolutePath());

            // Second end input FASTQ file
            cmd.add("-2");
            cmd.add(readsFile2.getAbsolutePath());

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

    final String extensionIndexFile = getExtensionIndexFile();

    final String index =
        new File(getIndexPath(archiveIndexDir, extensionIndexFile,
            extensionIndexFile.length())).getName();

    // TODO Warning streaming mode not currently enabled
    return new MapperProcess(getMapperName(), false, false, false) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line
        final List<String> cmd = new ArrayList<String>();

        // Bowtie Executable path
        cmd.add(bowtiePath);

        // Set user options
        if (getListMapperArguments() != null)
          cmd.addAll(getListMapperArguments());

        // Set the number of threads
        cmd.add("-p");
        cmd.add(getThreadsNumber() + "");

        // Input in FASTQ format
        cmd.add("-q");

        // Set the quality format
        cmd.add(bowtieQualityArgument());

        // Output in SAM format
        cmd.add("-S");

        // Genome index name
        cmd.add(index);

        // TODO Enable this in streaming mode
        // Input from stdin
        // cmd.add("-");

        // TODO Remove this when streaming mode will be enabled
        // Input from temporary FASTQ file
        cmd.add(getTmpInputFile1().getAbsolutePath());

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

    final String extensionIndexFile = getExtensionIndexFile();

    final String index =
        new File(getIndexPath(archiveIndexDir, extensionIndexFile,
            extensionIndexFile.length())).getName();

    // TODO Warning streaming mode not currently enabled
    return new MapperProcess(getMapperName(), false, false, true) {

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
        final List<String> cmd = new ArrayList<String>();

        // Bowtie Executable path
        cmd.add(bowtiePath);

        // Set the user options
        if (getListMapperArguments() != null)
          cmd.addAll(getListMapperArguments());

        // Set the number of threads to use
        cmd.add("-p");
        cmd.add(getThreadsNumber() + "");

        // TODO Remove this when streaming mode will be enabled
        // Input in FASTQ format
        cmd.add(("-q"));

        // Set the quality format
        cmd.add(bowtieQualityArgument());

        // TODO enable this in streaming mode
        // Input in Crossbow format
        // cmd.add(("-r"));

        // Output in SAM format
        cmd.add("-S");

        // Genome index name
        cmd.add(index);

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

  //
  // Init
  //

  @Override
  public void init(final boolean pairedEnd, final FastqFormat fastqFormat,
      final File archiveIndexFile, final File archiveIndexDir,
      final ReporterIncrementer incrementer, final String counterGroup)
      throws IOException {

    super.init(pairedEnd, fastqFormat, archiveIndexFile, archiveIndexDir,
        incrementer, counterGroup);
    setMapperArguments(getDefaultArguments());
  }

}
