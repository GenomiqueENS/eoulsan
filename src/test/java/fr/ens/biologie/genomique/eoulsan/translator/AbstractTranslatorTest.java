package fr.ens.biologie.genomique.eoulsan.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.translators.AddIdentifierTranslator;
import fr.ens.biologie.genomique.eoulsan.translators.MultiColumnTranslator;

public class AbstractTranslatorTest {

  private static final String[] ARRAY_FIELD = {"Col1", "Col2", "Col3", "Col4"};
  private static final String[] ARRAY_ROW_ONE = {"A", "1", "A1", "0"};
  private static final String[] ARRAY_ROW_TWO = {"B", "2", "B2", "0"};
  private static final String[] ARRAY_ROW_THREE = {"C", "3.0", "C3", "0"};
  private static final String[] ARRAY_ROW_FOUR = {"D", "4", "D4", "0"};
  private static final String[] ARRAY_ROW_FIVE = {"E", "4", "D4"};
  private static final String[] ARRAY_IDS = {"A", "B", "C", "D", "E"};
  private final MultiColumnTranslator transl =
      new MultiColumnTranslator(ARRAY_FIELD);

  @Test
  public void testNewTrans() {
    transl.addRow(ARRAY_ROW_ONE);
    AddIdentifierTranslator AddIdTransl = new AddIdentifierTranslator(transl);
    List<String> fields = AddIdTransl.getFields();
    assertTrue(AddIdTransl.isField(fields.get(0)));
    assertFalse(AddIdTransl.isField("missingField"));

    try {

      AddIdTransl.isField(null);
      assertTrue(false);
    } catch (NullPointerException e) {

      assertTrue(true);
    }
  }

  @Test
  public void testGetSetDefaultField() {

    assertEquals("Col2", transl.getDefaultField());

    try {

      transl.setDefaultField(null);
      assertTrue(false);
    } catch (RuntimeException e) {

      assertTrue(true);
    }

    transl.setDefaultField("Col3");
    assertEquals("Col3", transl.getDefaultField());
  }

  @Test
  public void testIsField() {

    List<String> fields = transl.getFields();
    assertTrue(transl.isField(fields.get(0)));
    assertFalse(transl.isField("missingField"));

    try {

      transl.isField(null);
      assertTrue(false);
    } catch (NullPointerException e) {

      assertTrue(true);
    }
  }

  @Test
  public void testGetIds() {

    transl.addRow(ARRAY_ROW_ONE);
    transl.addRow(ARRAY_ROW_TWO);
    transl.addRow(ARRAY_ROW_THREE);
    transl.addRow(ARRAY_ROW_FOUR);
    transl.addRow(ARRAY_ROW_FIVE);

    List<String> ids = transl.getIds();
    assertEquals(5, ids.size());

    for (String id : ARRAY_IDS) {
      assertTrue(ids.contains(id));
    }

  }

  @Test
  public void testTranslate() {

    transl.addRow(ARRAY_ROW_ONE);
    assertEquals("1", transl.translate("A").get(0));
    assertEquals("A1", transl.translate("A").get(1));
    assertEquals("0", transl.translate("A").get(2));

    try {

      transl.translate("").get(3);
      assertTrue(false);
    } catch (NullPointerException e) {

      assertTrue(true);
    }

    try {

      transl.translate("B").get(3);

      assertTrue(false);
    } catch (IndexOutOfBoundsException e) {

      assertTrue(true);
    }

  }

  @Test
  public void testTranslateField() {

    transl.addRow(ARRAY_ROW_ONE);
    assertEquals("1", transl.translateField("A", "Col2"));
    assertNull(transl.translateField("Col2", "A"));
    assertNull(transl.translateField("", ""));

  }

  @Test
  public void testIsLinkInfo() {
    transl.addRow(ARRAY_ROW_ONE);
    transl.addRow(ARRAY_ROW_TWO);
    transl.addRow(ARRAY_ROW_THREE);
    transl.addRow(ARRAY_ROW_FOUR);
    transl.addRow(ARRAY_ROW_FIVE);
    assertNull(transl.getLinkInfo("A", "Col2"));
  }

}
