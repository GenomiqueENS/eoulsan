package fr.ens.biologie.genomique.eoulsan.bio;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

public class BEDEntryTest {

  @Test
  public void testGetChromosomeName() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();
    e.parse("chr1\t11873\t14409\tuc001aaa.3\t0\t+", 6);

    assertEquals("chr1", e.getChromosomeName());
  }

  @Test
  public void testGetStart() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();
    e.parse("chr1\t11873\t14409\tuc001aaa.3\t0\t+", 6);

    assertEquals(11874, e.getStart());
  }

  @Test
  public void testGetEnd() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();
    e.parse("chr1\t11873\t14409\tuc001aaa.3\t0\t+", 6);

    assertEquals(14408, e.getEnd());
  }

  @Test
  public void testGetLength() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();
    e.parse("chr1\t1000\t2001\tuc001aaa.3\t0\t+", 6);

    assertEquals(1000, e.getLength());
  }

  @Test
  public void testGetName() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();
    e.parse("chr1\t11873\t14409\tuc001aaa.3\t0\t+", 6);

    assertEquals("uc001aaa.3", e.getName());
  }

  @Test
  public void testGetScore() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();
    e.parse("chr1\t11873\t14409\tuc001aaa.3\t500\t+", 6);

    assertEquals("500", e.getScore());
  }

  @Test
  public void testGetStrand() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();
    e.parse("chr1\t11873\t14409\tuc001aaa.3\t500\t+", 6);

    assertEquals('+', e.getStrand());
  }

  @Test
  public void testGetThickStart() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();

    String s =
        "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11874\t0\t3\t354,109,1189,\t0,739,1347,";

    e.parse(s, 12);
    assertEquals(11874, e.getThickStart());
  }

  @Test
  public void testGetThickEnd() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();

    String s =
        "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t21873\t0\t3\t354,109,1189,\t0,739,1347,";

    e.parse(s, 12);
    assertEquals(21872, e.getThickEnd());
  }

  @Test
  public void testGetThickLength() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();

    String s =
        "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t1000\t2001\t0\t3\t354,109,1189,\t0,739,1347,";

    e.parse(s, 12);
    assertEquals(1000, e.getThickLength());

    s = "chr1\t11873\t14409";
    e.parse(s);
    assertEquals(0, e.getThickLength());
  }

  @Test
  public void testGetBlockCount() {

    BEDEntry e = new BEDEntry();
    e.setChromosomeName("chr1");
    assertEquals(0, e.getBlockCount());

    e.addBlock(100, 250);
    assertEquals(1, e.getBlockCount());

    e.addBlock(300, 500);
    assertEquals(2, e.getBlockCount());
  }

  @Test
  public void testGetBlockSizes() {

    BEDEntry e = new BEDEntry();
    e.setChromosomeName("chr1");
    e.addBlock(100, 250);
    e.addBlock(300, 500);

    assertEquals(Arrays.asList(151, 201), e.getBlockSizes());
  }

  @Test
  public void testGetBlockStarts() {

    BEDEntry e = new BEDEntry();
    e.setChromosomeName("chr1");
    e.addBlock(100, 250);
    e.addBlock(300, 500);

    assertEquals(Arrays.asList(100, 300), e.getBlockStarts());
  }

  @Test
  public void testGetBlockEnds() {

    BEDEntry e = new BEDEntry();
    e.setChromosomeName("chr1");
    e.addBlock(100, 250);
    e.addBlock(300, 500);

    assertEquals(Arrays.asList(250, 500), e.getBlockEnds());
  }

  @Test
  public void testSetChromosomeName() {

    BEDEntry e = new BEDEntry();
    assertNull(e.getChromosomeName());

    e.setChromosomeName("chr1");

    assertEquals("chr1", e.getChromosomeName());
  }

  @Test
  public void testSetStart() {

    BEDEntry e = new BEDEntry();
    assertEquals(0, e.getStart());

    e.setStart(5555);

    assertEquals(5555, e.getStart());
  }

  @Test
  public void testSetEnd() {

    BEDEntry e = new BEDEntry();
    assertEquals(0, e.getEnd());

    e.setEnd(6666);

    assertEquals(6666, e.getEnd());
  }

  @Test
  public void testSetScoreString() {

    BEDEntry e = new BEDEntry();
    assertNull(e.getScore());

    e.setScore("up");

    assertEquals("up", e.getScore());
  }

  @Test
  public void testSetScoreInt() {

    BEDEntry e = new BEDEntry();
    assertNull(e.getScore());

    e.setScore(555);

    assertEquals("555", e.getScore());

    try {
      e.setScore(-1);
      assertTrue(false);
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }

    try {
      e.setScore(1001);
      assertTrue(false);
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }

  }

  @Test
  public void testSetScoreDouble() {

    BEDEntry e = new BEDEntry();
    assertNull(e.getScore());

    e.setScore(555.5);

    assertEquals("555.5", e.getScore());
  }

  @Test
  public void testSetStrand() {

    BEDEntry e = new BEDEntry();
    assertEquals(0, e.getStrand());

    e.setStrand('+');

    assertEquals('+', e.getStrand());
  }

  @Test
  public void testSetThickStart() {

    BEDEntry e = new BEDEntry();
    assertEquals(0, e.getThickStart());

    e.setThickStart(6666);

    assertEquals(6666, e.getThickStart());
  }

  @Test
  public void testSetThickEnd() {

    BEDEntry e = new BEDEntry();
    assertEquals(0, e.getThickEnd());

    e.setThickEnd(7777);

    assertEquals(7777, e.getThickEnd());
  }

  @Test
  public void testSetRgbItemInt() {

    BEDEntry e = new BEDEntry();
    assertEquals("0", e.getRgbItem());

    e.setRgbItem("15,120,200");

    assertEquals("15,120,200", e.getRgbItem());
  }

  @Test
  public void testSetRgbItemIntIntInt() {
    BEDEntry e = new BEDEntry();
    assertEquals("0", e.getRgbItem());

    e.setRgbItem(15, 120, 200);

    assertEquals("15,120,200", e.getRgbItem());
  }

  @Test
  public void testAddBlock() {

    BEDEntry e = new BEDEntry();
    e.setChromosomeName("chr1");
    assertEquals(0, e.getBlockCount());

    e.addBlock(100, 250);
    assertEquals(1, e.getBlockCount());

    e.addBlock(300, 500);
    assertEquals(2, e.getBlockCount());
  }

  @Test
  public void testRemoveBlock() {

    BEDEntry e = new BEDEntry();
    e.setChromosomeName("chr1");
    assertEquals(0, e.getBlockCount());

    e.addBlock(100, 250);
    e.addBlock(300, 500);
    e.addBlock(700, 1000);

    assertEquals(3, e.getBlockCount());
    e.removeBlock(300, 500);
    assertEquals(2, e.getBlockCount());
    e.removeBlock(100, 250);
    assertEquals(1, e.getBlockCount());
    e.removeBlock(700, 1000);
  }

  @Test
  public void testToBED3() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();

    String s = "chr1\t11873\t14409";

    e.parse(s, 3);

    assertEquals(s, e.toBED3());
  }

  @Test
  public void testToBED4() throws BadBioEntryException {
    BEDEntry e = new BEDEntry();

    String s = "chr1\t11873\t14409\tuc001aaa.3";

    e.parse(s, 4);

    assertEquals(s, e.toBED4());
  }

  @Test
  public void testToBED5() throws BadBioEntryException {
    BEDEntry e = new BEDEntry();

    String s = "chr1\t11873\t14409\tuc001aaa.3\t0";

    e.parse(s, 5);

    assertEquals(s, e.toBED5());
  }

  @Test
  public void testToBED6() throws BadBioEntryException {
    BEDEntry e = new BEDEntry();

    String s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+";

    e.parse(s, 6);

    assertEquals(s, e.toBED6());

  }

  @Test
  public void testToBED12() throws BadBioEntryException {
    BEDEntry e = new BEDEntry();

    String s =
        "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0\t3\t354,109,1189,\t0,739,1347,";
    e.parse(s, 12);
    assertEquals(s, e.toBED12());

    s = "chr2\t21873\t24409\tuc002aaa.3\t0,155,200\t-\t21873\t21873\t0\t3\t1354,1109,11189,\t0,739,1347,";
    e.parse(s, 12);
    assertEquals(s, e.toBED12());
  }

  @Test
  public void testParse() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();

    String s = "chr1\t11873\t14409";
    e.parse(s);
    assertEquals(s, e.toBED3());

    s = "chr1\t11873\t14409\tuc001aaa.3";
    e.parse(s);
    assertEquals(s, e.toBED4());

    s = "chr1\t11873\t14409\tuc001aaa.3\t0";
    e.parse(s);
    assertEquals(s, e.toBED5());

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+";
    e.parse(s);
    assertEquals(s, e.toBED6());

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0\t3\t354,109,1189,\t0,739,1347,";
    e.parse(s);
    assertEquals(s, e.toBED12());

    s = "chr1";
    try {
      e.parse(s);
      assertTrue(false);
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }

    s = "chr1\t11873";
    try {
      e.parse(s);
      assertTrue(false);
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0\t3\t354,109,1189";
    try {
      e.parse(s);
      assertTrue(false);
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }

  }

  @Test
  public void testIsMetaDataEntry() {

    BEDEntry e = new BEDEntry();
    try {
      e.parse(
          "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0\t3\t354,109,1189,\t0,739,1347,");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    assertFalse(e.isMetaDataEntry("key0"));
    assertEquals(0, e.getMetadataKeyNames().size());

    e.addMetaDataEntry("key1", "val1");
    assertFalse(e.isMetaDataEntry("key0"));
    assertTrue(e.isMetaDataEntry("key1"));

    e.addMetaDataEntry("key2", "val2");
    assertFalse(e.isMetaDataEntry("key0"));
    assertTrue(e.isMetaDataEntry("key1"));
    assertTrue(e.isMetaDataEntry("key2"));
  }

  @Test
  public void testGetMetadataEntryValues() {

    BEDEntry e = new BEDEntry();
    try {
      e.parse(
          "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0\t3\t354,109,1189,\t0,739,1347,");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    e.addMetaDataEntry("key1", "val1");
    assertEquals(1, e.getMetadataEntryValues("key1").size());
    assertEquals("val1", e.getMetadataEntryValues("key1").get(0));

    e.addMetaDataEntry("key2", "val2");
    assertEquals(1, e.getMetadataEntryValues("key1").size());
    assertEquals("val1", e.getMetadataEntryValues("key1").get(0));
    assertEquals(1, e.getMetadataEntryValues("key2").size());
    assertEquals("val2", e.getMetadataEntryValues("key2").get(0));

    e.clear();
    e.clearMetaData();

    e.addMetaDataEntry("browser", "position chr7:127471196-127495720");
    e.addMetaDataEntry("browser", "hide all");
    e.addMetaDataEntry("track",
        "name=\"ItemRGBDemo\" description=\"Item RGB demonstration\" visibility=2 itemRgb=\"On\"");

    assertEquals(2, e.getMetadataKeyNames().size());
    assertEquals(new HashSet<>(Arrays.asList("browser", "track")),
        e.getMetadataKeyNames());
    assertEquals(Arrays.asList("position chr7:127471196-127495720", "hide all"),
        e.getMetadataEntryValues("browser"));
    assertEquals(Collections.singletonList(
      "name=\"ItemRGBDemo\" description=\"Item RGB demonstration\" visibility=2 itemRgb=\"On\""),
        e.getMetadataEntryValues("track"));
  }

  @Test
  public void testGetMetadataKeyNames() {

    BEDEntry e = new BEDEntry();
    try {
      e.parse(
          "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0\t3\t354,109,1189,\t0,739,1347,");
    } catch (BadBioEntryException exp) {
      assertTrue(false);
    }

    assertEquals(0, e.getMetadataKeyNames().size());

    e.addMetaDataEntry("key1", "val1");
    assertEquals(1, e.getMetadataKeyNames().size());

    assertFalse(e.getMetadataKeyNames().contains("key0"));
    assertTrue(e.getMetadataKeyNames().contains("key1"));

    e.addMetaDataEntry("key2", "val2");
    assertEquals(2, e.getMetadataKeyNames().size());

    assertFalse(e.getMetadataKeyNames().contains("key0"));
    assertTrue(e.getMetadataKeyNames().contains("key1"));
    assertTrue(e.getMetadataKeyNames().contains("key2"));

  }

}