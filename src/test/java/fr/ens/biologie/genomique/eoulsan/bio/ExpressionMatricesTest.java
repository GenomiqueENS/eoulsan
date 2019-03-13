package fr.ens.biologie.genomique.eoulsan.bio;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class ExpressionMatricesTest {

  @Test
  public void testMerge() {

    ExpressionMatrix src = new DenseExpressionMatrix();
    ExpressionMatrix dest = new DenseExpressionMatrix();

    src.addColumns("a1", "a2", "a3", "b1", "b2", "b3");
    src.addRows("row1", "row2");

    src.setValue("row1", "a1", 1);
    src.setValue("row1", "a2", 2);
    src.setValue("row1", "a3", 4);
    src.setValue("row1", "b1", 8);
    src.setValue("row1", "b2", 16);
    src.setValue("row1", "b3", 32);

    src.setValue("row2", "a1", 101);
    src.setValue("row2", "a2", 102);
    src.setValue("row2", "a3", 104);
    src.setValue("row2", "b1", 108);
    src.setValue("row2", "b2", 116);
    src.setValue("row2", "b3", 132);

    ExpressionMatrices.merge(src, Arrays.asList("a1", "a2", "a3"), dest, "a");

    assertEquals(2, dest.getRowCount());
    assertEquals(1, dest.getColumnCount());
    assertEquals(Collections.singletonList("a"), dest.getColumnNames());

    assertEquals(7.0, dest.getValue("row1", "a"), 0.0);
    assertEquals(307.0, dest.getValue("row2", "a"), 0.0);

    ExpressionMatrices.merge(src, Arrays.asList("b1", "b2", "b3"), dest, "b");

    assertEquals(2, dest.getRowCount());
    assertEquals(2, dest.getColumnCount());
    assertEquals(Arrays.asList("a", "b"), dest.getColumnNames());

    assertEquals(7.0, dest.getValue("row1", "a"), 0.0);
    assertEquals(307.0, dest.getValue("row2", "a"), 0.0);
    assertEquals(56.0, dest.getValue("row1", "b"), 0.0);
    assertEquals(356.0, dest.getValue("row2", "b"), 0.0);
  }

  @Test
  public void testremoveEmptyRows() {

    ExpressionMatrix matrix = new DenseExpressionMatrix();

    matrix.addColumns("a1", "a2", "a3");
    matrix.addRows("row1", "row2", "row3", "row4");

    matrix.setValue("row1", "a1", 1);
    matrix.setValue("row1", "a2", 2);
    matrix.setValue("row1", "a3", 4);

    matrix.setValue("row3", "a1", 101);
    matrix.setValue("row3", "a2", 102);
    matrix.setValue("row3", "a3", 104);

    assertEquals(4, matrix.getRowCount());

    ExpressionMatrices.removeEmptyRows(matrix);

    assertEquals(2, matrix.getRowCount());
    assertEquals(Arrays.asList("row1", "row3"), matrix.getRowNames());

  }

}
