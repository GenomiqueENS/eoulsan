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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.data.storages;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static java.util.Objects.requireNonNull;

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

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.bio.GenomeDescription;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFileMetadata;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

/**
 * This class define a basic GenomeDescStorage based on an index file.
 * @since 1.2
 * @author Laurent Jourdren
 */
public class SimpleGenomeDescStorage implements GenomeDescStorage {

  private static final String INDEX_FILENAME = "genomes_desc_storage.txt";

  private final DataFile dir;
  private final Map<String, IndexEntry> entries = new LinkedHashMap<>();
  private String lastMD5Computed;
  private DataFile lastGenomeFile;
  private long lastGenomeFileModified;

  /**
   * This inner class define an entry of the index file.
   * @author Laurent Jourdren
   */
  private static final class IndexEntry {

    String genomeName;
    long genomeFileLength;
    String genomeFileMD5Sum;
    DataFile file;

    private String getKey() {
      return createKey(this.genomeFileLength, this.genomeFileMD5Sum);
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName()
          + "{genomeName=" + this.genomeName + ", genomeFileLength="
          + this.genomeFileLength + ", genomeFileMD5Sum="
          + this.genomeFileMD5Sum + ", file=" + this.file + "}";
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

    if (!this.dir.exists()) {
      throw new IOException("Genome description storage directory not found: "
          + this.dir.getSource());
    }

    final DataFile indexFile = new DataFile(this.dir, INDEX_FILENAME);

    // Create an empty index file if no index exists
    if (!indexFile.exists()) {

      save();
      return;
    }

    final BufferedReader br = new BufferedReader(
        new InputStreamReader(indexFile.open(), Globals.DEFAULT_CHARSET));

    final Pattern pattern = Pattern.compile("\t");
    String line = null;

    while ((line = br.readLine()) != null) {

      final String trimmedLine = line.trim();
      if ("".equals(trimmedLine) || trimmedLine.startsWith("#")) {
        continue;
      }

      final List<String> fields = Arrays.asList(pattern.split(trimmedLine));

      if (fields.size() != 4) {
        continue;
      }

      final IndexEntry e = new IndexEntry();
      e.genomeName = fields.get(0);
      e.genomeFileMD5Sum = fields.get(1);
      e.genomeFileLength = Long.parseLong(fields.get(2));
      e.file = new DataFile(this.dir, fields.get(3));

      if (e.file.exists()) {
        this.entries.put(e.getKey(), e);
      }
    }

    br.close();
  }

  /**
   * Save the information in the index file
   * @throws IOException if an error occurs while saving the index file
   */
  private void save() throws IOException {

    if (!this.dir.exists()) {
      throw new IOException("Genome description storage directory not found: "
          + this.dir.getSource());
    }

    final DataFile indexFile = new DataFile(this.dir, INDEX_FILENAME);

    // Create an empty index file
    final BufferedWriter writer = new BufferedWriter(
        new OutputStreamWriter(indexFile.create(), Globals.DEFAULT_CHARSET));
    writer.write("#Genome\tGenomeFileMD5\tGenomeFileLength\n");

    for (Map.Entry<String, IndexEntry> e : this.entries.entrySet()) {

      IndexEntry ie = e.getValue();

      writer.append(ie.genomeName);
      writer.append("\t");
      writer.append(ie.genomeFileMD5Sum);
      writer.append("\t");
      writer.append(Long.toString(ie.genomeFileLength));
      writer.append("\t");
      writer.append(ie.file.getName());
      writer.append("\n");
    }

    writer.close();
  }

  //
  // Other methods
  //

  private String createKey(final DataFile genomeFile) {

    try {
      final DataFileMetadata md = genomeFile.getMetaData();

      final String md5Sum = computeMD5Sum(genomeFile);

      return createKey(md.getContentLength(), md5Sum);
    } catch (IOException e) {
      return null;
    }
  }

  private static String createKey(final long genomeFileLength,
      final String genomeFileMD5Sum) {

    return genomeFileMD5Sum + '\t' + genomeFileLength;
  }

  private String computeMD5Sum(final DataFile genomeFile) throws IOException {

    DataFileMetadata md = null;

    try {
      md = genomeFile.getMetaData();
    } catch (IOException e) {
    }

    if (md != null
        && genomeFile.equals(this.lastGenomeFile)
        && this.lastGenomeFileModified == md.getLastModified()
        && this.lastMD5Computed != null) {
      return this.lastMD5Computed;
    }

    final String md5Sum = FileUtils.computeMD5Sum(genomeFile.rawOpen());

    if (md != null && md5Sum != null) {
      this.lastGenomeFile = genomeFile;
      this.lastGenomeFileModified = md.getLastModified();
      this.lastMD5Computed = md5Sum;
    }

    return md5Sum;
  }

  //
  // Interface methods
  //

  @Override
  public GenomeDescription get(final DataFile genomeFile) {

    requireNonNull(genomeFile, "Genome file is null");

    final IndexEntry entry = this.entries.get(createKey(genomeFile));

    if (entry == null || entry.file == null) {
      return null;
    }

    try {
      return GenomeDescription.load(entry.file.open());
    } catch (IOException e) {
      getLogger()
          .warning("Cannot read genome description file: " + e.getMessage());
      return null;
    }
  }

  @Override
  public void put(final DataFile genomeFile,
      final GenomeDescription genomeDesc) {

    requireNonNull(genomeFile, "GenomeFile is null");
    requireNonNull(genomeDesc, "Genome description is null");

    final String key = createKey(genomeFile);

    if (this.entries.containsKey(key)) {
      return;
    }

    try {
      final DataFileMetadata md = genomeFile.getMetaData();

      final IndexEntry entry = new IndexEntry();
      entry.genomeName = genomeFile.getName();
      entry.genomeFileLength = md.getContentLength();
      entry.genomeFileMD5Sum = computeMD5Sum(genomeFile);

      entry.file = new DataFile(this.dir,
          entry.genomeFileMD5Sum + "_" + entry.genomeFileLength + ".gdesc");

      genomeDesc.save(entry.file.create());
      this.entries.put(entry.getKey(), entry);
      save();
      getLogger().info("Successfully added "
          + entry.genomeName
          + " genome description to genome description storage.");
    } catch (IOException e) {
      getLogger().warning(
          "Cannot add genome description file to genome description storage: "
              + e.getMessage());
    }

  }

  //
  // Static methods
  //

  /**
   * Create a GenomeDescStorage
   * @param dir the path of the genome descriptions storage
   * @return a GenomeDescStorage object if the path contains an index storage or
   *         null if no index storage is found
   */
  public static final GenomeDescStorage getInstance(final DataFile dir) {

    try {
      return new SimpleGenomeDescStorage(dir);
    } catch (IOException | NullPointerException e) {
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
  private SimpleGenomeDescStorage(final DataFile dir) throws IOException {

    requireNonNull(dir, "Index directory is null");

    this.dir = dir;
    load();

    getLogger().info("Genome description storage found. "
        + this.entries.size() + " entries in : " + dir.getSource());
  }

}
