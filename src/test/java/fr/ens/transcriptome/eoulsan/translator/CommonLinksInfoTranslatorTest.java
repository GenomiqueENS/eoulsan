package fr.ens.transcriptome.eoulsan.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import fr.ens.transcriptome.eoulsan.translators.CommonLinksInfoTranslator;
import fr.ens.transcriptome.eoulsan.translators.MultiColumnTranslator;

public class CommonLinksInfoTranslatorTest {

  private static final String[] ARRAY_FIELD = {"Col1", "Col2", "Col3", "Col4"};
  private static final String[] ARRAY_ROW_ONE = {"A", "1", "A1", "0"};
  private static final String[] ARRAY_ROW_TWO = {"B", "2", "B2", "0"};
  private static final String[] ARRAY_ROW_THREE = {"C", "3.0", "C3", "0"};
  private static final String[] ARRAY_ROW_FOUR = {"D", "4", "D4", "0"};
  private static final String[] ARRAY_ROW_FIVE = {"E", "4", "D4"};
  MultiColumnTranslator transl = new MultiColumnTranslator(ARRAY_FIELD);

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
      assertTrue(false);
    } catch (RuntimeException e) {

      assertTrue(true);
    }

    CommonLinksInfoTranslator cmLinkInfoTransl =
        new CommonLinksInfoTranslator(transl);
    String[] fields = cmLinkInfoTransl.getFields();
    assertEquals("Col2", fields[0]);
  }

  @Test
  public void testIsLinkInfo() {
    CommonLinksInfoTranslator cmLinkInfoTransl =
        new CommonLinksInfoTranslator(transl);
    assertFalse(cmLinkInfoTransl.isLinkInfo(null));
    assertFalse(cmLinkInfoTransl.isLinkInfo("notLinkInfo"));
    assertTrue(cmLinkInfoTransl.isLinkInfo("EnsemblGeneID"));

  }

  @Test
  public void testGetLinkInfo() {
    CommonLinksInfoTranslator cmLinkInfoTransl =
        new CommonLinksInfoTranslator(transl);
    assertNull(cmLinkInfoTransl.getLinkInfo(new String(), new String()));


    assertEquals("http://www.ncbi.nlm.nih.gov/nuccore/TranslatedId",
        cmLinkInfoTransl.getLinkInfo("TranslatedId", "GI"));

    assertEquals(
        "http://www.ensembl.org/Multi/Search/Results?species=all;q=TranslatedId",
        cmLinkInfoTransl.getLinkInfo("TranslatedId", "EnsemblGeneID"));

    assertEquals(
        "http://www.ncbi.nlm.nih.gov/sites/entrez?Db=gene&Cmd=ShowDetailView&TermToSearch=TranslatedId",
        cmLinkInfoTransl.getLinkInfo("TranslatedId", "EntrezGeneID"));

    assertNull(cmLinkInfoTransl.getLinkInfo("TranslatedId", "MGI ID"));
    assertEquals(
        "http://www.informatics.jax.org/searches/accession_report.cgi?id=MGI%3ATranslatedId",
        cmLinkInfoTransl.getLinkInfo("MGI:TranslatedId", "MGI ID"));

    assertEquals("http://db.yeastgenome.org/cgi-bin/locus.pl?dbid=TranslatedId",
        cmLinkInfoTransl.getLinkInfo("TranslatedId", "SGDID"));

    assertEquals(
        "http://genome.jgi-psf.org/cgi-bin/dispGeneModel?db=Phatr2&tid=TranslatedId",
        cmLinkInfoTransl.getLinkInfo("TranslatedId",
            "Phatr2 Protein HyperLink"));

  }

}
