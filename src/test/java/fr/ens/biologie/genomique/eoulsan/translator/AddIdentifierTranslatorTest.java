package fr.ens.biologie.genomique.eoulsan.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.translators.AddIdentifierTranslator;
import fr.ens.biologie.genomique.eoulsan.translators.MultiColumnTranslator;

public class AddIdentifierTranslatorTest {

  private static final String[] ARRAY_FIELD = {"Col1", "Col2", "Col3", "Col4"};
  MultiColumnTranslator transl = new MultiColumnTranslator(ARRAY_FIELD);

  @Test
  public void testAddIdentifierTranslator() {

    try {

      new AddIdentifierTranslator(null);
      assertTrue(false);
    } catch (RuntimeException e) {

      assertTrue(true);
    }

    AddIdentifierTranslator addIdTransl = new AddIdentifierTranslator(transl);
    String[] fields = addIdTransl.getFields();
    assertEquals("OriginalId", fields[0]);
  }

  @Test
  public void testSetGetUpdateFields() {

    AddIdentifierTranslator addIdTransl =
        new AddIdentifierTranslator(transl, "first field");
    addIdTransl.setNewFieldName("last field");
    String[] fields = addIdTransl.getFields();
    assertEquals("first field", fields[0]);
    assertEquals("Col2", fields[1]);
    assertEquals("Col3", fields[2]);
    assertEquals("Col4", fields[3]);

    addIdTransl.updateFields();

    fields = addIdTransl.getFields();
    assertEquals("last field", fields[0]);
    assertEquals("Col2", fields[1]);
    assertEquals("Col3", fields[2]);
    assertEquals("Col4", fields[3]);

  }

}
