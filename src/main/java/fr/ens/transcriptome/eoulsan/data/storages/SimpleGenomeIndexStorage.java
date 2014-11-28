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

package fr.ens.transcriptome.eoulsan.data.storages;

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.util.Utils.checkNotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define a basic GenomeIndexStorage based on an index file.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class SimpleGenomeIndexStorage implements GenomeIndexStorage {

  private static final String INDEX_FILENAME = "genomes_index_storage.txt";

  private final DataFile dir;
  private Map<String, IndexEntry> entries = new LinkedHashMap<>();

  /**
   * This inner class define an entry of the index file.
   * @author Laurent Jourdren
   */
  private static final class IndexEntry {

    String genomeName;
    int sequences;
    long length;
    String genomeMD5;
    String mapperName;
    DataFile file;

    String getKey() {
      return createKey(this.mapperName, this.genomeMD5);
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName()
          + "{genomeName=" + genomeName + ", sequences=" + sequences
          + ", length=" + length + ", genomeMD5=" + genomeMD5
          + ", mapperName= " + mapperName + ", file=" + file + "}";
    }
  };

  //
  // Interface methods
  //

  @Override
  public DataFile get(final SequenceReadsMapper mapper,
      final GenomeDescription genome) {

    checkNotNull(mapper, "Mapper is null");
    checkNotNull(mapper, "Genome description is null");

    final IndexEntry entry = this.entries.get(createKey(mapper, genome));

    return entry == null ? null : entry.file;
  }

  @Override
  public void put(final SequenceReadsMapper mapper,
      final GenomeDescription genome, DataFile indexArchive) {

    checkNotNull(mapper, "Mapper is null");
    checkNotNull(genome, "Genome description is null");
    checkNotNull(indexArchive, "IndexArchive is null");

    if (!indexArchive.exists())
      return;

    final String key = createKey(mapper, genome);

    if (entries.containsKey(key))
      return;

    final IndexEntry entry = new IndexEntry();
    entry.genomeName = genome.getGenomeName().trim();
    entry.sequences = genome.getSequenceCount();
    entry.length = genome.getGenomeLength();
    entry.genomeMD5 = genome.getMD5Sum().trim();
    entry.mapperName = mapper.getMapperName().toLowerCase().trim();
    entry.file =
        new DataFile(dir, entry.mapperName + "-" + entry.genomeMD5 + ".zip");

    try {
      FileUtils.copy(indexArchive.rawOpen(), entry.file.create());
      this.entries.put(entry.getKey(), entry);
      save();
      getLogger().info(
          "Successully added "
              + indexArchive.getName()
              + " index archive to genome index storage.");
    } catch (IOException e) {
    }
  }

  //
  // Index management methods
  //

  /**
   * Load the information from the index file
   * @throws IOException if an error occurs while loading the index file
   */
  private void load() throws IOException {

    if (!this.dir.exists())
      throw new IOException("Genome index storage directory not found: "
          + this.dir.getSource());

    final DataFile indexFile = new DataFile(dir, INDEX_FILENAME);

    // Create an empty index file if no index exists
    if (!indexFile.exists()) {

      save();
      return;
    }

    final BufferedReader br =
        new BufferedReader(new InputStreamReader(indexFile.open(),
            Globals.DEFAULT_CHARSET));

    final Pattern pattern = Pattern.compile("\t");
    String line = null;

    while ((line = br.readLine()) != null) {

      final String trimmedLine = line.trim();
      if ("".equals(trimmedLine) || trimmedLine.startsWith("#"))
        continue;

      final List<String> fields = Arrays.asList(pattern.split(trimmedLine));

      if (fields.size() != 6)
        continue;

      final IndexEntry e = new IndexEntry();
      e.genomeName = fields.get(0);
      e.genomeMD5 = fields.get(1);
      e.mapperName = fields.get(4);
      e.file = new DataFile(dir, fields.get(5));

      if (e.file.exists())
        this.entries.put(e.getKey(), e);
    }

    br.close();
  }

  /**
   * Save the information in the index file
   * @throws IOException if an error occurs while saving the index file
   */
  private void save() throws IOException {

    if (!this.dir.exists())
      throw new IOException("Genome index storage directory not found: "
          + this.dir.getSource());

    final DataFile indexFile = new DataFile(dir, INDEX_FILENAME);

    // Create an empty index file
    final BufferedWriter writer =
        new BufferedWriter(new OutputStreamWriter(indexFile.create(),
            Globals.DEFAULT_CHARSET));
    writer
        .write("#Genome\tGenomeMD5\tGenomeSequences\tGenomeLength\tMapper\tIndexFile\n");

    for (Map.Entry<String, IndexEntry> e : this.entries.entrySet()) {

      IndexEntry ie = e.getValue();

      writer.append(ie.genomeName == null ? "???" : ie.genomeName);
      writer.append("\t");
      writer.append(ie.genomeMD5);
      writer.append("\t");
      writer.append(Integer.toString(ie.sequences));
      writer.append("\t");
      writer.append(Long.toString(ie.length));
      writer.append("\t");
      writer.append(ie.mapperName);
      writer.append("\t");
      writer.append(ie.file.getName());
      writer.append("\n");
    }

    writer.close();
  }

  //
  // Other methods
  //

  private static final String createKey(final SequenceReadsMapper mapper,
      final GenomeDescription genome) {

    return createKey(mapper.getMapperName(), genome.getMD5Sum());
  }

  private static final String createKey(final String mapperName,
      final String genomeMD5) {

    return mapperName.toLowerCase().trim() + '\t' + genomeMD5;
  }

  //
  // Static methods
  //

  /**
   * Create a GenomeIndexStorage
   * @param dir the path of the index storage
   * @return a GenomeIndexStorage object if the path contains an index storage
   *         or null if no index storage is found
   */
  public static final GenomeIndexStorage getInstance(final DataFile dir) {

    try {
      return new SimpleGenomeIndexStorage(dir);
    } catch (IOException e) {
      return null;
    } catch (NullPointerException e) {
      return null;
    }
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   * @param dir Path to the index storage
   * @throws IOException if an error occurs while testing the index storage
   */
  private SimpleGenomeIndexStorage(final DataFile dir) throws IOException {

    checkNotNull(dir, "Index directory is null");

    this.dir = dir;
    load();

    getLogger().info(
        "Genome index storage found."
            + this.entries.size() + " entries in : " + dir.getSource());
  }

}
