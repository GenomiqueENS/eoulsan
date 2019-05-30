package fr.ens.biologie.genomique.eoulsan.translator;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.translators.MultiColumnTranslator;

import static org.junit.Assert.*;

public class MultiColumnTranslatorTest {

  private static final String[] ARRAY_FIELD = {"Col1", "Col2", "Col3", "Col4"};
  private static final String[] ARRAY_ROW_ONE = {"A", "1", "A1", "0"};
  private static final String[] ARRAY_ROW_TWO = {"B", "2", "B2", "0"};
  private static final String[] ARRAY_ROW_THREE = {"C", "3.0", "C3", "0"};
  private static final String[] ARRAY_ROW_FOUR = {"D", "4", "D4", "0"};
  private static final String[] ARRAY_ROW_FIVE = {"E", "4", "D4"};
  private final MultiColumnTranslator transl =
      new MultiColumnTranslator(ARRAY_FIELD);

  @Test
  public void testAddRow() {

    transl.addRow(ARRAY_ROW_ONE);
    assertEquals("1", transl.translate("A").get(0));
    assertEquals("A1", transl.translate("A").get(1));
    assertEquals("0", transl.translate("A").get(2));

    transl.addRow(ARRAY_ROW_TWO);
    transl.addRow(ARRAY_ROW_THREE);
    transl.addRow(ARRAY_ROW_FOUR);
    transl.addRow(ARRAY_ROW_FIVE);

    assertEquals("4", transl.translate("D").get(0));
    assertNull(transl.translate("E").get(2));
  }

  @Test
  public void testGetSetFields() {

    List<String> fields = transl.getFields();
    assertEquals("Col2", fields.get(0));
    assertEquals("Col3", fields.get(1));
    assertEquals("Col4", fields.get(2));

  }

  @Test
  public void testClear() {

    transl.addRow(ARRAY_ROW_ONE);
    transl.addRow(ARRAY_ROW_TWO);
    transl.addRow(ARRAY_ROW_THREE);
    transl.addRow(ARRAY_ROW_FOUR);
    transl.addRow(ARRAY_ROW_FIVE);
    transl.clear();

    List<String> afterClearFields = transl.getFields();

    assertEquals("Col2", afterClearFields.get(0));
    List<String> afterClearIds = transl.getIds();
    assertEquals(0, afterClearIds.size());
  }

  @Test
  public void testMultiColumnTranslatorStringEmpty() {

    try {

      new MultiColumnTranslator(new ArrayList<>());
        fail();
    } catch (RuntimeException e) {

      assertTrue(true);
    }
  }

}
