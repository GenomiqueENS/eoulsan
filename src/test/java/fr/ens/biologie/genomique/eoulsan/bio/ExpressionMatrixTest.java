package fr.ens.biologie.genomique.eoulsan.bio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class ExpressionMatrixTest {

  @Test
  public void testAddRaw() {

    ExpressionMatrix matrix = new ExpressionMatrix();

    assertEquals(0, matrix.getRawCount());

    matrix.addRaw("raw1");
    assertEquals(1, matrix.getRawCount());
    assertTrue(matrix.containsraw("raw1"));
    assertFalse(matrix.containsraw("raw2"));
    assertEquals(Arrays.asList("raw1"), matrix.getRawNames());

    matrix.addRaw("raw2");
    assertEquals(2, matrix.getRawCount());
    assertTrue(matrix.containsraw("raw1"));
    assertTrue(matrix.containsraw("raw2"));
    assertFalse(matrix.containsraw("raw3"));
    assertEquals(Arrays.asList("raw1", "raw2"), matrix.getRawNames());

    matrix.addColumn("col1");
    assertEquals(0.0, matrix.getValue("raw1", "col1"), 0.0);
    assertEquals(0.0, matrix.getValue("raw2", "col1"), 0.0);
  }

  @Test
  public void testAddColumn() {

    ExpressionMatrix matrix = new ExpressionMatrix();

    assertEquals(0, matrix.getRawCount());

    matrix.addColumn("col1");
    assertEquals(1, matrix.getColumnCount());
    assertTrue(matrix.containsColumn("col1"));
    assertFalse(matrix.containsColumn("col2"));
    assertEquals(Arrays.asList("col1"), matrix.getColumnNames());

    matrix.addColumn("col2");
    assertEquals(2, matrix.getColumnCount());
    assertTrue(matrix.containsColumn("col1"));
    assertTrue(matrix.containsColumn("col2"));
    assertFalse(matrix.containsColumn("col3"));
    assertEquals(Arrays.asList("col1", "col2"), matrix.getColumnNames());

    matrix.addRaw("raw1");
    assertEquals(0.0, matrix.getValue("raw1", "col1"), 0.0);
    assertEquals(0.0, matrix.getValue("raw1", "col2"), 0.0);
  }

  @Test
  public void testGetRawValues() {

    ExpressionMatrix matrix = new ExpressionMatrix();
    matrix.addColumns("col1", "col2");
    matrix.addRaws("raw1", "raw2", "raw3");

    matrix.setValue("raw1", "col1", 1);
    matrix.setValue("raw2", "col1", 2);
    matrix.setValue("raw3", "col1", 3);
    matrix.setValue("raw1", "col2", 4);
    matrix.setValue("raw2", "col2", 5);
    matrix.setValue("raw3", "col2", 6);

    assertEquals(Arrays.asList(1.0, 4.0), matrix.getRawValues("raw1"));
    assertEquals(Arrays.asList(2.0, 5.0), matrix.getRawValues("raw2"));
    assertEquals(Arrays.asList(3.0, 6.0), matrix.getRawValues("raw3"));
  }

  @Test
  public void testGetColumnValues() {

    ExpressionMatrix matrix = new ExpressionMatrix();
    matrix.addColumns("col1", "col2");
    matrix.addRaws("raw1", "raw2", "raw3");

    matrix.setValue("raw1", "col1", 1);
    matrix.setValue("raw2", "col1", 2);
    matrix.setValue("raw3", "col1", 3);
    matrix.setValue("raw1", "col2", 4);
    matrix.setValue("raw2", "col2", 5);
    matrix.setValue("raw3", "col2", 6);

    assertEquals(Arrays.asList(1.0, 2.0, 3.0), matrix.getColumnValues("col1"));
    assertEquals(Arrays.asList(4.0, 5.0, 6.0), matrix.getColumnValues("col2"));
  }

  @Test
  public void testGetValue() {

    ExpressionMatrix matrix = new ExpressionMatrix();
    matrix.addColumns("col1", "col2");
    matrix.addRaws("raw1", "raw2", "raw3");

    matrix.setValue("raw1", "col1", 1);
    matrix.setValue("raw2", "col1", 2);
    matrix.setValue("raw3", "col1", 3);
    matrix.setValue("raw1", "col2", 4);
    matrix.setValue("raw2", "col2", 5);
    matrix.setValue("raw3", "col2", 6);

    assertEquals(1.0, matrix.getValue("raw1", "col1"), 0.0);
    assertEquals(2.0, matrix.getValue("raw2", "col1"), 0.0);
    assertEquals(3.0, matrix.getValue("raw3", "col1"), 0.0);
    assertEquals(4.0, matrix.getValue("raw1", "col2"), 0.0);
    assertEquals(5.0, matrix.getValue("raw2", "col2"), 0.0);
    assertEquals(6.0, matrix.getValue("raw3", "col2"), 0.0);

    try {
      matrix.getValue("raw4", "col1");
      assertTrue(false);
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }

    try {
      matrix.getValue("raw1", "col4");
      assertTrue(false);
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }

  }

  @Test
  public void testAdd() {

    ExpressionMatrix matrix1 = new ExpressionMatrix();
    matrix1.addColumns("col1", "col2");
    matrix1.addRaws("raw1", "raw2", "raw3");

    matrix1.setValue("raw1", "col1", 1);
    matrix1.setValue("raw2", "col1", 2);
    matrix1.setValue("raw3", "col1", 3);
    matrix1.setValue("raw1", "col2", 4);
    matrix1.setValue("raw2", "col2", 5);
    matrix1.setValue("raw3", "col2", 6);

    ExpressionMatrix matrix2 = new ExpressionMatrix();
    matrix2.addColumns("col2", "col3");
    matrix2.addRaws("raw2", "raw3", "raw4");

    matrix2.setValue("raw2", "col2", 7);
    matrix2.setValue("raw3", "col2", 8);
    matrix2.setValue("raw4", "col2", 9);
    matrix2.setValue("raw2", "col3", 10);
    matrix2.setValue("raw3", "col3", 11);
    matrix2.setValue("raw4", "col3", 12);

    matrix1.add(matrix2);

    assertEquals(1.0, matrix1.getValue("raw1", "col1"), 0.0);
    assertEquals(2.0, matrix1.getValue("raw2", "col1"), 0.0);
    assertEquals(3.0, matrix1.getValue("raw3", "col1"), 0.0);
    assertEquals(0.0, matrix1.getValue("raw4", "col1"), 0.0);
    assertEquals(4.0, matrix1.getValue("raw1", "col2"), 0.0);
    assertEquals(7.0, matrix1.getValue("raw2", "col2"), 0.0);
    assertEquals(8.0, matrix1.getValue("raw3", "col2"), 0.0);
    assertEquals(9.0, matrix1.getValue("raw4", "col2"), 0.0);
    assertEquals(0.0, matrix1.getValue("raw1", "col3"), 0.0);
    assertEquals(10.0, matrix1.getValue("raw2", "col3"), 0.0);
    assertEquals(11.0, matrix1.getValue("raw3", "col3"), 0.0);
    assertEquals(12.0, matrix1.getValue("raw4", "col3"), 0.0);
  }

  @Test
  public void testRenameColumn() {

    ExpressionMatrix matrix = new ExpressionMatrix();

    assertEquals(0, matrix.getRawCount());

    matrix.addColumns("col1", "col2", "col3");
    assertEquals(3, matrix.getColumnCount());
    assertTrue(matrix.containsColumn("col2"));

    matrix.renameColumn("col2", "newcol2");
    assertEquals(3, matrix.getColumnCount());
    assertTrue(matrix.containsColumn("newcol2"));
    assertFalse(matrix.containsColumn("col2"));

    matrix.addRaws("raw1", "raw2");

    matrix.setValue("raw1", "col1", 1);
    matrix.setValue("raw2", "col1", 2);
    matrix.setValue("raw1", "newcol2", 3);
    matrix.setValue("raw2", "newcol2", 4);
    matrix.setValue("raw1", "col3", 5);
    matrix.setValue("raw2", "col3", 6);

    assertEquals(3.0, matrix.getValue("raw1", "newcol2"), 0.0);
    assertEquals(4.0, matrix.getValue("raw2", "newcol2"), 0.0);
    assertEquals(5.0, matrix.getValue("raw1", "col3"), 0.0);
    assertEquals(6.0, matrix.getValue("raw2", "col3"), 0.0);

    matrix.renameColumn("col3", "newcol3");
    assertEquals(3, matrix.getColumnCount());
    assertFalse(matrix.containsColumn("col3"));
    assertTrue(matrix.containsColumn("newcol3"));

    assertEquals(3.0, matrix.getValue("raw1", "newcol2"), 0.0);
    assertEquals(4.0, matrix.getValue("raw2", "newcol2"), 0.0);
    assertEquals(5.0, matrix.getValue("raw1", "newcol3"), 0.0);
    assertEquals(6.0, matrix.getValue("raw2", "newcol3"), 0.0);
  }

  @Test
  public void testSetDefaultValue() {

    ExpressionMatrix matrix = new ExpressionMatrix();

    assertEquals(0.0, matrix.getDefaultValue(), 0.0);

    matrix.addColumns("col1", "col2");
    matrix.addRaws("raw1", "raw2");

    assertEquals(0.0, matrix.getValue("raw1", "col1"), 0.0);
    assertEquals(0.0, matrix.getValue("raw2", "col1"), 0.0);
    assertEquals(0.0, matrix.getValue("raw1", "col2"), 0.0);
    assertEquals(0.0, matrix.getValue("raw2", "col2"), 0.0);

    matrix.setDefaultValue(1.0);
    matrix.addRaw("raw3");
    assertEquals(0.0, matrix.getValue("raw1", "col1"), 0.0);
    assertEquals(0.0, matrix.getValue("raw2", "col1"), 0.0);
    assertEquals(1.0, matrix.getValue("raw3", "col1"), 0.0);

    assertEquals(0.0, matrix.getValue("raw1", "col2"), 0.0);
    assertEquals(0.0, matrix.getValue("raw2", "col2"), 0.0);
    assertEquals(1.0, matrix.getValue("raw3", "col2"), 0.0);

    matrix.setDefaultValue(Double.NaN);
    matrix.addRaw("raw4");
    assertEquals(0.0, matrix.getValue("raw1", "col1"), 0.0);
    assertEquals(0.0, matrix.getValue("raw2", "col1"), 0.0);
    assertEquals(1.0, matrix.getValue("raw3", "col1"), 0.0);
    assertEquals(Double.NaN, matrix.getValue("raw4", "col1"), 0.0);

    assertEquals(0.0, matrix.getValue("raw1", "col2"), 0.0);
    assertEquals(0.0, matrix.getValue("raw2", "col2"), 0.0);
    assertEquals(1.0, matrix.getValue("raw3", "col2"), 0.0);
    assertEquals(Double.NaN, matrix.getValue("raw4", "col2"), 0.0);
  }

  @Test
  public void testRemoveRaw() {

    ExpressionMatrix matrix = new ExpressionMatrix();
    matrix.addColumns("col1", "col2");
    matrix.addRaws("raw1", "raw2", "raw3");

    matrix.setValue("raw1", "col1", 1);
    matrix.setValue("raw2", "col1", 2);
    matrix.setValue("raw3", "col1", 3);
    matrix.setValue("raw1", "col2", 4);
    matrix.setValue("raw2", "col2", 5);
    matrix.setValue("raw3", "col2", 6);

    assertEquals(3, matrix.getRawCount());
    assertEquals(2, matrix.getColumnCount());

    matrix.removeRaw("raw2");

    assertEquals(2, matrix.getRawCount());
    assertEquals(2, matrix.getColumnCount());

    assertEquals(1.0, matrix.getValue("raw1", "col1"), 0.0);
    assertEquals(3.0, matrix.getValue("raw3", "col1"), 0.0);
    assertEquals(4.0, matrix.getValue("raw1", "col2"), 0.0);
    assertEquals(6.0, matrix.getValue("raw3", "col2"), 0.0);
  }

  @Test
  public void testRemoveColumn() {

    ExpressionMatrix matrix = new ExpressionMatrix();
    matrix.addColumns("col1", "col2");
    matrix.addRaws("raw1", "raw2", "raw3");

    matrix.setValue("raw1", "col1", 1);
    matrix.setValue("raw2", "col1", 2);
    matrix.setValue("raw3", "col1", 3);
    matrix.setValue("raw1", "col2", 4);
    matrix.setValue("raw2", "col2", 5);
    matrix.setValue("raw3", "col2", 6);

    assertEquals(3, matrix.getRawCount());
    assertEquals(2, matrix.getColumnCount());

    matrix.removeColumn("col2");

    assertEquals(3, matrix.getRawCount());
    assertEquals(1, matrix.getColumnCount());

    assertEquals(1.0, matrix.getValue("raw1", "col1"), 0.0);
    assertEquals(2.0, matrix.getValue("raw2", "col1"), 0.0);
    assertEquals(3.0, matrix.getValue("raw3", "col1"), 0.0);
  }

}
