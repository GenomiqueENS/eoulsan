package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.util.GuavaCompatibility;

/**
 * This class define a reader for CellRanger output matrix.
 * @author Laurent Jourdren
 * @since 2.2
 */
public class CellRangerExpressionMatrixReader
    extends MarketMatrixExpressionMatrixReader {

  static final int DEFAULT_CELL_RANGER_FORMAT = 1;

  static final String MATRIX_FILENAME = "matrix.mtx";
  static final String GENES_FILENAME = "genes.tsv";
  static final String BARCODES_FILENAME = "barcodes.tsv";

  static final String MATRIX_V2_FILENAME = "matrix.mtx.gz";
  static final String GENES_V2_FILENAME = "features.tsv.gz";
  static final String BARCODES_V2_FILENAME = "barcodes.tsv.gz";

  private List<String> geneNames = new ArrayList<>();
  private List<String> barcodesNames = new ArrayList<>();
  private Map<String, String> geneAliases = new HashMap<>();

  /**
   * Get gene aliases.
   * @return the gene aliases
   */
  public Map<String, String> getGeneAliases() {

    return this.geneAliases;
  }

  @Override
  protected String getRowName(final int rowNumber) {

    return this.geneNames.get(rowNumber - 1);
  }

  @Override
  protected String getColumnName(final int columnNumber) {

    return this.barcodesNames.get(columnNumber - 1);
  }

  /**
   * Load a list of features in TSV format.
   * @param file the file to load
   * @param list the list to file
   * @throws FileNotFoundException if the file does not exists
   * @throws IOException if an error occurs while reading the file
   */
  private static void loadList(final File file, final List<String> list,
      final Map<String, String> aliases)
      throws FileNotFoundException, IOException {

    final Splitter splitter = Splitter.on('\t').trimResults();

    try (final BufferedReader reader = createReader(file)) {

      String line;

      while ((line = reader.readLine()) != null) {

        line = line.trim();

        if (line.isEmpty()) {
          continue;
        }

        List<String> fields = GuavaCompatibility.splitToList(splitter, line);
        list.add(fields.get(0));
        if (aliases != null && fields.size() > 1) {
          aliases.put(fields.get(0), fields.get(1));
        }
      }

    }

  }

  /**
   * Create a reader that can read GZipped files if filename ends with ".gz"
   * extension.
   * @param file the file to read
   * @return a BufferedReader object
   * @throws IOException
   */
  private static BufferedReader createReader(final File file)
      throws IOException {

    if (file.getName().endsWith(".gz")) {

      return new BufferedReader(new InputStreamReader(
          new GZIPInputStream(new FileInputStream(file))));
    }

    return new BufferedReader(new FileReader(file));
  }

  /**
   * Check format version.
   * @param formatVersion the format version
   * @return the format version if valid
   */
  static int checkCellRangerFormatVersion(int formatVersion) {

    if (formatVersion < 1 || formatVersion > 2) {
      throw new IllegalArgumentException(
          "Invalid format version: " + formatVersion);
    }

    return formatVersion;
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   * @param directory CellRanger matrix directory
   * @throws IOException if an error occurs while reading the TSV files
   */
  public CellRangerExpressionMatrixReader(final File directory)
      throws IOException {

    this(directory, DEFAULT_CELL_RANGER_FORMAT);
  }

  /**
   * Public constructor
   * @param directory CellRanger matrix directory
   * @param formatVersion Cell Ranger format version
   * @throws IOException if an error occurs while reading the TSV files
   */
  public CellRangerExpressionMatrixReader(final File directory,
      final int formatVersion) throws IOException {

    super(new File(directory, checkCellRangerFormatVersion(formatVersion) == 2
        ? MATRIX_V2_FILENAME : MATRIX_FILENAME));

    // Load row and column names
    loadList(
        new File(directory,
            formatVersion == 2 ? GENES_V2_FILENAME : GENES_FILENAME),
        this.geneNames, this.geneAliases);
    loadList(
        new File(directory,
            formatVersion == 2 ? BARCODES_V2_FILENAME : BARCODES_FILENAME),
        this.barcodesNames, null);
  }

}
