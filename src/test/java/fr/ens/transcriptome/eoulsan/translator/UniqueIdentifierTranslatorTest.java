package fr.ens.transcriptome.eoulsan.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import fr.ens.transcriptome.eoulsan.translators.MultiColumnTranslator;
import fr.ens.transcriptome.eoulsan.translators.Translator;
import fr.ens.transcriptome.eoulsan.translators.UniqueIdentifierTranslator;

public class UniqueIdentifierTranslatorTest {

  private static final String[] ARRAY_FIELD = {"Col1", "Col2", "Col3", "Col4"};
  private static final String[] ARRAY_ROW_ONE = {"A", "1", "A1", "0"};
  private static final String[] ARRAY_ROW_TWO = {"B", "2", "B2", "0"};
  private static final String[] ARRAY_ROW_THREE = {"C", "3.0", "C3", "0"};
  private static final String[] ARRAY_ROW_FOUR = {"D", "4", "D4", "0"};
  private static final String[] ARRAY_ROW_FIVE = {"E", "4", "D4"};
  private static final String[] ARRAY_UNIQUE_IDS = {"A", "B", "C"};
  MultiColumnTranslator transl = new MultiColumnTranslator(ARRAY_FIELD);

  @Before
  public void setUp() throws Exception {
    transl.addRow(ARRAY_ROW_ONE);
    transl.addRow(ARRAY_ROW_TWO);
    transl.addRow(ARRAY_ROW_THREE);
    transl.addRow(ARRAY_ROW_FOUR);
    transl.addRow(ARRAY_ROW_FIVE);

  }

//  @Test
//  public void testGetReverseTranslatorUnique() {
//    UniqueIdentifierTranslator uniqIdTransl =
//        new UniqueIdentifierTranslator(ARRAY_UNIQUE_IDS, transl);
//    uniqIdTransl.setNewFieldName("Col3");
//    uniqIdTransl.updateFields();
//    UniqueIdentifierTranslator reverseTransl = new UniqueIdentifierTranslator(
//        ARRAY_UNIQUE_IDS, uniqIdTransl.getReverseTranslator());
////    System.out.println(reverseTransl.getFields()[0]);
////    System.out.println(reverseTransl.getIds());
////    assertEquals("A1", reverseTransl.translateField("UniqueId"));
////    assertEquals("0", reverseTransl.translateField("Col4", "3.0"));
//  }


  @Test
  public void testTranslateFieldUnique() {
    UniqueIdentifierTranslator uniqIdTransl =
        new UniqueIdentifierTranslator(ARRAY_UNIQUE_IDS, transl);
    uniqIdTransl.setNewFieldName("newField");
    uniqIdTransl.updateFields();
    assertEquals("A1", uniqIdTransl.translateField("1", "Col3"));
    assertEquals("0", uniqIdTransl.translateField("3.0", "Col4"));
  }

  @Test
  public void testUpdateandSetNewFieldName() {
    UniqueIdentifierTranslator uniqIdTransl =
        new UniqueIdentifierTranslator(ARRAY_UNIQUE_IDS, transl);

    uniqIdTransl.setNewFieldName("last field");
    String[] fields = uniqIdTransl.getFields();
    assertEquals("UniqueId", fields[0]);
    assertEquals("Col2", fields[1]);
    assertEquals("Col3", fields[2]);
    assertEquals("Col4", fields[3]);

    uniqIdTransl.updateFields();

    fields = uniqIdTransl.getFields();
    assertEquals("last field", fields[0]);
    assertEquals("Col2", fields[1]);
    assertEquals("Col3", fields[2]);
    assertEquals("Col4", fields[3]);
  }

  @Test
  public void testUniqueIdentifierTranslator() {
    try {

      new UniqueIdentifierTranslator(null, null);
      assertTrue(false);
    } catch (NullPointerException e) {

      assertTrue(true);
    }
  }

}
