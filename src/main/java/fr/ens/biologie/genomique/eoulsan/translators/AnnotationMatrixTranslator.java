package fr.ens.biologie.genomique.eoulsan.translators;

import java.util.List;
import java.util.Objects;

import fr.ens.biologie.genomique.eoulsan.bio.AnnotationMatrix;

/**
 * This class implements a translator using an AnnotationMatrix.
 * @since 2.4
 * @author Laurent Jourdren
 */
public class AnnotationMatrixTranslator extends AbstractTranslator {

  private final AnnotationMatrix matrix;

  @Override
  public List<String> getFields() {

    return this.matrix.getColumnNames();
  }

  @Override
  public String translateField(final String id, final String field) {

    if (id == null || !this.matrix.containsRow(id)) {
      return null;
    }

    return this.matrix.getValue(id, field == null ? getDefaultField() : field);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param matrix matrix to use
   */
  public AnnotationMatrixTranslator(final AnnotationMatrix matrix) {

    Objects.requireNonNull(matrix, "matrix argument cannot be null");

    this.matrix = matrix;
  }

}
