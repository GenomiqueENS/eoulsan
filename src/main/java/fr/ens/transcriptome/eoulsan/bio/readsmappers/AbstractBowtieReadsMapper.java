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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.SAMParserLine;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define a wrapper on the Bowtie mapper.
 * @since 1.3
 * @author Laurent Jourdren
 */
public abstract class AbstractBowtieReadsMapper extends
    AbstractSequenceReadsMapper {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String SYNC = AbstractBowtieReadsMapper.class.getName();

  private File outputFile;

  abstract protected String getExtensionIndexFile();

  abstract public String getMapperName();

  abstract public DataFormat getArchiveFormat();

  abstract protected String[] getMapperExecutables();

  abstract protected String getIndexerExecutable();

  abstract public String getDefaultArguments();

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

  @Override
  protected void internalMap(File readsFile1, File readsFile2,
      File archiveIndexDir) throws IOException {

    final String bowtiePath;

    synchronized (SYNC) {
      bowtiePath = install(getMapperExecutables());
    }

    final String extensionIndexFile = getExtensionIndexFile();

    final String index =
        new File(getIndexPath(archiveIndexDir, extensionIndexFile,
            extensionIndexFile.length())).getName();

    final File outputFile =
        FileUtils.createTempFile(readsFile1.getParentFile(), getMapperName()
            .toLowerCase() + "-outputFile-", ".sam");

    // Build the command line
    final List<String> cmd = new ArrayList<String>();

    cmd.add(bowtiePath);
    cmd.add(bowtieQualityArgument());

    if (getListMapperArguments() != null)
      cmd.addAll(getListMapperArguments());
    cmd.add("-p");
    cmd.add(getThreadsNumber() + "");
    cmd.add(index);
    cmd.add("-1");
    cmd.add(readsFile1.getAbsolutePath());
    cmd.add("-2");
    cmd.add(readsFile2.getAbsolutePath());
    cmd.add("-S");
    cmd.add(outputFile.getAbsolutePath());

    LOGGER.info(cmd.toString());

    final int exitValue = sh(cmd, archiveIndexDir);

    if (exitValue != 0) {
      throw new IOException("Bad error result for "
          + getMapperName() + " execution: " + exitValue);
    }

    this.outputFile = outputFile;

  }

  @Override
  protected void internalMap(File readsFile, File archiveIndexDir)
      throws IOException {

    final String bowtiePath;

    synchronized (SYNC) {
      bowtiePath = install(getMapperExecutables());
    }

    final String extensionIndexFile = getExtensionIndexFile();

    final String index =
        new File(getIndexPath(archiveIndexDir, extensionIndexFile,
            extensionIndexFile.length())).getName();

    final File outputFile =
        FileUtils.createTempFile(readsFile.getParentFile(), getMapperName()
            .toLowerCase() + "-outputFile-", ".sam");

    // Build the command line
    final List<String> cmd = new ArrayList<String>();

    cmd.add(bowtiePath);
    if (getListMapperArguments() != null)
      cmd.addAll(getListMapperArguments());
    cmd.add(bowtieQualityArgument());
    cmd.add("-p");
    cmd.add(getThreadsNumber() + "");
    cmd.add(index);
    cmd.add("-q");
    cmd.add(readsFile.getAbsolutePath());
    cmd.add("-S");
    cmd.add(outputFile.getAbsolutePath());

    LOGGER.info(cmd.toString());

    final int exitValue = sh(cmd, archiveIndexDir);

    if (exitValue != 0) {
      throw new IOException("Bad error result for "
          + getMapperName() + " execution: " + exitValue);
    }

    this.outputFile = outputFile;
  }

  /**
   * create the line command for execute single-end mapping
   * @param readsFile fastq file
   * @param archiveIndexDir index file used for mapping
   * @param parserLine SAMParserLine which get back a output stream of bowtie
   * @throw error IO in reading with readsFile
   */
  protected void internalMap(File readsFile, File archiveIndexDir,
      SAMParserLine parserLine) throws IOException {

    final String bowtiePath;

    synchronized (SYNC) {
      bowtiePath = install(getMapperExecutables());
    }

    final String extensionIndexFile = getExtensionIndexFile();

    final String index =
        new File(getIndexPath(archiveIndexDir, extensionIndexFile,
            extensionIndexFile.length())).getName();

    // Build the command line
    final List<String> cmd = new ArrayList<String>();

    cmd.add(bowtiePath);
    if (getListMapperArguments() != null)
      cmd.addAll(getListMapperArguments());
    cmd.add(bowtieQualityArgument());
    cmd.add("-p");
    cmd.add(getThreadsNumber() + "");
    cmd.add(index);
    cmd.add("-q");
    cmd.add(readsFile.getAbsolutePath());
    cmd.add("-S");

    // TODO to remove
    System.out.println("cmd bowtie : " + cmd);

    LOGGER.info(cmd.toString());

    final int exitValue = sh(cmd, archiveIndexDir, parserLine);

    if (exitValue != 0) {
      throw new IOException("Bad error result for "
          + getMapperName() + " execution: " + exitValue);
    }
  }

  @Override
  /**
   * create the line command for execute pair-end mapping
   * @param readsFile1 fastq file
   * @param readsFile2 fastq file
   * @param archiveIndexDir index file used for mapping
   * @param parserLine SAMParserLine which get back a output stream of bowtie
   * @throw error IO in reading with readsFile1 or readsFile2 
   */
  protected void internalMap(File readsFile1, File readsFile2,
      File archiveIndexDir, SAMParserLine parserLine) throws IOException {

    final String bowtiePath;

    synchronized (SYNC) {
      bowtiePath = install(getMapperExecutables());
    }

    final String extensionIndexFile = getExtensionIndexFile();

    final String index =
        new File(getIndexPath(archiveIndexDir, extensionIndexFile,
            extensionIndexFile.length())).getName();

    // Build the command line
    final List<String> cmd = new ArrayList<String>();

    cmd.add(bowtiePath);
    if (getListMapperArguments() != null)
      cmd.addAll(getListMapperArguments());
    cmd.add(bowtieQualityArgument());
    cmd.add("-p");
    cmd.add(getThreadsNumber() + "");
    cmd.add(index);
    cmd.add("-q");
    cmd.add("-1");
    cmd.add(readsFile1.getAbsolutePath());
    cmd.add("-2");
    cmd.add(readsFile2.getAbsolutePath());
    cmd.add("-S");

    // TODO to remove
    System.out.println("cmd bowtie : " + cmd.toString().replace(',', ' '));

    LOGGER.info(cmd.toString());

    final int exitValue = sh(cmd, archiveIndexDir, parserLine);

    if (exitValue != 0) {
      throw new IOException("Bad error result for "
          + getMapperName() + " execution: " + exitValue);
    }
  }

  @Override
  public void clean() {
  }

  @Override
  public File getSAMFile(final GenomeDescription gd) throws IOException {

    return this.outputFile;
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
