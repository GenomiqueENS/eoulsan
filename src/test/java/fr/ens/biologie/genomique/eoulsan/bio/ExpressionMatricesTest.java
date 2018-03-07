package fr.ens.biologie.genomique.eoulsan.bio;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class ExpressionMatricesTest {

  @Test
  public void testMerge() {

    ExpressionMatrix src = new DenseExpressionMatrix();
    ExpressionMatrix dest = new DenseExpressionMatrix();

    src.addColumns("a1", "a2", "a3", "b1", "b2", "b3");
    src.addRaws("raw1", "raw2");

    src.setValue("raw1", "a1", 1);
    src.setValue("raw1", "a2", 2);
    src.setValue("raw1", "a3", 4);
    src.setValue("raw1", "b1", 8);
    src.setValue("raw1", "b2", 16);
    src.setValue("raw1", "b3", 32);

    src.setValue("raw2", "a1", 101);
    src.setValue("raw2", "a2", 102);
    src.setValue("raw2", "a3", 104);
    src.setValue("raw2", "b1", 108);
    src.setValue("raw2", "b2", 116);
    src.setValue("raw2", "b3", 132);

    ExpressionMatrices.merge(src, Arrays.asList("a1", "a2", "a3"), dest, "a");

    assertEquals(2, dest.getRawCount());
    assertEquals(1, dest.getColumnCount());
    assertEquals(Arrays.asList("a"), dest.getColumnNames());

    assertEquals(7.0, dest.getValue("raw1", "a"), 0.0);
    assertEquals(307.0, dest.getValue("raw2", "a"), 0.0);

    ExpressionMatrices.merge(src, Arrays.asList("b1", "b2", "b3"), dest, "b");

    assertEquals(2, dest.getRawCount());
    assertEquals(2, dest.getColumnCount());
    assertEquals(Arrays.asList("a", "b"), dest.getColumnNames());

    assertEquals(7.0, dest.getValue("raw1", "a"), 0.0);
    assertEquals(307.0, dest.getValue("raw2", "a"), 0.0);
    assertEquals(56.0, dest.getValue("raw1", "b"), 0.0);
    assertEquals(356.0, dest.getValue("raw2", "b"), 0.0);
  }

  @Test
  public void testremoveEmptyRows() {

    ExpressionMatrix matrix = new DenseExpressionMatrix();

    matrix.addColumns("a1", "a2", "a3");
    matrix.addRaws("raw1", "raw2", "raw3", "raw4");

    matrix.setValue("raw1", "a1", 1);
    matrix.setValue("raw1", "a2", 2);
    matrix.setValue("raw1", "a3", 4);

    matrix.setValue("raw3", "a1", 101);
    matrix.setValue("raw3", "a2", 102);
    matrix.setValue("raw3", "a3", 104);

    assertEquals(4, matrix.getRawCount());

    ExpressionMatrices.removeEmptyRows(matrix);

    assertEquals(2, matrix.getRawCount());
    assertEquals(Arrays.asList("raw1", "raw3"), matrix.getRawNames());

  }

}
