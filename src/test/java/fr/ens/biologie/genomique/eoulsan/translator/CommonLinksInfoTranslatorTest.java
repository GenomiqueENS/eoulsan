package fr.ens.biologie.genomique.eoulsan.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.translators.CommonLinksInfoTranslator;
import fr.ens.biologie.genomique.eoulsan.translators.MultiColumnTranslator;

public class CommonLinksInfoTranslatorTest {

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
  public void testCommonLinksInfoTranslatorTranslator() {

    try {

      new CommonLinksInfoTranslator(null);
      fail();
    } catch (RuntimeException e) {

      assertTrue(true);
    }

    CommonLinksInfoTranslator cmLinkInfoTransl =
        new CommonLinksInfoTranslator(transl);
    List<String> fields = cmLinkInfoTransl.getFields();
    assertEquals("Col2", fields.get(0));
  }

  @Test
  public void testIsLinkInfo() {
    CommonLinksInfoTranslator cmLinkInfoTransl =
        new CommonLinksInfoTranslator(transl);
    assertFalse(cmLinkInfoTransl.isLinkInfo(null));
    assertFalse(cmLinkInfoTransl.isLinkInfo("notLinkInfo"));
    assertTrue(cmLinkInfoTransl.isLinkInfo("EnsemblID"));

  }

  @Test
  public void testGetLinkInfo() throws UnsupportedEncodingException {
    CommonLinksInfoTranslator cmLinkInfoTransl =
        new CommonLinksInfoTranslator(transl);
    assertNull(cmLinkInfoTransl.getLinkInfo("", ""));

    assertEquals("http://www.ncbi.nlm.nih.gov/nuccore/TranslatedId",
        cmLinkInfoTransl.getLinkInfo("TranslatedId", "GI"));

    assertEquals("http://www.ensembl.org/id/TranslatedId",
        cmLinkInfoTransl.getLinkInfo("TranslatedId", "EnsemblID"));

    assertEquals(
        "http://www.ncbi.nlm.nih.gov/sites/entrez?Db=gene&Cmd=ShowDetailView&TermToSearch=TranslatedId",
        cmLinkInfoTransl.getLinkInfo("TranslatedId", "EntrezGeneID"));

    assertEquals("http://www.informatics.jax.org/marker/MGI%253ATranslatedId",
        cmLinkInfoTransl.getLinkInfo(
            URLEncoder.encode("MGI:TranslatedId", "UTF-8"), "MGI ID"));

    assertEquals("http://db.yeastgenome.org/cgi-bin/locus.pl?dbid=TranslatedId",
        cmLinkInfoTransl.getLinkInfo("TranslatedId", "SGDID"));

    assertEquals(
        "http://genome.jgi-psf.org/cgi-bin/dispGeneModel?db=Phatr2&tid=TranslatedId",
        cmLinkInfoTransl.getLinkInfo("TranslatedId",
            "Phatr2 Protein HyperLink"));

  }

}
