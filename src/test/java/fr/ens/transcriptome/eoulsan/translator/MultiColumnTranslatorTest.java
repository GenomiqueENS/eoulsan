package fr.ens.transcriptome.eoulsan.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.transcriptome.eoulsan.translators.MultiColumnTranslator;

public class MultiColumnTranslatorTest {

  private static final String[] ARRAY_FIELD = {"Col1", "Col2", "Col3", "Col4"};
  private static final String[] ARRAY_ROW_ONE = {"A", "1", "A1", "0"};
  private static final String[] ARRAY_ROW_TWO = {"B", "2", "B2", "0"};
  private static final String[] ARRAY_ROW_THREE = {"C", "3.0", "C3", "0"};
  private static final String[] ARRAY_ROW_FOUR = {"D", "4", "D4", "0"};
  private static final String[] ARRAY_ROW_FIVE = {"E", "4", "D4"};
  MultiColumnTranslator transl = new MultiColumnTranslator(ARRAY_FIELD);

  @Test
  public void testAddRow() {

    transl.addRow(ARRAY_ROW_ONE);
    assertEquals("1", transl.translate("A")[0]);
    assertEquals("A1", transl.translate("A")[1]);
    assertEquals("0", transl.translate("A")[2]);

    transl.addRow(ARRAY_ROW_TWO);
    transl.addRow(ARRAY_ROW_THREE);
    transl.addRow(ARRAY_ROW_FOUR);
    transl.addRow(ARRAY_ROW_FIVE);

    assertEquals("4", transl.translate("D")[0]);
    assertNull(transl.translate("E")[2]);
  }

  @Test
  public void testGetSetFields() {

    String[] fields = transl.getFields();
    assertEquals("Col2", fields[0]);
    assertEquals("Col3", fields[1]);
    assertEquals("Col4", fields[2]);

  }

  @Test
  public void testClear() {

    transl.addRow(ARRAY_ROW_ONE);
    transl.addRow(ARRAY_ROW_TWO);
    transl.addRow(ARRAY_ROW_THREE);
    transl.addRow(ARRAY_ROW_FOUR);
    transl.addRow(ARRAY_ROW_FIVE);
    transl.clear();

    String[] afterClearFields = transl.getFields();

    assertEquals("Col2", afterClearFields[0]);
    String[] afterClearIds = transl.getIds();
    assertEquals(0, afterClearIds.length);
  }

  @Test
  public void testMultiColumnTranslatorStringEmpty() {

    try {

      new MultiColumnTranslator(null);
      assertTrue(false);
    } catch (NullPointerException e) {

      assertTrue(true);
    }
  }

}
