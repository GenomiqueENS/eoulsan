package fr.ens.transcriptome.eoulsan.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import fr.ens.transcriptome.eoulsan.translators.AddIdentifierTranslator;
import fr.ens.transcriptome.eoulsan.translators.MultiColumnTranslator;

public class AbstractTranslatorTest {

  private static final String[] ARRAY_FIELD = {"Col1", "Col2", "Col3", "Col4"};
  private static final String[] ARRAY_ROW_ONE = {"A", "1", "A1", "0"};
  private static final String[] ARRAY_ROW_TWO = {"B", "2", "B2", "0"};
  private static final String[] ARRAY_ROW_THREE = {"C", "3.0", "C3", "0"};
  private static final String[] ARRAY_ROW_FOUR = {"D", "4", "D4", "0"};
  private static final String[] ARRAY_ROW_FIVE = {"E", "4", "D4"};
  private static final String[] ARRAY_IDS = {"A", "B", "C", "D", "E"};
  MultiColumnTranslator transl = new MultiColumnTranslator(ARRAY_FIELD);

  @Test
  public void testNewTrans() {
    transl.addRow(ARRAY_ROW_ONE);
    AddIdentifierTranslator AddIdTransl = new AddIdentifierTranslator(transl);
    String[] fields = AddIdTransl.getFields();
    System.out.println(fields[0]);
    assertTrue(AddIdTransl.isField(fields[0]));
    assertFalse(AddIdTransl.isField("missingField"));
    assertFalse(AddIdTransl.isField(null));

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

    String[] fields = transl.getFields();
    assertTrue(transl.isField(fields[0]));
    assertFalse(transl.isField("missingField"));
    assertFalse(transl.isField(null));
  }

  @Test
  public void testGetIds() {

    transl.addRow(ARRAY_ROW_ONE);
    transl.addRow(ARRAY_ROW_TWO);
    transl.addRow(ARRAY_ROW_THREE);
    transl.addRow(ARRAY_ROW_FOUR);
    transl.addRow(ARRAY_ROW_FIVE);

    List<String> ids = Arrays.asList(transl.getIds());
    assertEquals(5, ids.size());

    for (String id : ARRAY_IDS) {
      assertTrue(ids.contains(id));
    }

  }

  @Test
  public void testTranslate() {

    transl.addRow(ARRAY_ROW_ONE);
    assertEquals("1", transl.translate("A")[0]);
    assertEquals("A1", transl.translate("A")[1]);
    assertEquals("0", transl.translate("A")[2]);
    assertNull(transl.translate(new String())[0]);
    assertNull(new String(), transl.translate("B")[0]);
  }

  @Test
  public void testTranslateField() {

    transl.addRow(ARRAY_ROW_ONE);
    assertEquals("1", transl.translateField("A", "Col2"));
    assertNull(transl.translateField("Col2", "A"));
    assertNull(transl.translateField(new String(), new String()));

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
