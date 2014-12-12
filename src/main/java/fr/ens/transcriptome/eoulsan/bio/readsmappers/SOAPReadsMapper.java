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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define a wrapper on the SOAP2 mapper.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class SOAPReadsMapper extends AbstractSequenceReadsMapper {

  private static final String DEFAULT_PACKAGE_VERSION = "2.20";
  private static final String MAPPER_EXECUTABLE = "soap";
  private static final String INDEXER_EXECUTABLE = "2bwt-builder";

  public static final String DEFAULT_ARGUMENTS = "-r 2 -l 28";

  private static final String SYNC = SOAPReadsMapper.class.getName();

  @Override
  public String getMapperName() {

    return "SOAP";
  }

  @Override
  protected String getDefaultPackageVersion() {

    return DEFAULT_PACKAGE_VERSION;
  }

  @Override
  public boolean isSplitsAllowed() {

    return true;
  }

  @Override
  public String getMapperVersion() {

    try {
      final String execPath;

      synchronized (SYNC) {
        execPath = install(MAPPER_EXECUTABLE);
      }

      final String cmd = execPath;

      final String s = ProcessUtils.execToString(cmd, true, false);
      final String[] lines = s.split("\n");

      for (String line : lines) {
        if (line.startsWith("Version:")) {

          final String[] tokens = line.split(":");
          if (tokens.length > 1) {
            return tokens[1].trim();
          }
        }
      }

      return null;

    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public DataFormat getArchiveFormat() {

    return DataFormats.SOAP_INDEX_ZIP;
  }

  @Override
  protected String getIndexerExecutable() {

    return INDEXER_EXECUTABLE;
  }

  @Override
  protected List<String> getIndexerCommand(final String indexerPathname,
      final String genomePathname) {
    List<String> cmd = new ArrayList<>();
    cmd.add(indexerPathname);
    cmd.add(genomePathname);
    return cmd;
  }

  @Override
  protected InputStream internalMapSE(final File readsFile,
      final File archiveIndexDir, final GenomeDescription genomeDescription)
      throws IOException {

    final String soapPath;

    synchronized (SYNC) {
      soapPath = install(MAPPER_EXECUTABLE);
    }

    final File outputFile =
        FileUtils.createTempFile(readsFile.getParentFile(), "soap-outputFile-",
            ".soap");
    final File unmapFile =
        FileUtils.createTempFile(readsFile.getParentFile(), "soap-outputFile-",
            ".unmap");

    return createMapperProcessSE(soapPath,
        getIndexPath(archiveIndexDir, ".index.amb", 4), readsFile, outputFile,
        unmapFile, genomeDescription, true).getStout();
  }

  @Override
  protected InputStream internalMapPE(final File readsFile1,
      final File readsFile2, final File archiveIndexDir,
      final GenomeDescription genomeDescription) throws IOException {

    final String soapPath;

    synchronized (SYNC) {
      soapPath = install(MAPPER_EXECUTABLE);
    }

    final File outputFile =
        FileUtils.createTempFile(readsFile1.getParentFile(), "soap-output-",
            ".soap");
    final File unmapFile =
        FileUtils.createTempFile(readsFile1.getParentFile(), "soap-output-",
            ".unmap");
    final File unpairedFile =
        FileUtils.createTempFile(readsFile1.getParentFile(), "soap-output-",
            ".unpaired");

    return createMapperProcessPE(soapPath,
        getIndexPath(archiveIndexDir, ".index.amb", 4), readsFile1, readsFile2,
        outputFile, unmapFile, unpairedFile, genomeDescription, true)
        .getStout();
  }

  @Override
  protected MapperProcess internalMapSE(final File archiveIndex,
      final GenomeDescription genomeDescription) throws IOException {
    final String soapPath;

    synchronized (SYNC) {
      soapPath = install(MAPPER_EXECUTABLE);
    }

    // Get temp dir from Eoulsan Runtime
    final File tmpDir = EoulsanRuntime.getRuntime().getTempDirectory();

    final File outputFile =
        FileUtils.createTempFile(tmpDir, "soap-outputFile-", ".soap");
    final File unmapFile =
        FileUtils.createTempFile(tmpDir, "soap-outputFile-", ".unmap");

    return createMapperProcessSE(soapPath,
        getIndexPath(archiveIndex, ".index.amb", 4), null, outputFile,
        unmapFile, genomeDescription, false);
  }

  @Override
  protected MapperProcess internalMapPE(final File archiveIndex,
      final GenomeDescription genomeDescription) throws IOException {
    final String soapPath;

    synchronized (SYNC) {
      soapPath = install(MAPPER_EXECUTABLE);
    }

    // Get temp dir from Eoulsan Runtime
    final File tmpDir = EoulsanRuntime.getRuntime().getTempDirectory();

    final File outputFile =
        FileUtils.createTempFile(tmpDir, "soap-output-", ".soap");
    final File unmapFile =
        FileUtils.createTempFile(tmpDir, "soap-output-", ".unmap");
    final File unpairedFile =
        FileUtils.createTempFile(tmpDir, "soap-output-", ".unpaired");

    return createMapperProcessPE(soapPath,
        getIndexPath(archiveIndex, ".index.amb", 4), null, null, outputFile,
        unmapFile, unpairedFile, genomeDescription, true);
  }

  private MapperProcess createMapperProcessSE(final String soapPath,
      final String archivePath, final File readsFile, final File outputFile,
      final File unmapFile, final GenomeDescription genomeDescription,
      final boolean fileMode) throws IOException {

    return new MapperProcess(this, fileMode, false, false) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line
        final List<String> cmd = new ArrayList<>();

        cmd.add(soapPath);
        if (getListMapperArguments() != null) {
          cmd.addAll(getListMapperArguments());
        }
        cmd.add("-p");
        cmd.add(getThreadsNumber() + "");
        cmd.add("-a");
        if (fileMode) {
          cmd.add(readsFile.getAbsolutePath());
        } else {
          cmd.add(getTmpInputFile1().getAbsolutePath());
        }
        cmd.add("-D");
        cmd.add(archivePath);
        cmd.add("-o");
        cmd.add(outputFile.getAbsolutePath());
        cmd.add("-u");
        cmd.add(unmapFile.getAbsolutePath());

        return Collections.singletonList(cmd);
      }

      @Override
      protected InputStream createCustomInputStream(final InputStream stdout)
          throws FileNotFoundException {

        return new SequenceInputStream(convertSOAP2SAM(stdout,
            genomeDescription, isPairedEnd()),
            convertFasta2SAM(new FileInputStream(unmapFile)));
      }

      @Override
      protected void clean() {

        if (!outputFile.delete()) {
          getLogger().warning(
              "Cannot remove SOAP temporary file: " + outputFile);
        }

        if (!unmapFile.delete()) {
          getLogger()
              .warning("Cannot remove SOAP temporary file: " + unmapFile);
        }
      }

    };

  }

  private MapperProcess createMapperProcessPE(final String soapPath,
      final String archivePath, final File readsFile1, final File readsFile2,
      final File outputFile, final File unmapFile, final File unpairedFile,
      final GenomeDescription genomeDescription, final boolean fileMode)
      throws IOException {

    return new MapperProcess(this, true, false, true) {

      @Override
      protected List<List<String>> createCommandLines() {

        // Build the command line
        final List<String> cmd = new ArrayList<>();

        cmd.add(soapPath);
        if (getListMapperArguments() != null) {
          cmd.addAll(getListMapperArguments());
        }
        cmd.add("-p");
        cmd.add(getThreadsNumber() + "");
        cmd.add("-a");
        cmd.add(readsFile1.getAbsolutePath());
        cmd.add("-b");
        cmd.add(readsFile2.getAbsolutePath());
        cmd.add("-D");
        cmd.add(archivePath);
        cmd.add("-o");
        cmd.add(outputFile.getAbsolutePath());
        cmd.add("-u");
        cmd.add(unmapFile.getAbsolutePath());
        cmd.add("-2");
        cmd.add(unpairedFile.getAbsolutePath());

        return Collections.singletonList(cmd);
      }

      @Override
      protected InputStream createCustomInputStream(final InputStream stdout)
          throws FileNotFoundException {

        return new SequenceInputStream(convertSOAP2SAM(stdout,
            genomeDescription, isPairedEnd()),
            convertFasta2SAM(new FileInputStream(unmapFile)));
      }

      @Override
      protected void clean() {

        if (!outputFile.delete()) {
          getLogger().warning(
              "Cannot remove SOAP temporary file: " + outputFile);
        }

        if (!unmapFile.delete()) {
          getLogger()
              .warning("Cannot remove SOAP temporary file: " + unmapFile);
        }
      }

    };

  }

  private final static InputStream convertSOAP2SAM(final InputStream in,
      final GenomeDescription gd, final boolean pairedEnd) {

    final SOAP2SAM s2s = new SOAP2SAM(gd);

    return new MapperResult2SAMInputStream(in) {

      @Override
      protected List<String> transform(final String s) {

        if (s == null) {
          return s2s.last();
        }
        return s2s.c(s, pairedEnd);
      }

    };
  }

  private final static InputStream convertFasta2SAM(final InputStream in) {

    return new MapperResult2SAMInputStream(in) {

      private String id;

      @Override
      protected List<String> transform(final String s) {

        if (s == null || s.trim().length() == 0) {
          return null;
        }

        if (this.id == null) {
          this.id = s.substring(1).trim();
          return null;
        }

        final List<String> result =
            Collections.singletonList(this.id
                + "\t4\t*\t0\t0\t*\t*\t0\t0\t" + s + "\t*\t\n");
        this.id = null;

        return result;
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

    setMapperArguments(DEFAULT_ARGUMENTS);
    super.init(archiveIndexFile, archiveIndexDir, incrementer, counterGroup);
  }

}
