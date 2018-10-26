package fr.ens.biologie.genomique.eoulsan.bio.io;

import static fr.ens.biologie.genomique.eoulsan.bio.io.CellRangerExpressionMatrixReader.BARCODES_FILENAME;
import static fr.ens.biologie.genomique.eoulsan.bio.io.CellRangerExpressionMatrixReader.GENES_FILENAME;
import static fr.ens.biologie.genomique.eoulsan.bio.io.CellRangerExpressionMatrixReader.MATRIX_FILENAME;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import fr.ens.biologie.genomique.eoulsan.bio.AnnotationMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.ExpressionMatrix;

/**
 * This class define a writer to save matrix saved in Cell Ranger format.
 * @author Laurent Jourdren
 * @since 2.4
 */
public class CellRangerExpressionMatrixWriter
    extends MarketMatrixExpressionMatrixWriter {

  private File barcodesFile;
  private File genesFile;
  private Map<String, String> geneAliases;

  @Override
  public void write(final ExpressionMatrix matrix,
      final Collection<String> rowNamesToWrite) throws IOException {

    // Write matrix
    super.write(matrix, rowNamesToWrite);

    // Write barcodes
    writeBarcodes(matrix);

    // Write genes
    writeGenes(matrix, rowNamesToWrite);
  }

  /**
   * Write barcodes.
   * @param matrix the matrix
   * @throws IOException if an error occurs while writing the barcodes
   */
  private void writeBarcodes(final ExpressionMatrix matrix) throws IOException {

    try (Writer writer = new FileWriter(this.barcodesFile)) {

      for (String barcode : matrix.getColumnNames()) {
        writer.write(barcode + '\n');
      }
    }
  }

  /**
   * Write genes.
   * @param matrix the matrix
   * @throws IOException if an error occurs while writing the genes
   */
  private void writeGenes(final ExpressionMatrix matrix,
      final Collection<String> rowNamesToWrite) throws IOException {

    try (Writer writer = new FileWriter(this.genesFile)) {

      for (String geneName : matrix.getRowNames()) {

        if (rowNamesToWrite.contains(geneName)) {

          String alias = this.geneAliases.get(geneName);
          writer.write(geneName + '\t' + (alias == null ? "" : alias) + '\n');
        }
      }
    }
  }

  private static Map<String, String> extractGeneAliases(
      final AnnotationMatrix annotation, final String geneAliasesField)
      throws IOException {

    Objects.requireNonNull(annotation, "annotation parameter cannot be null");
    Objects.requireNonNull(geneAliasesField,
        "geneAliasesField parameter cannot be null");

    if (!annotation.containsColumn(geneAliasesField)) {
      throw new IOException("Unknown field in annotation: " + geneAliasesField);
    }

    Map<String, String> result = new HashMap<>();

    for (String key : annotation.getRowNames()) {
      result.put(key, annotation.getValue(key, geneAliasesField));
    }

    return result;
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param directory CellRanger matrix directory
   * @throws IOException if an error occurs while reading the TSV files
   */
  public CellRangerExpressionMatrixWriter(final File directory)
      throws IOException {

    this(directory, Collections.emptyMap());
  }

  /**
   * Public constructor
   * @param directory CellRanger matrix directory
   * @param annotation the annotation matrix
   * @param annotationField the field of the annotation matrix that contains the
   *          gene aliases
   * @throws IOException if an error occurs while reading the TSV files
   */
  public CellRangerExpressionMatrixWriter(final File directory,
      final AnnotationMatrix annotation, final String annotationField)
      throws IOException {

    this(directory, extractGeneAliases(annotation, annotationField));
  }

  /**
   * Public constructor
   * @param directory CellRanger matrix directory
   * @param geneAliases gene aliases
   * @throws IOException if an error occurs while reading the TSV files
   */
  public CellRangerExpressionMatrixWriter(final File directory,
      final Map<String, String> geneAliases) throws IOException {

    super(new File(directory, MATRIX_FILENAME));

    Objects.requireNonNull(geneAliases, "geneAliases parameter cannot be null");

    this.genesFile = new File(directory, GENES_FILENAME);
    this.barcodesFile = new File(directory, BARCODES_FILENAME);
    this.geneAliases = geneAliases;
  }

}
