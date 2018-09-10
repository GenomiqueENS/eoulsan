package fr.ens.biologie.genomique.eoulsan.bio;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;

@SuppressWarnings("deprecation")
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

    assertEquals(14409, e.getEnd());
  }

  @Test
  public void testGetLength() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();
    e.parse("chr1\t1000\t2001\tuc001aaa.3\t0\t+", 6);

    assertEquals(1001, e.getLength());
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
    assertEquals(21873, e.getThickEnd());
  }

  @Test
  public void testGetThickLength() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();

    assertEquals(0, e.getThickLength());
    e.setThickStart(100);
    e.setThickEnd(200);
    assertEquals(101, e.getThickLength());

    e.clear();
    e.setThickEnd(200);
    assertEquals(0, e.getThickLength());
    e.setThickStart(100);
    assertEquals(101, e.getThickLength());

    e.clear();
    e.setThickStart(100);
    assertEquals(0, e.getThickLength());
    e.setThickEnd(200);
    assertEquals(101, e.getThickLength());

    String s =
        "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t1000\t2001\t0\t3\t354,109,1189,\t0,739,1347,";

    e.parse(s, 12);
    assertEquals(1001, e.getThickLength());

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
    assertEquals("", e.getChromosomeName());

    try {
      e.setChromosomeName(null);
      fail();
    } catch (NullPointerException exp) {
      assertTrue(true);
    }

    e.setChromosomeName("chr1");

    assertEquals("chr1", e.getChromosomeName());
  }

  @Test
  public void testSetStart() {

    BEDEntry e = new BEDEntry();
    assertEquals(-1, e.getStart());

    e.setStart(5555);
    assertEquals(5555, e.getStart());

    try {
      e.setStart(-1);
      fail();
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }
    assertEquals(5555, e.getStart());

  }

  @Test
  public void testSetEnd() {

    BEDEntry e = new BEDEntry();
    assertEquals(-1, e.getEnd());

    e.setEnd(6666);

    assertEquals(6666, e.getEnd());

    try {
      e.setEnd(-1);
      fail();
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }
    assertEquals(6666, e.getEnd());
  }

  @Test
  public void testSetScoreString() {

    BEDEntry e = new BEDEntry();
    assertEquals("", e.getScore());

    e.setScore("up");

    assertEquals("up", e.getScore());
  }

  @Test
  public void testSetScoreInt() {

    BEDEntry e = new BEDEntry();
    assertEquals("", e.getScore());

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
    assertEquals("", e.getScore());

    e.setScore(555.5);

    assertEquals("555.5", e.getScore());
  }

  @Test
  public void testSetStrand() {

    BEDEntry e = new BEDEntry();
    assertEquals(0, e.getStrand());

    e.setStrand('+');

    assertEquals('+', e.getStrand());

    try {
      e.setStrand('.');
      fail();
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }

  }

  @Test
  public void testSetThickStart() {

    BEDEntry e = new BEDEntry();
    assertEquals(-1, e.getThickStart());

    try {
      e.setThickStart(0);
      fail();
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }

    e.setThickStart(6666);

    assertEquals(6666, e.getThickStart());
  }

  @Test
  public void testSetThickEnd() {

    BEDEntry e = new BEDEntry();
    assertEquals(-1, e.getThickEnd());

    try {
      e.setThickEnd(0);
      fail();
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }

    e.setThickEnd(7777);

    assertEquals(7777, e.getThickEnd());
  }

  @Test
  public void testSetRgbItemInt() {

    BEDEntry e = new BEDEntry();
    assertEquals("0", e.getRgbItem());

    e.setRgbItem("15,120,200");

    assertEquals("15,120,200", e.getRgbItem());

    e.setRgbItem(1, 2, 3);
    assertEquals("1,2,3", e.getRgbItem());
    e.setRgbItem(0, 0, 0);
    assertEquals("0,0,0", e.getRgbItem());
    e.setRgbItem(255, 255, 255);
    assertEquals("255,255,255", e.getRgbItem());

    e.clear();

    try {
      e.setRgbItem(-1, 2, 3);
      fail();
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }
    assertEquals("0", e.getRgbItem());

    try {
      e.setRgbItem(1, -1, 3);
      fail();
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }
    assertEquals("0", e.getRgbItem());

    try {
      e.setRgbItem(1, 2, -1);
      fail();
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }
    assertEquals("0", e.getRgbItem());

    try {
      e.setRgbItem(256, 2, 3);
      fail();
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }
    assertEquals("0", e.getRgbItem());

    try {
      e.setRgbItem(1, 256, 3);
      fail();
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }
    assertEquals("0", e.getRgbItem());

    try {
      e.setRgbItem(1, 2, 256);
      fail();
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }
    assertEquals("0", e.getRgbItem());

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
    assertFalse(e.removeBlock(3000, 5000));
    assertEquals(3, e.getBlockCount());
    assertTrue(e.removeBlock(300, 500));
    assertEquals(2, e.getBlockCount());
    assertTrue(e.removeBlock(100, 250));
    assertEquals(1, e.getBlockCount());
    assertTrue(e.removeBlock(700, 1000));
    assertEquals(0, e.getBlockCount());

    e.setStrand('+');
    e.addBlock(100, 250);
    e.addBlock(300, 500);
    e.addBlock(700, 1000);

    assertEquals(3, e.getBlockCount());
    assertFalse(e.removeBlock(3000, 5000));
    assertEquals(3, e.getBlockCount());
    assertTrue(e.removeBlock(300, 500));
    assertEquals(2, e.getBlockCount());
    assertTrue(e.removeBlock(100, 250));
    assertEquals(1, e.getBlockCount());
    assertTrue(e.removeBlock(700, 1000));
    assertEquals(0, e.getBlockCount());
  }

  @Test
  public void testToBED3() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();
    assertEquals("\t0\t0", e.toBED(3));

    String s = "chr1\t11873\t14409";

    e.parse(s, 3);

    assertEquals(s, e.toBED(3));
  }

  @Test
  public void testToBED4() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();
    assertEquals("\t0\t0\t", e.toBED(4));

    String s = "chr1\t11873\t14409\tuc001aaa.3";

    e.parse(s, 4);

    assertEquals(s, e.toBED(4));
  }

  @Test
  public void testToBED5() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();
    assertEquals("\t0\t0\t\t", e.toBED(5));

    String s = "chr1\t11873\t14409\tuc001aaa.3\t0";

    e.parse(s, 5);

    assertEquals(s, e.toBED(5));
  }

  @Test
  public void testToBED6() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();
    assertEquals("\t0\t0\t\t\t", e.toBED(6));

    String s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+";

    e.parse(s, 6);

    assertEquals(s, e.toBED(6));
  }

  @Test
  public void testToBED12() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();
    assertEquals("\t0\t0\t\t\t\t0\t0\t0\t0\t\t", e.toBED(12));

    String s =
        "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0\t3\t354,109,1189,\t0,739,1347,";
    e.parse(s, 12);
    assertEquals(s, e.toBED(12));

    s = "chr2\t21873\t24409\tuc002aaa.3\t0\t-\t21873\t21873\t0,155,200\t3\t1354,1109,11189,\t0,739,1347,";
    e.parse(s, 12);
    assertEquals(s, e.toBED(12));
  }

  @Test
  public void testParse() throws BadBioEntryException {

    BEDEntry e = new BEDEntry();

    try {
      e.parse(null);
      fail();
    } catch (NullPointerException exp) {
      assertTrue(true);
    }

    try {
      e.parse(null, 12);
      fail();
    } catch (NullPointerException exp) {
      assertTrue(true);
    }

    String s = "chr1\t11873\t14409";
    e.parse(s);
    assertEquals(s, e.toBED(3));

    s = "chr1\t11873\t14409\tuc001aaa.3";
    e.parse(s);
    assertEquals(s, e.toBED(4));

    s = "chr1\t11873\t14409\tuc001aaa.3\t0";
    e.parse(s);
    assertEquals(s, e.toBED(5));

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+";
    e.parse(s);
    assertEquals(s, e.toBED(6));

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873";
    e.parse(s);
    assertEquals(s, e.toBED(8));

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0";
    e.parse(s);
    assertEquals(s, e.toBED(9));

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0\t3\t354,109,1189,\t0,739,1347,";
    e.parse(s);
    assertEquals(s, e.toBED(12));
    assertEquals(s, e.toBED());
    assertEquals(s, e.toString());

    s = "chr1";
    try {
      e.parse(s);
      fail();
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }

    s = "chr1\t11873";
    try {
      e.parse(s);
      fail();
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11D873\t0\t3\t354,109,1189,\t0,739,1347,";
    e.parse(s);

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0\t3\t354,109,1189";
    try {
      e.parse(s);
      fail();
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0\t3\t354,109,1189";
    try {
      e.parse(s, 13);
      fail();
    } catch (IllegalArgumentException exp) {
      assertTrue(true);
    }

    s = " \t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0\t3\t354,109,1189";
    try {
      e.parse(s, 12);
      fail();
    } catch (BadBioEntryException exp) {
      assertTrue(true);
    }

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0\t3\t354,109,1189,\t0,739,1347,";
    e.parse(s);
    assertEquals('+', e.getStrand());

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t-\t11873\t11873\t0\t3\t354,109,1189,\t0,739,1347,";
    e.parse(s);
    assertEquals('-', e.getStrand());

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t.\t11873\t11873\t0\t3\t354,109,1189,\t0,739,1347,";
    e.parse(s);
    assertEquals(0, e.getStrand());

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0\t\t354,109,1189";
    try {
      e.parse(s, 12);
      fail();
    } catch (BadBioEntryException exp) {
      assertTrue(true);
    }

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0\t3\t354,109,\t0,739,1347,";
    try {
      e.parse(s, 12);
      fail();
    } catch (BadBioEntryException exp) {
      assertTrue(true);
    }

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0\t3\t354,109,1189,\t0,739,";
    try {
      e.parse(s, 12);
      fail();
    } catch (BadBioEntryException exp) {
      assertTrue(true);
    }

    s = "chr1\t11873\t14409\tuc001aaa.3\t0\t+\t11873\t11873\t0\t3\t354,109,1189,\t0,73D9,1347,";
    try {
      e.parse(s, 12);
      fail();
    } catch (BadBioEntryException exp) {
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

    assertFalse(e.isMetaDataEntry(null));

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

    assertEquals(Collections.emptyList(), e.getMetadataEntryValues(null));
    assertEquals(Collections.emptyList(), e.getMetadataEntryValues("toto"));

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
    assertEquals(
        Arrays.asList(
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

  @Test
  public void testGetBlocks() {

    BEDEntry e = new BEDEntry();
    e.setChromosomeName("chr1");
    assertEquals(Collections.emptyList(), e.getBlocks());

    e.addBlock(100, 250);
    assertEquals(asList(new GenomicInterval("chr1", 100, 250, '.')),
        e.getBlocks());

    e.addBlock(300, 500);
    assertEquals(2, e.getBlockCount());
    assertEquals(asList(new GenomicInterval("chr1", 100, 250, '.'),
        new GenomicInterval("chr1", 300, 500, '.')), e.getBlocks());

  }

  @Test
  public void testAddMetaDataEntry() {

    BEDEntry e = new BEDEntry();
    assertEquals(emptySet(), e.getMetadataKeyNames());

    assertFalse(e.addMetaDataEntry(null, null));
    assertEquals(emptySet(), e.getMetadataKeyNames());
    assertFalse(e.addMetaDataEntry("key", null));
    assertEquals(emptySet(), e.getMetadataKeyNames());
    assertFalse(e.addMetaDataEntry(null, "value"));
    assertEquals(emptySet(), e.getMetadataKeyNames());
    assertTrue(e.addMetaDataEntry("key1", "value1"));
    assertEquals(Collections.singleton("key1"), e.getMetadataKeyNames());

  }

  @Test
  public void testAddMetaDataEntries() {

    BEDEntry e = new BEDEntry();

    assertFalse(e.addMetaDataEntries(null));
    Map<String, List<String>> entries = new HashMap<>();
    assertTrue(e.addMetaDataEntries(entries));
    entries.put("key0", null);
    assertFalse(e.addMetaDataEntries(entries));
    entries.clear();
    List<String> l = new ArrayList<String>();
    l.add(null);
    entries.put("key00", l);
    assertFalse(e.addMetaDataEntries(entries));
    entries.clear();
    entries.put("key1", Arrays.asList("val1"));
    assertTrue(e.addMetaDataEntries(entries));
    entries.clear();
    entries.put("key2", Arrays.asList("val2", "val3"));
    assertTrue(e.addMetaDataEntries(entries));
  }

  @Test
  public void testRemoveMetaDataEntry() {

    BEDEntry e = new BEDEntry();

    assertEquals(Collections.emptySet(), e.getMetadataKeyNames());
    e.addMetaDataEntry("key1", "value1");
    assertEquals(Collections.singleton("key1"), e.getMetadataKeyNames());
    e.addMetaDataEntry("key2", "value2");
    assertEquals(new HashSet<String>(asList("key1", "key2")),
        e.getMetadataKeyNames());
    assertFalse(e.removeMetaDataEntry("key3"));
    assertEquals(new HashSet<String>(asList("key1", "key2")),
        e.getMetadataKeyNames());
    assertFalse(e.removeMetaDataEntry(null));
    assertEquals(new HashSet<String>(asList("key1", "key2")),
        e.getMetadataKeyNames());
    assertTrue(e.removeMetaDataEntry("key1"));
    assertEquals(Collections.singleton("key2"), e.getMetadataKeyNames());
  }

  @Test
  public void testGetMetadata() {

    BEDEntry e1 = new BEDEntry();
    assertEquals(Collections.emptyMap(), e1.getMetadata().entries());

    e1.addMetaDataEntry("key1", "value1");
    assertEquals(
        Collections.singletonMap("key1", Collections.singletonList("value1")),
        e1.getMetadata().entries());

    e1.clearMetaData();
    assertEquals(Collections.emptyMap(), e1.getMetadata().entries());
    e1.getMetadata().add("key2", "value2");
    assertEquals(
        Collections.singletonMap("key2", Collections.singletonList("value2")),
        e1.getMetadata().entries());

    EntryMetadata m = new EntryMetadata();
    m.add("key3", "value3");

    BEDEntry e2 = new BEDEntry(m);

    assertEquals(
        Collections.singletonMap("key3", Collections.singletonList("value3")),
        e2.getMetadata().entries());
  }

  @Test
  public void testSetName() {

    BEDEntry e = new BEDEntry();

    assertEquals("", e.getName());

    try {
      e.setName(null);
      fail();
    } catch (NullPointerException exp) {
      assertTrue(true);
    }

    e.setName(" toto   ");
    assertEquals("toto", e.getName());
  }

  @Test
  public void testEqualsObject() {

    BEDEntry e1 = new BEDEntry();
    BEDEntry e2 = new BEDEntry();

    assertEquals(e1, e1);
    assertTrue(e1.equals(e1));

    assertFalse(e1.equals(null));
    assertFalse(e1.equals("toto"));
    assertEquals(e1, e2);
    assertTrue(e1.equals(e2));

    e1.setChromosomeName("value");
    assertNotEquals(e1, e2);
    assertFalse(e1.equals(e2));

    e2.setChromosomeName("value");
    assertEquals(e1, e2);
    assertTrue(e1.equals(e2));
  }

  @Test
  public void testHashCode() {

    BEDEntry e1 = new BEDEntry();
    BEDEntry e2 = new BEDEntry();

    assertEquals(e1.hashCode(), e2.hashCode());

    e1.setChromosomeName("value");
    assertNotEquals(e1.hashCode(), e2.hashCode());
  }

}