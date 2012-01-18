/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import java.io.File;
import java.io.IOException;

import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define a mapper for Gmap. This is a fake mapper that can be only
 * use to create index for gmap/gsnap as gmap does not read fastq file and not
 * generate SAM files.
 * @author Laurent Jourdren
 */
public class GmapReadsMapper extends AbstractSequenceReadsMapper {

  private static final String MAPPER_EXECUTABLE = "gmap";
  private static final String[] INDEXER_EXECUTABLES = new String[] {
      "fa_coords", "gmap_process", "gmapindex", "gmap_build"};

  public static final String DEFAULT_ARGUMENTS = "";

  private static final String SYNC = GmapReadsMapper.class.getName();
  private static final String MAPPER_NAME = "Gmap";

  @Override
  public String getMapperName() {

    return MAPPER_NAME;
  }

  @Override
  public boolean isSplitsAllowed() {

    return false;
  }
  
  @Override
  public boolean isIndexGeneratorOnly() {
    return true;
  }

  @Override
  public String getMapperVersion() {

    try {
      final String gmapPath;

      synchronized (SYNC) {
        gmapPath = install(MAPPER_EXECUTABLE);
      }

      final String cmd = gmapPath + " --version";

      final String s = ProcessUtils.execToString(cmd);

      if (s == null)
        return null;

      final String[] lines = s.split("\n");
      if (lines.length == 0)
        return null;

      final String[] tokens1 = lines[0].split(" version ");
      if (tokens1.length > 1) {
        final String[] tokens2 = tokens1[1].trim().split(" called ");
        if (tokens2.length > 1)
          return tokens2[0];
      }

      return null;

    } catch (IOException e) {

      return null;
    }
  }

  @Override
  public DataFormat getArchiveFormat() {

    return DataFormats.BOWTIE_INDEX_ZIP;
  }

  @Override
  protected String getIndexerExecutable() {

    return null;
  }

  @Override
  protected String[] getIndexerExecutables() {

    return INDEXER_EXECUTABLES.clone();
  }

  @Override
  protected String getIndexerCommand(String indexerPathname,
      String genomePathname) {

    final String binariesDirectory =
        new File(indexerPathname).getParentFile().getAbsolutePath();
    final String genomeDirectory =
        new File(genomePathname).getParentFile().getAbsolutePath();

    return indexerPathname
        + " -B " + binariesDirectory + " -D " + genomeDirectory + " -d genome "
        + genomePathname;
  }

  @Override
  public File getSAMFile(GenomeDescription gd) throws IOException {

    return null;
  }

  @Override
  public void clean() {
    // Do nothing
  }

  @Override
  protected void internalMap(File readsFile1, File readsFile2, File archiveIndex)
      throws IOException {
    // Do Nothing
  }

  @Override
  protected void internalMap(File readsFile, File archiveIndex)
      throws IOException {
    // Do Nothing
  }

  //
  // Init
  //

  @Override
  public void init(final boolean pairEnd, final FastqFormat fastqFormat,
      final ReporterIncrementer incrementer, final String counterGroup) {

    super.init(pairEnd, fastqFormat, incrementer, counterGroup);
    setMapperArguments(DEFAULT_ARGUMENTS);
  }

}
