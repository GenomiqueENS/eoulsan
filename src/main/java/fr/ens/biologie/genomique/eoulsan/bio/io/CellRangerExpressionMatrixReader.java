package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Splitter;

/**
 * This class define a reader for CellRanger output matrix.
 * @author Laurent Jourdren
 * @since 2.2
 */
public class CellRangerExpressionMatrixReader
    extends MarketMatrixExpressionMatrixReader {

  private static final String MATRIX_FILENAME = "matrix.mtx";
  private static final String GENES_FILENAME = "genes.tsv";
  private static final String BARCODES_FILENAME = "barcodes.tsv";

  private List<String> geneNames = new ArrayList<>();
  private List<String> barcodesNames = new ArrayList<>();

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
  private static void loadList(final File file, final List<String> list)
      throws FileNotFoundException, IOException {

    final Splitter splitter = Splitter.on('\t').trimResults();

    try (final BufferedReader reader =
        new BufferedReader(new FileReader(file))) {

      String line;

      while ((line = reader.readLine()) != null) {

        line = line.trim();

        if (line.isEmpty()) {
          continue;
        }

        List<String> fields = splitter.splitToList(line);
        list.add(fields.get(0));
      }

    }

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

    super(new File(directory, MATRIX_FILENAME));

    // Load row and column names
    loadList(new File(directory, GENES_FILENAME), this.geneNames);
    loadList(new File(directory, BARCODES_FILENAME), this.barcodesNames);
  }

}
