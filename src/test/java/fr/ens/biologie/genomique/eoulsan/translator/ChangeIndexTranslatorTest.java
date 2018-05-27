package fr.ens.biologie.genomique.eoulsan.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.translators.ChangeIndexTranslator;
import fr.ens.biologie.genomique.eoulsan.translators.MultiColumnTranslator;

public class ChangeIndexTranslatorTest {

  private static final String[] ARRAY_FIELD = {"Col1", "Col2", "Col3", "Col4"};
  private static final String[] ARRAY_ROW_ONE = {"A", "1", "A1", "0"};
  private static final String[] ARRAY_ROW_TWO = {"B", "2", "B2", "0"};
  private static final String[] ARRAY_ROW_THREE = {"C", "3.0", "C3", "0"};
  private static final String[] ARRAY_ROW_FOUR = {"D", "4", "D4", "0"};
  private static final String[] ARRAY_ROW_FIVE = {"E", "4", "D4"};
  private final MultiColumnTranslator transl =
      new MultiColumnTranslator(ARRAY_FIELD);

  @Before
  public void setUp() throws Exception {
    transl.addRow(ARRAY_ROW_ONE);
    transl.addRow(ARRAY_ROW_TWO);
    transl.addRow(ARRAY_ROW_THREE);
    transl.addRow(ARRAY_ROW_FOUR);
    transl.addRow(ARRAY_ROW_FIVE);

  }

  @Test
  public void testCreateAndIndex() {
    try {

      new ChangeIndexTranslator(null, null);
      assertTrue(false);
    } catch (RuntimeException e) {

      assertTrue(true);
    }
    try {

      new ChangeIndexTranslator(transl, "unknown");
      assertTrue(false);
    } catch (RuntimeException e) {

      assertTrue(true);
    }

    ChangeIndexTranslator ChangeIdxTransl =
        new ChangeIndexTranslator(transl, "Col3");
    List<String> fields = ChangeIdxTransl.getFields();
    assertEquals("Col2", fields.get(0));
    assertEquals("Col4", fields.get(1));
    assertEquals("1", ChangeIdxTransl.translateField("A", "Col2"));
    assertEquals("2", ChangeIdxTransl.translateField("B", "Col2"));
    assertNull(ChangeIdxTransl.translateField("Col2", "A"));
    assertNull(ChangeIdxTransl.translateField("", ""));
  }

}
