package fr.ens.biologie.genomique.eoulsan.bio.io;

import static fr.ens.biologie.genomique.eoulsan.bio.io.CellRangerExpressionMatrixReader.BARCODES_FILENAME;
import static fr.ens.biologie.genomique.eoulsan.bio.io.CellRangerExpressionMatrixReader.BARCODES_V2_FILENAME;
import static fr.ens.biologie.genomique.eoulsan.bio.io.CellRangerExpressionMatrixReader.DEFAULT_CELL_RANGER_FORMAT;
import static fr.ens.biologie.genomique.eoulsan.bio.io.CellRangerExpressionMatrixReader.GENES_FILENAME;
import static fr.ens.biologie.genomique.eoulsan.bio.io.CellRangerExpressionMatrixReader.GENES_V2_FILENAME;
import static fr.ens.biologie.genomique.eoulsan.bio.io.CellRangerExpressionMatrixReader.MATRIX_FILENAME;
import static fr.ens.biologie.genomique.eoulsan.bio.io.CellRangerExpressionMatrixReader.MATRIX_V2_FILENAME;
import static fr.ens.biologie.genomique.eoulsan.bio.io.CellRangerExpressionMatrixReader.checkCellRangerFormatVersion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;

import fr.ens.biologie.genomique.eoulsan.bio.AnnotationMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.ExpressionMatrix;

/**
 * This class define a writer to save matrix saved in Cell Ranger format.
 * @author Laurent Jourdren
 * @since 2.4
 */
public class CellRangerExpressionMatrixWriter
    extends MarketMatrixExpressionMatrixWriter {

  public static final String DEFAULT_FEATURE_TYPE = "Gene Expression";

  private File barcodesFile;
  private File featuresFile;
  private Map<String, String> geneAliases;
  private final String featureType;

  @Override
  public void write(final ExpressionMatrix matrix,
      final Collection<String> rowNamesToWrite) throws IOException {

    // Write matrix
    super.write(matrix, rowNamesToWrite);

    // Write barcodes
    writeBarcodes(matrix);

    // Write genes
    writeFeatures(matrix, rowNamesToWrite);
  }

  /**
   * Write barcodes.
   * @param matrix the matrix
   * @throws IOException if an error occurs while writing the barcodes
   */
  private void writeBarcodes(final ExpressionMatrix matrix) throws IOException {

    try (Writer writer = createWriter(this.barcodesFile)) {

      for (String barcode : matrix.getColumnNames()) {
        writer.write(barcode + '\n');
      }
    }
  }

  /**
   * Write features.
   * @param matrix the matrix
   * @throws IOException if an error occurs while writing the features
   */
  private void writeFeatures(final ExpressionMatrix matrix,
      final Collection<String> rowNamesToWrite) throws IOException {

    try (Writer writer = createWriter(this.featuresFile)) {

      for (String geneName : matrix.getRowNames()) {

        if (rowNamesToWrite.contains(geneName)) {

          String alias = this.geneAliases.get(geneName);
          writer.write(geneName
              + '\t' + (alias == null ? "" : alias)
              + (this.featureType != null ? ("\t" + this.featureType) : "")
              + '\n');
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

  /**
   * Create a writer that can writer GZipped files if filename ends with ".gz"
   * extension.
   * @param file the file to write
   * @return a BufferedReader object
   * @throws IOException
   */
  private static Writer createWriter(final File file) throws IOException {

    if (file.getName().endsWith(".gz")) {

      return new OutputStreamWriter(
          new GZIPOutputStream(new FileOutputStream(file)));
    }

    return new FileWriter(file);
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

    this(directory, DEFAULT_CELL_RANGER_FORMAT);
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

    this(directory, annotation, annotationField, DEFAULT_CELL_RANGER_FORMAT);
  }

  /**
   * Public constructor
   * @param directory CellRanger matrix directory
   * @param geneAliases gene aliases
   * @throws IOException if an error occurs while reading the TSV files
   */
  public CellRangerExpressionMatrixWriter(final File directory,
      final Map<String, String> geneAliases) throws IOException {

    this(directory, geneAliases, DEFAULT_CELL_RANGER_FORMAT, null);
  }

  /**
   * Public constructor
   * @param directory CellRanger matrix directory
   * @param formatVersion Cell Ranger format version
   * @throws IOException if an error occurs while reading the TSV files
   */
  public CellRangerExpressionMatrixWriter(final File directory,
      final int formatVersion) throws IOException {

    this(directory, Collections.emptyMap(), formatVersion, null);
  }

  /**
   * Public constructor
   * @param directory CellRanger matrix directory
   * @param annotation the annotation matrix
   * @param annotationField the field of the annotation matrix that contains the
   *          gene aliases
   * @param formatVersion Cell Ranger format version
   * @throws IOException if an error occurs while reading the TSV files
   */
  public CellRangerExpressionMatrixWriter(final File directory,
      final AnnotationMatrix annotation, final String annotationField,
      final int formatVersion) throws IOException {

    this(directory, annotation, annotationField, formatVersion, null);
  }

  /**
   * Public constructor
   * @param directory CellRanger matrix directory
   * @param annotation the annotation matrix
   * @param annotationField the field of the annotation matrix that contains the
   *          gene aliases
   * @param formatVersion Cell Ranger format version
   * @param featureType feature type, the value of the third column of
   *          features.tsv.gz file
   * @throws IOException if an error occurs while reading the TSV files
   */
  public CellRangerExpressionMatrixWriter(final File directory,
      final AnnotationMatrix annotation, final String annotationField,
      final int formatVersion, final String featureType) throws IOException {

    this(directory, extractGeneAliases(annotation, annotationField),
        formatVersion, featureType);
  }

  /**
   * Public constructor
   * @param directory CellRanger matrix directory
   * @param geneAliases gene aliases
   * @param formatVersion format version
   * @param featureType feature type, the value of the third column of
   *          features.tsv.gz file
   * @throws IOException if an error occurs while reading the TSV files
   */
  public CellRangerExpressionMatrixWriter(final File directory,
      final Map<String, String> geneAliases, final int formatVersion,
      final String featureType) throws IOException {

    super(new File(directory, checkCellRangerFormatVersion(formatVersion) == 2
        ? MATRIX_V2_FILENAME : MATRIX_FILENAME));

    Objects.requireNonNull(geneAliases, "geneAliases parameter cannot be null");

    this.featureType = formatVersion == 1
        ? null
        : (featureType == null ? DEFAULT_FEATURE_TYPE : featureType.trim());

    this.featuresFile = new File(directory,
        formatVersion == 2 ? GENES_V2_FILENAME : GENES_FILENAME);
    this.barcodesFile = new File(directory,
        formatVersion == 2 ? BARCODES_V2_FILENAME : BARCODES_FILENAME);
    this.geneAliases = geneAliases;
  }

}
