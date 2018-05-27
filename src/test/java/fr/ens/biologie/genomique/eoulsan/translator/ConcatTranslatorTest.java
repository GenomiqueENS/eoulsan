package fr.ens.biologie.genomique.eoulsan.translator;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.translators.ConcatTranslator;
import fr.ens.biologie.genomique.eoulsan.translators.MultiColumnTranslator;

public class ConcatTranslatorTest {

  private static final String[] ARRAY_FIELD_A =
      {"Col1", "Col2", "Col3", "CommonCol"};
  private static final String[] ARRAY_FIELD_B =
      {"Col5", "Col6", "CommonCol", "Col8"};
  private static final String[] ARRAY_ROW_ONE = {"A", "1", "A1", "0"};
  private static final String[] ARRAY_ROW_TWO = {"B", "2", "B2", "0"};
  private static final String[] ARRAY_ROW_THREE = {"C", "3.0", "C3", "0"};
  private static final String[] ARRAY_ROW_FOUR = {"D", "4", "D4", "0"};
  private static final String[] ARRAY_ROW_FIVE = {"E", "4", "D4"};
  private final MultiColumnTranslator translA =
      new MultiColumnTranslator(ARRAY_FIELD_A);
  private final MultiColumnTranslator translB =
      new MultiColumnTranslator(ARRAY_FIELD_B);

  @Before
  public void setUp() throws Exception {
    translA.addRow(ARRAY_ROW_ONE);
    translA.addRow(ARRAY_ROW_TWO);
    translB.addRow(ARRAY_ROW_THREE);
    translB.addRow(ARRAY_ROW_FOUR);
    translB.addRow(ARRAY_ROW_FIVE);

  }

  @Test
  public void testAddTranslator() {
    ConcatTranslator concatTransl = new ConcatTranslator(translA, translB);
    List<String> fields = concatTransl.getFields();
    assertEquals("Col2", fields.get(0));
    assertEquals("Col3", fields.get(1));
    assertEquals("CommonCol", fields.get(2));
    assertEquals("Col6", fields.get(3));
    assertEquals("Col8", fields.get(4));
  }

}
