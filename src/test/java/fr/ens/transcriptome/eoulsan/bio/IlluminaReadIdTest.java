/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.bio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.transcriptome.eoulsan.EoulsanException;

public class IlluminaReadIdTest {

  @Test
  public void testGetInstrumentId() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertEquals("HWUSI-EAS100R", ii.getInstrumentId());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertEquals("HWUSI-EAS100R", ii.getInstrumentId());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertEquals("HWUSI-EAS100R", ii.getInstrumentId());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertEquals("SOLEXA3_162", ii.getInstrumentId());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertEquals("SOLEXA3_162", ii.getInstrumentId());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertEquals("HWI-1KL110", ii.getInstrumentId());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertEquals("HWI-1KL110", ii.getInstrumentId());

    ii = new IlluminaReadId("HWI-1KL110:25:B0866ABXX:1:1101:1167:2098 1:N:0:");
    assertEquals("HWI-1KL110", ii.getInstrumentId());

  }

  @Test
  public void testGetRunId() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertEquals(-1, ii.getRunId());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertEquals(-1, ii.getRunId());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertEquals(-1, ii.getRunId());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertEquals(-1, ii.getRunId());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertEquals(-1, ii.getRunId());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertEquals(24, ii.getRunId());
  }

  @Test
  public void testGetFlowCellId() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertEquals(null, ii.getFlowCellId());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertEquals(null, ii.getFlowCellId());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertEquals(null, ii.getFlowCellId());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertEquals(null, ii.getFlowCellId());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertEquals(null, ii.getFlowCellId());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertEquals("AB0868ABXX", ii.getFlowCellId());
  }

  @Test
  public void testGetFlowCellLane() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertEquals(6, ii.getFlowCellLane());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertEquals(6, ii.getFlowCellLane());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertEquals(6, ii.getFlowCellLane());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertEquals(7, ii.getFlowCellLane());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertEquals(7, ii.getFlowCellLane());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertEquals(3, ii.getFlowCellLane());
  }

  @Test
  public void testGetTileNumberInFlowCellLane() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertEquals(73, ii.getTileNumberInFlowCellLane());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertEquals(73, ii.getTileNumberInFlowCellLane());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertEquals(73, ii.getTileNumberInFlowCellLane());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertEquals(100, ii.getTileNumberInFlowCellLane());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertEquals(100, ii.getTileNumberInFlowCellLane());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertEquals(1101, ii.getTileNumberInFlowCellLane());
  }

  @Test
  public void testGetxClusterCoordinateInTile() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertEquals(941, ii.getXClusterCoordinateInTile());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertEquals(941, ii.getXClusterCoordinateInTile());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertEquals(941, ii.getXClusterCoordinateInTile());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertEquals(10000, ii.getXClusterCoordinateInTile());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertEquals(10000, ii.getXClusterCoordinateInTile());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertEquals(1492, ii.getXClusterCoordinateInTile());
  }

  @Test
  public void testGetyClusterCoordinateInTile() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertEquals(1973, ii.getYClusterCoordinateInTile());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertEquals(1973, ii.getYClusterCoordinateInTile());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertEquals(1973, ii.getYClusterCoordinateInTile());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertEquals(1220, ii.getYClusterCoordinateInTile());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertEquals(1220, ii.getYClusterCoordinateInTile());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertEquals(2178, ii.getYClusterCoordinateInTile());
  }

  @Test
  public void testGetMultiplexedSample() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertEquals("0", ii.getSequenceIndex());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#ATCACG/1");
    assertEquals("ATCACG", ii.getSequenceIndex());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertEquals("0", ii.getSequenceIndex());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertEquals("0", ii.getSequenceIndex());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertEquals("0", ii.getSequenceIndex());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertEquals("0", ii.getSequenceIndex());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertEquals("ATCACG", ii.getSequenceIndex());
  }

  @Test
  public void testGetPairMember() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertEquals(1, ii.getPairMember());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertEquals(1, ii.getPairMember());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertEquals(-1, ii.getPairMember());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertEquals(1, ii.getPairMember());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertEquals(-1, ii.getPairMember());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertEquals(1, ii.getPairMember());
  }

  @Test
  public void testisFiltered() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertFalse(ii.isFiltered());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertFalse(ii.isFiltered());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertFalse(ii.isFiltered());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertFalse(ii.isFiltered());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertFalse(ii.isFiltered());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertFalse(ii.isFiltered());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:Y:0:ATCACG");
    assertTrue(ii.isFiltered());
  }

  @Test
  public void testGetControlNumber() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertEquals(-1, ii.getControlNumber());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertEquals(-1, ii.getControlNumber());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertEquals(-1, ii.getControlNumber());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertEquals(-1, ii.getControlNumber());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertEquals(-1, ii.getControlNumber());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertEquals(0, ii.getControlNumber());
  }

  @Test
  public void testParse() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertEquals("HWUSI-EAS100R", ii.getInstrumentId());

    ii.parse("HWUSI-EAS100S:6:73:941:1973#0/1");
    assertEquals("HWUSI-EAS100S", ii.getInstrumentId());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertEquals("HWI-1KL110", ii.getInstrumentId());
    ii.parse("HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertEquals("HWI-1KL110", ii.getInstrumentId());

    try {
      ii.parse((String) null);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    try {
      ii.parse((Sequence) null);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    Sequence s = new Sequence();

    try {
      ii.parse(s);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    try {
      new IlluminaReadId((String) null);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    try {
      new IlluminaReadId((Sequence) null);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    try {
      new IlluminaReadId(s);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    s.setName("HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    ii = new IlluminaReadId(s);
    assertEquals("HWI-1KL110", ii.getInstrumentId());
    s.setName("HWI-1K1111:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    ii.parse(s);
    assertEquals("HWI-1K1111", ii.getInstrumentId());
  }

  @Test
  public void testIlluminaReadId() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertEquals("HWUSI-EAS100R", ii.getInstrumentId());

    ii.parse(" HWUSI-EAS100R:6:73:941:1973#0/1 ");
    assertEquals("HWUSI-EAS100R", ii.getInstrumentId());

    try {
      ii.parse("HWUSI-EAS100R:6:73:941:1973:0/1");
      assertTrue(false);
    } catch (EoulsanException e) {
      assertTrue(true);
    }

    try {
      ii.parse("HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0#ATCACG");
      assertTrue(false);
    } catch (EoulsanException e) {
      assertTrue(true);
    }

    try {
      ii.parse("HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178:1:N:0:ATCACG");
      assertTrue(false);
    } catch (EoulsanException e) {
      assertTrue(true);
    }

  }

}
