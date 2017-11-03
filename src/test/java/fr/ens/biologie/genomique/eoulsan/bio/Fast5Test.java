package fr.ens.biologie.genomique.eoulsan.bio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Date;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.bio.Fast5.ChemistryVersion;
import fr.ens.biologie.genomique.eoulsan.bio.Fast5.Status;
import fr.ens.biologie.genomique.eoulsan.bio.Fast5.Type;
import fr.ens.biologie.genomique.eoulsan.bio.Fast5.Version;

public class Fast5Test {
  private final String file1 = "/fast5/alexander_PC_20161027_R9-4_1D.fast5";
  private final String file2 =
      "/fast5/dnacpc14_20160617_R7_2D_prebasecalling.fast5";
  private final String file3 = "/fast5/dnacpc14_20160617_R7_2D.fast5";
  private final String file4 =
      "/fast5/dnacpc14_20161011_R9_2D_prebasecalling.fast5";
  private final String file5 = "/fast5/dnacpc14_20161011_R9_2D.fast5";
  private final String file6 =
      "/fast5/dnacpc14_20170124_R9-4_2D_prebasecalling.fast5";
  private final String file7 = "/fast5/dnacpc14_20170124_R9-4_2D.fast5";
  private final String file8 =
      "/fast5/dnacpc14_20170328_R9-4_1D_prebasecalling.fast5";
  private final String file9 =
      "/fast5/dnacpc14_20170328_R9-4_1D_albacore.fast5";

  /**
   * Read an input stream.
   */
  private static String readInputStream(String path) {

    InputStream is = Fast5Test.class.getResourceAsStream(path);
    StringBuilder sb = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      String s;
      while ((s = br.readLine()) != null) {
        sb.append(s);
        sb.append('\n');
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return sb.toString();
  }

  private File getResourceAsFile(String path) {
    URL resource = Fast5Test.class.getResource(path);
    try {
      return Paths.get(resource.toURI()).toFile();
    } catch (URISyntaxException e) {
      return null;
    }
  }

  @Test
  public void testReadVersion() throws ParseException {

    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(Version.V1_1, testf1.getVersion());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getVersion());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(Version.V1_1, testf3.getVersion());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getVersion());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(Version.V1_1, testf5.getVersion());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getVersion());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals(Version.V1_1, testf7.getVersion());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getVersion());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(Version.V1_1, testf9.getVersion());
    testf9.close();

  }

  @Test
  public void testReadStatus() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(Status.AFTER_BASECALLING, testf1.getStatus());
    assertNotEquals(Status.PRE_BASECALLING, testf1.getStatus());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(Status.PRE_BASECALLING, testf2.getStatus());
    assertNotEquals(Status.AFTER_BASECALLING, testf2.getStatus());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(Status.AFTER_BASECALLING, testf3.getStatus());
    assertNotEquals(Status.PRE_BASECALLING, testf3.getStatus());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(Status.PRE_BASECALLING, testf4.getStatus());
    assertNotEquals(Status.AFTER_BASECALLING, testf4.getStatus());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(Status.AFTER_BASECALLING, testf5.getStatus());
    assertNotEquals(Status.PRE_BASECALLING, testf5.getStatus());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(Status.PRE_BASECALLING, testf6.getStatus());
    assertNotEquals(Status.AFTER_BASECALLING, testf6.getStatus());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals(Status.AFTER_BASECALLING, testf7.getStatus());
    assertNotEquals(Status.PRE_BASECALLING, testf7.getStatus());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertNotEquals(Status.AFTER_BASECALLING, testf8.getStatus());
    assertEquals(Status.PRE_BASECALLING, testf8.getStatus());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(Status.AFTER_BASECALLING, testf9.getStatus());
    assertNotEquals(Status.PRE_BASECALLING, testf9.getStatus());
    testf9.close();

  }

  @Test
  public void testReadType() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(Type.TYPE_1D, testf1.getType());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getType());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(Type.TYPE_2D, testf3.getType());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getType());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(Type.TYPE_2D, testf5.getType());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getType());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals(Type.TYPE_2D, testf7.getType());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getType());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(Type.TYPE_1D, testf9.getType());
    testf9.close();

  }

  @Test
  public void testReadRVersion() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(ChemistryVersion.R9_4, testf1.getChemistryVersion());
    assertNotEquals(ChemistryVersion.R9, testf1.getChemistryVersion());
    assertNotEquals(ChemistryVersion.R7_3, testf1.getChemistryVersion());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getChemistryVersion());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(ChemistryVersion.R7_3, testf3.getChemistryVersion());
    assertNotEquals(ChemistryVersion.R9, testf3.getChemistryVersion());
    assertNotEquals(ChemistryVersion.R9_4, testf3.getChemistryVersion());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getChemistryVersion());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(ChemistryVersion.R9, testf5.getChemistryVersion());
    assertNotEquals(ChemistryVersion.R9_4, testf5.getChemistryVersion());
    assertNotEquals(ChemistryVersion.R7_3, testf5.getChemistryVersion());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getChemistryVersion());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals(ChemistryVersion.R9_4, testf7.getChemistryVersion());
    assertNotEquals(ChemistryVersion.R9, testf7.getChemistryVersion());
    assertNotEquals(ChemistryVersion.R7_3, testf7.getChemistryVersion());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getChemistryVersion());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(ChemistryVersion.R9_4, testf9.getChemistryVersion());
    assertNotEquals(ChemistryVersion.R9, testf9.getChemistryVersion());
    assertNotEquals(ChemistryVersion.R7_3, testf9.getChemistryVersion());
    testf9.close();

  }

  @Test
  public void testReadBasecaller() throws ParseException {

    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(Fast5.Basecaller.METRICHOR, testf1.getBasecaller());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getBasecaller());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(Fast5.Basecaller.METRICHOR, testf3.getBasecaller());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getBasecaller());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(Fast5.Basecaller.METRICHOR, testf5.getBasecaller());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getBasecaller());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals(Fast5.Basecaller.METRICHOR, testf7.getBasecaller());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getBasecaller());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(Fast5.Basecaller.ALBACORE, testf9.getBasecaller());
    testf9.close();

  }

  //
  //
  // macro
  //
  //

  @Test
  public void testIsBarcoded() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertFalse(testf1.isBarcoded());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertFalse(testf2.isBarcoded());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertFalse(testf3.isBarcoded());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertFalse(testf4.isBarcoded());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertFalse(testf5.isBarcoded());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertFalse(testf6.isBarcoded());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals(true, testf7.isBarcoded());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertFalse(testf8.isBarcoded());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(true, testf9.isBarcoded());
    testf9.close();
  }

  @Test
  public void testIsBasecalled() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(true, testf1.isBasecalled());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(false, testf2.isBasecalled());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(true, testf3.isBasecalled());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(false, testf4.isBasecalled());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(true, testf5.isBasecalled());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(false, testf6.isBasecalled());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals(true, testf7.isBasecalled());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(false, testf8.isBasecalled());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(true, testf9.isBasecalled());
    testf9.close();
  }

  @Test
  public void testIs2D() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(false, testf1.is2D());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(false, testf2.is2D());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(true, testf3.is2D());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(false, testf4.is2D());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(true, testf5.is2D());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(false, testf6.is2D());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals(true, testf7.is2D());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(false, testf8.is2D());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(false, testf9.is2D());
    testf9.close();
  }

  //
  //
  // primary information getters
  //
  //

  //
  // UniqueGlobalkey Group Information
  //
  @Test
  public void testGetNumMinION() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals("MN17351", testf1.getNumMinION());
    assertNotEquals(null, testf1.getNumMinION());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals("MN16592", testf2.getNumMinION());
    assertNotEquals(null, testf2.getNumMinION());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals("MN16592", testf3.getNumMinION());
    assertNotEquals(null, testf3.getNumMinION());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals("MN17734", testf4.getNumMinION());
    assertNotEquals(null, testf4.getNumMinION());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals("MN17734", testf5.getNumMinION());
    assertNotEquals(null, testf5.getNumMinION());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals("MN17734", testf6.getNumMinION());
    assertNotEquals(null, testf6.getNumMinION());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals("MN17734", testf7.getNumMinION());
    assertNotEquals(null, testf7.getNumMinION());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals("MN17734", testf8.getNumMinION());
    assertNotEquals(null, testf8.getNumMinION());
    testf7.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals("MN17734", testf9.getNumMinION());
    assertNotEquals(null, testf9.getNumMinION());
    testf9.close();
  }

  @Test
  public void testGetFlowcellId() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals("FAB44188", testf1.getFlowcellId());
    assertNotEquals(null, testf1.getFlowcellId());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals("FAA105486", testf2.getFlowcellId());
    assertNotEquals(null, testf2.getFlowcellId());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals("FAA105486", testf3.getFlowcellId());
    assertNotEquals(null, testf3.getFlowcellId());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals("FAD22491", testf4.getFlowcellId());
    assertNotEquals(null, testf4.getFlowcellId());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals("FAD22491", testf5.getFlowcellId());
    assertNotEquals(null, testf5.getFlowcellId());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals("FAF03829", testf6.getFlowcellId());
    assertNotEquals(null, testf6.getFlowcellId());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals("FAF03829", testf7.getFlowcellId());
    assertNotEquals(null, testf7.getFlowcellId());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals("FAF04250", testf8.getFlowcellId());
    assertNotEquals(null, testf8.getFlowcellId());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals("FAF04250", testf9.getFlowcellId());
    assertNotEquals(null, testf9.getFlowcellId());
    testf9.close();
  }

  @Test
  public void testGetMinknowVersion() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals("1.1.15", testf1.getMinknowVersion());
    assertNotEquals(null, testf1.getMinknowVersion());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals("0.51.3.40 b201605171140", testf2.getMinknowVersion());
    assertNotEquals(null, testf2.getMinknowVersion());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals("0.51.3.40 b201605171140", testf3.getMinknowVersion());
    assertNotEquals(null, testf3.getMinknowVersion());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals("1.0.8", testf4.getMinknowVersion());
    assertNotEquals(null, testf4.getMinknowVersion());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals("1.0.8", testf5.getMinknowVersion());
    assertNotEquals(null, testf5.getMinknowVersion());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals("1.3.25", testf6.getMinknowVersion());
    assertNotEquals(null, testf6.getMinknowVersion());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals("1.3.25", testf7.getMinknowVersion());
    assertNotEquals(null, testf7.getMinknowVersion());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals("1.5.5", testf8.getMinknowVersion());
    assertNotEquals(null, testf8.getMinknowVersion());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals("1.5.5", testf9.getMinknowVersion());
    assertNotEquals(null, testf9.getMinknowVersion());
    testf9.close();
  }

  @Test
  public void testGetDateExpMetrichor() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    long Dateint1 = 1477580791;
    Date datef1 = new Date(Dateint1 * 1000);
    assertEquals(datef1, testf1.getDateExpMetrichor());
    assertNotEquals(null, testf1.getDateExpMetrichor());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getDateExpMetrichor());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    long Dateint3 = 1466159977;
    Date datef3 = new Date(Dateint3 * 1000);
    assertEquals(datef3, testf3.getDateExpMetrichor());
    assertNotEquals(null, testf3.getDateExpMetrichor());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getDateExpMetrichor());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    long Dateint5 = 1476200754;
    Date datef5 = new Date(Dateint5 * 1000);
    assertEquals(datef5, testf5.getDateExpMetrichor());
    assertNotEquals(null, testf5.getDateExpMetrichor());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getDateExpMetrichor());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    long Dateint7 = 1485252889;
    Date datef7 = new Date(Dateint7 * 1000);
    assertEquals(datef7, testf7.getDateExpMetrichor());
    assertNotEquals(null, testf7.getDateExpMetrichor());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getDateExpMetrichor());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(null, testf9.getDateExpMetrichor());
    testf9.close();
  }

  @Test
  public void testGetDateExpAlbacore() throws ParseException {

    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(null, testf1.getDateExpAlbacore());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getDateExpAlbacore());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(null, testf3.getDateExpAlbacore());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getDateExpAlbacore());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(null, testf5.getDateExpAlbacore());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getDateExpAlbacore());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals(null, testf7.getDateExpAlbacore());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getDateExpAlbacore());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals("2017-03-28T15:36:30Z", testf9.getDateExpAlbacore());
    assertNotEquals(null, testf9.getDateExpAlbacore());
    testf9.close();
  }

  @Test
  public void testGetProtocolRunId() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals("4bb23786-e23f-4d30-9b17-c7ee511fa306",
        testf1.getProtocolRunId());
    assertNotEquals(null, testf1.getProtocolRunId());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals("0cc6bdd9-6487-48be-a9cc-ccad13f25bcf",
        testf2.getProtocolRunId());
    assertNotEquals(null, testf2.getProtocolRunId());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals("0cc6bdd9-6487-48be-a9cc-ccad13f25bcf",
        testf3.getProtocolRunId());
    assertNotEquals(null, testf3.getProtocolRunId());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals("a5dddcb4-2268-4aa1-b44b-641ac2977f1b",
        testf4.getProtocolRunId());
    assertNotEquals(null, testf4.getProtocolRunId());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals("a5dddcb4-2268-4aa1-b44b-641ac2977f1b",
        testf5.getProtocolRunId());
    assertNotEquals(null, testf5.getProtocolRunId());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals("7c54fd3b-e517-4542-9f70-0cbdb36db17b",
        testf6.getProtocolRunId());
    assertNotEquals(null, testf6.getProtocolRunId());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals("7c54fd3b-e517-4542-9f70-0cbdb36db17b",
        testf7.getProtocolRunId());
    assertNotEquals(null, testf7.getProtocolRunId());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals("c03d869d-810e-4b55-96d8-ca8bca45fa7a",
        testf8.getProtocolRunId());
    assertNotEquals(null, testf8.getProtocolRunId());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals("c03d869d-810e-4b55-96d8-ca8bca45fa7a",
        testf9.getProtocolRunId());
    assertNotEquals(null, testf9.getProtocolRunId());
    testf9.close();
  }

  @Test
  public void testGetHostname() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals("alexander-PC", testf1.getHostname());
    assertNotEquals(null, testf1.getHostname());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals("dnacpc14", testf2.getHostname());
    assertNotEquals(null, testf2.getHostname());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals("dnacpc14", testf3.getHostname());
    assertNotEquals(null, testf3.getHostname());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals("dnacpc14", testf4.getHostname());
    assertNotEquals(null, testf4.getHostname());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals("dnacpc14", testf5.getHostname());
    assertNotEquals(null, testf5.getHostname());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals("dnacpc14", testf6.getHostname());
    assertNotEquals(null, testf6.getHostname());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals("dnacpc14", testf7.getHostname());
    assertNotEquals(null, testf7.getHostname());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals("dnacpc14", testf8.getHostname());
    assertNotEquals(null, testf8.getHostname());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals("dnacpc14", testf9.getHostname());
    assertNotEquals(null, testf9.getHostname());
    testf9.close();
  }

  @Test
  public void testGetOS() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals("Windows 6.1", testf1.getOS());
    assertNotEquals(null, testf1.getOS());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals("Windows 6.1", testf2.getOS());
    assertNotEquals(null, testf2.getOS());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals("Windows 6.1", testf3.getOS());
    assertNotEquals(null, testf3.getOS());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals("Windows 6.1", testf4.getOS());
    assertNotEquals(null, testf4.getOS());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals("Windows 6.1", testf5.getOS());
    assertNotEquals(null, testf5.getOS());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals("Windows 6.1", testf6.getOS());
    assertNotEquals(null, testf6.getOS());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals("Windows 6.1", testf7.getOS());
    assertNotEquals(null, testf7.getOS());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals("Windows 6.1", testf8.getOS());
    assertNotEquals(null, testf8.getOS());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals("Windows 6.1", testf9.getOS());
    assertNotEquals(null, testf9.getOS());
    testf9.close();
  }

  @Test
  public void testGetExperimentKit() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals("genomic_dna", testf1.getExperimentKit());
    assertNotEquals(null, testf1.getExperimentKit());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals("lambda_burn_in", testf2.getExperimentKit());
    assertNotEquals(null, testf2.getExperimentKit());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals("lambda_burn_in", testf3.getExperimentKit());
    assertNotEquals(null, testf3.getExperimentKit());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals("genomic_dna", testf4.getExperimentKit());
    assertNotEquals(null, testf4.getExperimentKit());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals("genomic_dna", testf5.getExperimentKit());
    assertNotEquals(null, testf5.getExperimentKit());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals("genomic_dna", testf6.getExperimentKit());
    assertNotEquals(null, testf6.getExperimentKit());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals("genomic_dna", testf7.getExperimentKit());
    assertNotEquals(null, testf7.getExperimentKit());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals("genomic_dna", testf8.getExperimentKit());
    assertNotEquals(null, testf8.getExperimentKit());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals("genomic_dna", testf9.getExperimentKit());
    assertNotEquals(null, testf9.getExperimentKit());
    testf9.close();
  }

  @Test
  public void testGetExperimentType() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals("customer_qc", testf1.getExperimentType());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getExperimentType());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(null, testf3.getExperimentType());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals("customer_qc", testf4.getExperimentType());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals("customer_qc", testf5.getExperimentType());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getExperimentType());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals(null, testf7.getExperimentType());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals("customer_qc", testf8.getExperimentType());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals("customer_qc", testf9.getExperimentType());
    testf9.close();
  }

  @Test
  public void testGetSampleFrequency() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(4000, testf1.getSampleFrequency());
    assertNotEquals(null, testf1.getSampleFrequency());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(3012, testf2.getSampleFrequency());
    assertNotEquals(null, testf2.getSampleFrequency());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(3012, testf3.getSampleFrequency());
    assertNotEquals(null, testf3.getSampleFrequency());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(4000, testf4.getSampleFrequency());
    assertNotEquals(null, testf4.getSampleFrequency());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(4000, testf5.getSampleFrequency());
    assertNotEquals(null, testf5.getSampleFrequency());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(4000, testf6.getSampleFrequency());
    assertNotEquals(null, testf6.getSampleFrequency());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals(4000, testf7.getSampleFrequency());
    assertNotEquals(null, testf7.getSampleFrequency());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(4000, testf8.getSampleFrequency());
    assertNotEquals(null, testf8.getSampleFrequency());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(4000, testf9.getSampleFrequency());
    assertNotEquals(null, testf9.getSampleFrequency());
    testf9.close();
  }

  @Test
  public void testGetChannelNumber() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    int test = 96;
    assertEquals(test, testf1.getChannelNumber());
    assertNotEquals(null, testf1.getChannelNumber());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(96, testf2.getChannelNumber());
    assertNotEquals(null, testf2.getChannelNumber());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(96, testf3.getChannelNumber());
    assertNotEquals(null, testf3.getChannelNumber());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(96, testf4.getChannelNumber());
    assertNotEquals(null, testf4.getChannelNumber());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(96, testf5.getChannelNumber());
    assertNotEquals(null, testf5.getChannelNumber());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(10, testf6.getChannelNumber());
    assertNotEquals(null, testf6.getChannelNumber());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals(10, testf7.getChannelNumber());
    assertNotEquals(null, testf7.getChannelNumber());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(261, testf8.getChannelNumber());
    assertNotEquals(null, testf8.getChannelNumber());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(261, testf9.getChannelNumber());
    assertNotEquals(null, testf9.getChannelNumber());
    testf9.close();
  }

  //
  //
  // basecalling information getters
  //
  //
  @Test
  public void testGetNumberRead() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(1000, testf1.getNumberRead());
    assertNotEquals(null, testf1.getNumberRead());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(563, testf2.getNumberRead());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(293, testf3.getNumberRead());
    assertNotEquals(null, testf3.getNumberRead());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(77, testf4.getNumberRead());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(14, testf5.getNumberRead());
    assertNotEquals(null, testf5.getNumberRead());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(6, testf6.getNumberRead());
    assertNotEquals(null, testf6.getNumberRead());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals(6, testf7.getNumberRead());
    assertNotEquals(null, testf7.getNumberRead());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(46035, testf8.getNumberRead());
    assertNotEquals(null, testf8.getNumberRead());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(46035, testf9.getNumberRead());
    assertNotEquals(null, testf9.getNumberRead());
    testf9.close();
  }

  @Test
  public void testGetSubModuleMetrichorVersion() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals("chimaera v1.23.3 | dragonet v1.23.0",
        testf1.getSubModuleMetrichorVersion());
    assertNotEquals(null, testf1.getSubModuleMetrichorVersion());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getSubModuleMetrichorVersion());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals("chimaera v1.22.6 | dragonet v1.22.2",
        testf3.getSubModuleMetrichorVersion());
    assertNotEquals(null, testf3.getSubModuleMetrichorVersion());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getSubModuleMetrichorVersion());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals("chimaera v1.22.10 | dragonet v1.22.4",
        testf5.getSubModuleMetrichorVersion());
    assertNotEquals(null, testf5.getSubModuleMetrichorVersion());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getSubModuleMetrichorVersion());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals("chimaera v1.23.4 | dragonet v1.23.0",
        testf7.getSubModuleMetrichorVersion());
    assertNotEquals(null, testf7.getSubModuleMetrichorVersion());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getSubModuleMetrichorVersion());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(null, testf9.getSubModuleMetrichorVersion());
    testf9.close();
  }

  @Test
  public void testGetAlbacoreVersion() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(null, testf1.getAlbacoreVersion());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf1.getAlbacoreVersion());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(null, testf1.getAlbacoreVersion());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf1.getAlbacoreVersion());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(null, testf1.getAlbacoreVersion());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf1.getAlbacoreVersion());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals(null, testf1.getAlbacoreVersion());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf1.getAlbacoreVersion());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals("1.0.2", testf9.getAlbacoreVersion());
    testf9.close();
  }

  @Test
  public void testGetTemplateLength() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(1193, testf1.getTemplateLength());
    assertNotEquals(0, testf1.getTemplateLength());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(0, testf2.getTemplateLength());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(294, testf3.getTemplateLength());
    assertNotEquals(0, testf3.getTemplateLength());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(0, testf4.getTemplateLength());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(781, testf5.getTemplateLength());
    assertNotEquals(0, testf5.getTemplateLength());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(0, testf6.getTemplateLength());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals(2307, testf7.getTemplateLength());
    assertNotEquals(0, testf7.getTemplateLength());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(0, testf8.getTemplateLength());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(594, testf9.getTemplateLength());
    assertNotEquals(0, testf9.getTemplateLength());
    testf9.close();
  }

  @Test
  public void getComplementeLength() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(0, testf1.getComplementeLength());
    assertNotEquals("1193000000", testf1.getComplementeLength());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(0, testf2.getComplementeLength());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(239, testf3.getComplementeLength());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(0, testf4.getComplementeLength());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(721, testf5.getComplementeLength());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(0, testf6.getComplementeLength());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals(2204, testf7.getComplementeLength());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(0, testf8.getComplementeLength());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(0, testf9.getComplementeLength());
    testf9.close();
  }

  @Test
  public void testGetNumBarcode() throws ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(null, testf1.getNumBarcode());
    assertNotEquals("BC05", testf1.getNumBarcode());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getNumBarcode());
    assertNotEquals("BC05", testf2.getNumBarcode());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(null, testf3.getNumBarcode());
    assertNotEquals("BC05", testf3.getNumBarcode());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getNumBarcode());
    assertNotEquals("BC05", testf4.getNumBarcode());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(null, testf5.getNumBarcode());
    assertNotEquals("BC05", testf5.getNumBarcode());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getNumBarcode());
    assertNotEquals("BC01", testf6.getNumBarcode());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals("BC01", testf7.getNumBarcode());
    assertNotEquals(null, testf7.getNumBarcode());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertNotEquals("BC02_var1", testf8.getNumBarcode());
    assertEquals(null, testf8.getNumBarcode());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals("BC02_var1", testf9.getNumBarcode());
    assertNotEquals(null, testf9.getNumBarcode());
    testf9.close();
  }

  //
  //
  // FASTQ getters
  //
  //

  @Test
  public void testGetTemplateFastq() throws IOException, ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    String sequence1 =
        readInputStream("/fast5/alexander_PC_20161027_R9-4_1D_template.fastq");
    assertEquals(sequence1, testf1.getTemplateFastq());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getTemplateFastq());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    String sequence3 =
        readInputStream("/fast5/dnacpc14_20160617_R7_2D_template.fastq");
    assertEquals(sequence3, testf3.getTemplateFastq());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getTemplateFastq());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    String sequence5 =
        readInputStream("/fast5/dnacpc14_20161011_R9_2D_template.fastq");
    assertEquals(sequence5, testf5.getTemplateFastq());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getTemplateFastq());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    String sequence7 =
        readInputStream("/fast5/dnacpc14_20170124_R9-4_2D_template.fastq");
    assertEquals(sequence7, testf7.getTemplateFastq());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getTemplateFastq());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    String sequence9 = readInputStream(
        "/fast5/dnacpc14_20170328_R9-4_1D_albacore_template.fastq");
    assertEquals(sequence9, testf9.getTemplateFastq());
    testf9.close();

  }

  @Test
  public void testGetComplementFastq() throws IOException, ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(null, testf1.getComplementFastq());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getComplementFastq());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    String sequence3 =
        readInputStream("/fast5/dnacpc14_20160617_R7_2D_complement.fastq");
    assertEquals(sequence3, testf3.getComplementFastq());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getComplementFastq());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    String sequence5 =
        readInputStream("/fast5/dnacpc14_20161011_R9_2D_complement.fastq");
    assertEquals(sequence5, testf5.getComplementFastq());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getComplementFastq());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    String sequence7 =
        readInputStream("/fast5/dnacpc14_20170124_R9-4_2D_complement.fastq");
    assertEquals(sequence7, testf7.getComplementFastq());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getComplementFastq());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(null, testf9.getComplementFastq());
    testf9.close();
  }

  @Test
  public void testGetConsensusFastq() throws IOException, ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(null, testf1.getConsensusFastq());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getConsensusFastq());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    String sequence3 =
        readInputStream("/fast5/dnacpc14_20160617_R7_2D_consensus.fastq");
    assertEquals(sequence3, testf3.getConsensusFastq());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getConsensusFastq());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    String sequence5 =
        readInputStream("/fast5/dnacpc14_20161011_R9_2D_consensus.fastq");
    assertEquals(sequence5, testf5.getConsensusFastq());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getConsensusFastq());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    String sequence7 =
        readInputStream("/fast5/dnacpc14_20170124_R9-4_2D_consensus.fastq");
    assertEquals(sequence7, testf7.getConsensusFastq());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getConsensusFastq());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(null, testf9.getConsensusFastq());
    testf9.close();
  }

  @Test
  public void testGetTranscriptFastq() throws IOException, ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(null, testf1.getTranscriptFastq());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getTranscriptFastq());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(null, testf3.getTranscriptFastq());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getTranscriptFastq());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(null, testf5.getTranscriptFastq());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getTranscriptFastq());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    String sequence7 =
        readInputStream("/fast5/dnacpc14_20170124_R9-4_2D_transcript.fastq");
    assertEquals(sequence7, testf7.getTranscriptFastq());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getTranscriptFastq());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(null, testf9.getTranscriptFastq());
    testf9.close();
  }

  @Test
  public void testGetBarcodindFinalStatus() throws IOException, ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(null, testf1.getBarcodindFinalStatus());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getBarcodindFinalStatus());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(null, testf3.getBarcodindFinalStatus());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getBarcodindFinalStatus());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals(null, testf5.getBarcodindFinalStatus());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getBarcodindFinalStatus());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals("Classifying design3 strand type.",
        testf7.getBarcodindFinalStatus());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getBarcodindFinalStatus());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(null, testf9.getBarcodindFinalStatus());
    testf9.close();
  }

  @Test
  public void testGetBaseCall1DFinalStatus()
      throws IOException, ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals("1D basecall failed quality filters.",
        testf1.getBaseCall1DFinalStatus());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getBaseCall1DFinalStatus());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals("Workflow completed sucessfully.",
        testf3.getBaseCall1DFinalStatus());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getBaseCall1DFinalStatus());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals("Workflow completed sucessfully.",
        testf5.getBaseCall1DFinalStatus());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getBaseCall1DFinalStatus());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals("Workflow completed sucessfully.",
        testf7.getBaseCall1DFinalStatus());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getBaseCall1DFinalStatus());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(null, testf9.getBaseCall1DFinalStatus());
    testf9.close();
  }

  @Test
  public void testGetBaseCall2DFinalStatus()
      throws IOException, ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(null, testf1.getBaseCall2DFinalStatus());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getBaseCall2DFinalStatus());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals("Workflow completed successfully.",
        testf3.getBaseCall2DFinalStatus());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getBaseCall2DFinalStatus());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals("Workflow completed successfully.",
        testf5.getBaseCall2DFinalStatus());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getBaseCall2DFinalStatus());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals("Workflow completed successfully.",
        testf7.getBaseCall2DFinalStatus());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getBaseCall2DFinalStatus());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(null, testf9.getBaseCall2DFinalStatus());
    testf9.close();
  }

  @Test
  public void testGetCalibrationStrandFinalStatus()
      throws IOException, ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals("Previous basecall was not successful -- returning failure",
        testf1.getCalibrationStrandFinalStatus());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getCalibrationStrandFinalStatus());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals("No calibration strand detected.",
        testf3.getCalibrationStrandFinalStatus());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getCalibrationStrandFinalStatus());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals("No calibration strand detected.",
        testf5.getCalibrationStrandFinalStatus());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getCalibrationStrandFinalStatus());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals("No calibration strand detected.",
        testf7.getCalibrationStrandFinalStatus());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getCalibrationStrandFinalStatus());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(null, testf9.getCalibrationStrandFinalStatus());
    testf9.close();
  }

  @Test
  public void testGetEventDetectionFinalStatus()
      throws IOException, ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals("Workflow completed sucessfully.",
        testf1.getEventDetectionFinalStatus());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getEventDetectionFinalStatus());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals(null, testf3.getEventDetectionFinalStatus());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getEventDetectionFinalStatus());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals("Workflow completed sucessfully.",
        testf5.getEventDetectionFinalStatus());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getEventDetectionFinalStatus());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals("Workflow completed sucessfully.",
        testf7.getEventDetectionFinalStatus());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getEventDetectionFinalStatus());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(null, testf9.getEventDetectionFinalStatus());
    testf9.close();
  }

  @Test
  public void testGetHairpinSplitFinalStatus()
      throws IOException, ParseException {
    Fast5 testf1 = new Fast5(getResourceAsFile(file1));
    assertEquals(null, testf1.getHairpinSplitFinalStatus());
    testf1.close();

    Fast5 testf2 = new Fast5(getResourceAsFile(file2));
    assertEquals(null, testf2.getHairpinSplitFinalStatus());
    testf2.close();

    Fast5 testf3 = new Fast5(getResourceAsFile(file3));
    assertEquals("Splitting hairpin by double_abasic.",
        testf3.getHairpinSplitFinalStatus());
    testf3.close();

    Fast5 testf4 = new Fast5(getResourceAsFile(file4));
    assertEquals(null, testf4.getHairpinSplitFinalStatus());
    testf4.close();

    Fast5 testf5 = new Fast5(getResourceAsFile(file5));
    assertEquals("Splitting hairpin by double_abasic.",
        testf5.getHairpinSplitFinalStatus());
    testf5.close();

    Fast5 testf6 = new Fast5(getResourceAsFile(file6));
    assertEquals(null, testf6.getHairpinSplitFinalStatus());
    testf6.close();

    Fast5 testf7 = new Fast5(getResourceAsFile(file7));
    assertEquals("Found triple-abasic hairpin.",
        testf7.getHairpinSplitFinalStatus());
    testf7.close();

    Fast5 testf8 = new Fast5(getResourceAsFile(file8));
    assertEquals(null, testf8.getHairpinSplitFinalStatus());
    testf8.close();

    Fast5 testf9 = new Fast5(getResourceAsFile(file9));
    assertEquals(null, testf9.getHairpinSplitFinalStatus());
    testf9.close();
  }

}
