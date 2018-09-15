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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.bio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;

public class IlluminaReadIdTest {

  @Test
  public void testGetInstrumentId() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertTrue(ii.isInstrumentIdField());
    assertEquals("HWUSI-EAS100R", ii.getInstrumentId());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertTrue(ii.isInstrumentIdField());
    assertEquals("HWUSI-EAS100R", ii.getInstrumentId());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertTrue(ii.isInstrumentIdField());
    assertEquals("HWUSI-EAS100R", ii.getInstrumentId());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertTrue(ii.isInstrumentIdField());
    assertEquals("SOLEXA3_162", ii.getInstrumentId());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertTrue(ii.isInstrumentIdField());
    assertEquals("SOLEXA3_162", ii.getInstrumentId());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertTrue(ii.isInstrumentIdField());
    assertEquals("HWI-1KL110", ii.getInstrumentId());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertTrue(ii.isInstrumentIdField());
    assertEquals("HWI-1KL110", ii.getInstrumentId());

    ii = new IlluminaReadId(
        "NB500892:67:HVN5KBGXX:1:11101:22912:1064 1:N:0:CTCTCTAC+TACTCCTT");
    assertTrue(ii.isInstrumentIdField());
    assertEquals("NB500892", ii.getInstrumentId());

    ii = new IlluminaReadId("HWI-1KL110:25:B0866ABXX:1:1101:1167:2098 1:N:0:");
    assertTrue(ii.isInstrumentIdField());
    assertEquals("HWI-1KL110", ii.getInstrumentId());

    ii = new IlluminaReadId("NB500892:10:H3YL2AFXX:1:11101:1108:1044 1:Y:0:1");
    assertTrue(ii.isInstrumentIdField());
    assertEquals("NB500892", ii.getInstrumentId());

    ii = new IlluminaReadId(
        "SRR1577083.1 HWI-ST1160:266:D0H3RACXX:6:1315:4634:59858 length=50");
    assertTrue(ii.isInstrumentIdField());
    assertEquals("HWI-ST1160", ii.getInstrumentId());

  }

  @Test
  public void testGetRunId() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertFalse(ii.isRunIdField());
    assertEquals(-1, ii.getRunId());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertFalse(ii.isRunIdField());
    assertEquals(-1, ii.getRunId());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertFalse(ii.isRunIdField());
    assertEquals(-1, ii.getRunId());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertFalse(ii.isRunIdField());
    assertEquals(-1, ii.getRunId());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertFalse(ii.isRunIdField());
    assertEquals(-1, ii.getRunId());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertTrue(ii.isRunIdField());
    assertEquals(24, ii.getRunId());

    ii = new IlluminaReadId(
        "NB500892:67:HVN5KBGXX:1:11101:22912:1064 1:N:0:CTCTCTAC+TACTCCTT");
    assertTrue(ii.isRunIdField());
    assertEquals(67, ii.getRunId());

    ii = new IlluminaReadId("NB500892:10:H3YL2AFXX:1:11101:1108:1044 1:Y:0:1");
    assertTrue(ii.isRunIdField());
    assertEquals(10, ii.getRunId());

    ii = new IlluminaReadId(
        "SRR1577083.1 HWI-ST1160:266:D0H3RACXX:6:1315:4634:59858 length=50");
    assertTrue(ii.isRunIdField());
    assertEquals(266, ii.getRunId());
  }

  @Test
  public void testGetFlowCellId() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertFalse(ii.isFlowCellIdField());
    assertEquals(null, ii.getFlowCellId());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertFalse(ii.isFlowCellIdField());
    assertEquals(null, ii.getFlowCellId());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertFalse(ii.isFlowCellIdField());
    assertEquals(null, ii.getFlowCellId());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertFalse(ii.isFlowCellIdField());
    assertEquals(null, ii.getFlowCellId());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertFalse(ii.isFlowCellIdField());
    assertEquals(null, ii.getFlowCellId());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertTrue(ii.isFlowCellIdField());
    assertEquals("AB0868ABXX", ii.getFlowCellId());

    ii = new IlluminaReadId(
        "NB500892:67:HVN5KBGXX:1:11101:22912:1064 1:N:0:CTCTCTAC+TACTCCTT");
    assertTrue(ii.isFlowCellIdField());
    assertEquals("HVN5KBGXX", ii.getFlowCellId());

    ii = new IlluminaReadId("NB500892:10:H3YL2AFXX:1:11101:1108:1044 1:Y:0:1");
    assertTrue(ii.isFlowCellIdField());
    assertEquals("H3YL2AFXX", ii.getFlowCellId());

    ii = new IlluminaReadId(
        "SRR1577083.1 HWI-ST1160:266:D0H3RACXX:6:1315:4634:59858 length=50");
    assertTrue(ii.isFlowCellIdField());
    assertEquals("D0H3RACXX", ii.getFlowCellId());
  }

  @Test
  public void testGetFlowCellLane() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertTrue(ii.isFlowCellLaneField());
    assertEquals(6, ii.getFlowCellLane());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertTrue(ii.isFlowCellLaneField());
    assertEquals(6, ii.getFlowCellLane());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertTrue(ii.isFlowCellLaneField());
    assertEquals(6, ii.getFlowCellLane());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertTrue(ii.isFlowCellLaneField());
    assertEquals(7, ii.getFlowCellLane());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertTrue(ii.isFlowCellLaneField());
    assertEquals(7, ii.getFlowCellLane());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertTrue(ii.isFlowCellLaneField());
    assertEquals(3, ii.getFlowCellLane());

    ii = new IlluminaReadId(
        "NB500892:67:HVN5KBGXX:1:11101:22912:1064 1:N:0:CTCTCTAC+TACTCCTT");
    assertTrue(ii.isFlowCellLaneField());
    assertEquals(1, ii.getFlowCellLane());

    ii = new IlluminaReadId("NB500892:10:H3YL2AFXX:1:11101:1108:1044 1:Y:0:1");
    assertTrue(ii.isFlowCellLaneField());
    assertEquals(1, ii.getFlowCellLane());

    ii = new IlluminaReadId(
        "SRR1577083.1 HWI-ST1160:266:D0H3RACXX:6:1315:4634:59858 length=50");
    assertTrue(ii.isFlowCellLaneField());
    assertEquals(6, ii.getFlowCellLane());
  }

  @Test
  public void testGetTileNumberInFlowCellLane() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertTrue(ii.isTileNumberInFlowCellLaneField());
    assertEquals(73, ii.getTileNumberInFlowCellLane());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertTrue(ii.isTileNumberInFlowCellLaneField());
    assertEquals(73, ii.getTileNumberInFlowCellLane());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertTrue(ii.isTileNumberInFlowCellLaneField());
    assertEquals(73, ii.getTileNumberInFlowCellLane());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertTrue(ii.isTileNumberInFlowCellLaneField());
    assertEquals(100, ii.getTileNumberInFlowCellLane());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertTrue(ii.isTileNumberInFlowCellLaneField());
    assertEquals(100, ii.getTileNumberInFlowCellLane());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertTrue(ii.isTileNumberInFlowCellLaneField());
    assertEquals(1101, ii.getTileNumberInFlowCellLane());

    ii = new IlluminaReadId(
        "NB500892:67:HVN5KBGXX:1:11101:22912:1064 1:N:0:CTCTCTAC+TACTCCTT");
    assertTrue(ii.isTileNumberInFlowCellLaneField());
    assertEquals(11101, ii.getTileNumberInFlowCellLane());

    ii = new IlluminaReadId("NB500892:10:H3YL2AFXX:1:11101:1108:1044 1:Y:0:1");
    assertTrue(ii.isTileNumberInFlowCellLaneField());
    assertEquals(11101, ii.getTileNumberInFlowCellLane());

    ii = new IlluminaReadId(
        "SRR1577083.1 HWI-ST1160:266:D0H3RACXX:6:1315:4634:59858 length=50");
    assertTrue(ii.isTileNumberInFlowCellLaneField());
    assertEquals(1315, ii.getTileNumberInFlowCellLane());
  }

  @Test
  public void testGetxClusterCoordinateInTile() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertTrue(ii.isXClusterCoordinateInTileField());
    assertEquals(941, ii.getXClusterCoordinateInTile());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertTrue(ii.isXClusterCoordinateInTileField());
    assertEquals(941, ii.getXClusterCoordinateInTile());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertTrue(ii.isXClusterCoordinateInTileField());
    assertEquals(941, ii.getXClusterCoordinateInTile());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertTrue(ii.isXClusterCoordinateInTileField());
    assertEquals(10000, ii.getXClusterCoordinateInTile());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertTrue(ii.isXClusterCoordinateInTileField());
    assertEquals(10000, ii.getXClusterCoordinateInTile());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertTrue(ii.isXClusterCoordinateInTileField());
    assertEquals(1492, ii.getXClusterCoordinateInTile());

    ii = new IlluminaReadId(
        "NB500892:67:HVN5KBGXX:1:11101:22912:1064 1:N:0:CTCTCTAC+TACTCCTT");
    assertTrue(ii.isXClusterCoordinateInTileField());
    assertEquals(22912, ii.getXClusterCoordinateInTile());

    ii = new IlluminaReadId("NB500892:10:H3YL2AFXX:1:11101:1108:1044 1:Y:0:1");
    assertTrue(ii.isXClusterCoordinateInTileField());
    assertEquals(1108, ii.getXClusterCoordinateInTile());

    ii = new IlluminaReadId(
        "SRR1577083.1 HWI-ST1160:266:D0H3RACXX:6:1315:4634:59858 length=50");
    assertTrue(ii.isXClusterCoordinateInTileField());
    assertEquals(4634, ii.getXClusterCoordinateInTile());
  }

  @Test
  public void testGetyClusterCoordinateInTile() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertTrue(ii.isYClusterCoordinateInTileField());
    assertEquals(1973, ii.getYClusterCoordinateInTile());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertTrue(ii.isYClusterCoordinateInTileField());
    assertEquals(1973, ii.getYClusterCoordinateInTile());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertTrue(ii.isYClusterCoordinateInTileField());
    assertEquals(1973, ii.getYClusterCoordinateInTile());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertTrue(ii.isYClusterCoordinateInTileField());
    assertEquals(1220, ii.getYClusterCoordinateInTile());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertTrue(ii.isYClusterCoordinateInTileField());
    assertEquals(1220, ii.getYClusterCoordinateInTile());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertTrue(ii.isYClusterCoordinateInTileField());
    assertEquals(2178, ii.getYClusterCoordinateInTile());

    ii = new IlluminaReadId(
        "NB500892:67:HVN5KBGXX:1:11101:22912:1064 1:N:0:CTCTCTAC+TACTCCTT");
    assertTrue(ii.isYClusterCoordinateInTileField());
    assertEquals(1064, ii.getYClusterCoordinateInTile());

    ii = new IlluminaReadId("NB500892:10:H3YL2AFXX:1:11101:1108:1044 1:Y:0:1");
    assertTrue(ii.isYClusterCoordinateInTileField());
    assertEquals(1044, ii.getYClusterCoordinateInTile());

    ii = new IlluminaReadId(
        "SRR1577083.1 HWI-ST1160:266:D0H3RACXX:6:1315:4634:59858 length=50");
    assertTrue(ii.isYClusterCoordinateInTileField());
    assertEquals(59858, ii.getYClusterCoordinateInTile());
  }

  @Test
  public void testGetMultiplexedSample() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertTrue(ii.isSequenceIndexField());
    assertEquals("0", ii.getSequenceIndex());
    assertEquals(Collections.emptyList(), ii.getSequenceIndexList());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#ATCACG/1");
    assertTrue(ii.isSequenceIndexField());
    assertEquals("ATCACG", ii.getSequenceIndex());
    assertEquals(Collections.singletonList("ATCACG"), ii.getSequenceIndexList());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertFalse(ii.isSequenceIndexField());
    assertEquals("0", ii.getSequenceIndex());
    assertEquals(Collections.emptyList(), ii.getSequenceIndexList());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertFalse(ii.isSequenceIndexField());
    assertEquals("0", ii.getSequenceIndex());
    assertEquals(Collections.emptyList(), ii.getSequenceIndexList());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertFalse(ii.isSequenceIndexField());
    assertEquals("0", ii.getSequenceIndex());
    assertEquals(Collections.emptyList(), ii.getSequenceIndexList());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertFalse(ii.isSequenceIndexField());
    assertEquals("0", ii.getSequenceIndex());
    assertEquals(Collections.emptyList(), ii.getSequenceIndexList());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertTrue(ii.isSequenceIndexField());
    assertEquals("ATCACG", ii.getSequenceIndex());
    assertEquals(Arrays.asList("ATCACG"), ii.getSequenceIndexList());

    ii = new IlluminaReadId(
        "NB500892:67:HVN5KBGXX:1:11101:22912:1064 1:N:0:CTCTCTAC+TACTCCTT");
    assertTrue(ii.isSequenceIndexField());
    assertEquals("CTCTCTAC+TACTCCTT", ii.getSequenceIndex());
    assertEquals(Arrays.asList("CTCTCTAC", "TACTCCTT"),
        ii.getSequenceIndexList());

    ii = new IlluminaReadId("NB500892:10:H3YL2AFXX:1:11101:1108:1044 1:Y:0:1");
    assertFalse(ii.isSequenceIndexField());
    assertEquals("0", ii.getSequenceIndex());

    ii = new IlluminaReadId(
        "SRR1577083.1 HWI-ST1160:266:D0H3RACXX:6:1315:4634:59858 length=50");
    assertFalse(ii.isSequenceIndexField());
    assertEquals("0", ii.getSequenceIndex());
  }

  @Test
  public void testGetPairMember() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertTrue(ii.isPairMemberField());
    assertEquals(1, ii.getPairMember());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertTrue(ii.isPairMemberField());
    assertEquals(1, ii.getPairMember());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertFalse(ii.isPairMemberField());
    assertEquals(-1, ii.getPairMember());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertEquals(1, ii.getPairMember());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertFalse(ii.isPairMemberField());
    assertEquals(-1, ii.getPairMember());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertTrue(ii.isPairMemberField());
    assertEquals(1, ii.getPairMember());

    ii = new IlluminaReadId(
        "NB500892:67:HVN5KBGXX:1:11101:22912:1064 1:N:0:CTCTCTAC+TACTCCTT");
    assertTrue(ii.isPairMemberField());
    assertEquals(1, ii.getPairMember());

    ii = new IlluminaReadId("NB500892:10:H3YL2AFXX:1:11101:1108:1044 1:Y:0:1");
    assertTrue(ii.isPairMemberField());
    assertEquals(1, ii.getPairMember());

    ii = new IlluminaReadId(
        "SRR1577083.1 HWI-ST1160:266:D0H3RACXX:6:1315:4634:59858 length=50");
    assertFalse(ii.isPairMemberField());
    assertEquals(-1, ii.getPairMember());
  }

  @Test
  public void testisFiltered() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertFalse(ii.isFilteredField());
    assertFalse(ii.isFiltered());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertTrue(ii.isFilteredField());
    assertFalse(ii.isFiltered());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertFalse(ii.isFilteredField());
    assertFalse(ii.isFiltered());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertFalse(ii.isFilteredField());
    assertFalse(ii.isFiltered());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertFalse(ii.isFilteredField());
    assertFalse(ii.isFiltered());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertFalse(ii.isFilteredField());
    assertFalse(ii.isFiltered());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:Y:0:ATCACG");
    assertTrue(ii.isFilteredField());
    assertTrue(ii.isFiltered());

    ii = new IlluminaReadId(
        "NB500892:67:HVN5KBGXX:1:11101:22912:1064 1:N:0:CTCTCTAC+TACTCCTT");
    assertTrue(ii.isFilteredField());
    assertFalse(ii.isFiltered());

    ii = new IlluminaReadId("NB500892:10:H3YL2AFXX:1:11101:1108:1044 1:Y:0:1");
    assertTrue(ii.isFilteredField());
    assertTrue(ii.isFiltered());

    ii = new IlluminaReadId(
        "SRR1577083.1 HWI-ST1160:266:D0H3RACXX:6:1315:4634:59858 length=50");
    assertFalse(ii.isFilteredField());
    assertFalse(ii.isFiltered());
  }

  @Test
  public void testGetControlNumber() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");
    assertFalse(ii.isControlNumberField());
    assertEquals(-1, ii.getControlNumber());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973/1");
    assertFalse(ii.isControlNumberField());
    assertEquals(-1, ii.getControlNumber());

    ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973");
    assertFalse(ii.isControlNumberField());
    assertEquals(-1, ii.getControlNumber());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220/1");
    assertFalse(ii.isControlNumberField());
    assertEquals(-1, ii.getControlNumber());

    ii = new IlluminaReadId("SOLEXA3_162:7:100:10000:1220");
    assertFalse(ii.isControlNumberField());
    assertEquals(-1, ii.getControlNumber());

    ii = new IlluminaReadId(
        "HWI-1KL110:24:AB0868ABXX:3:1101:1492:2178 1:N:0:ATCACG");
    assertTrue(ii.isControlNumberField());
    assertEquals(0, ii.getControlNumber());

    ii = new IlluminaReadId(
        "NB500892:67:HVN5KBGXX:1:11101:22912:1064 1:N:0:CTCTCTAC+TACTCCTT");
    assertTrue(ii.isControlNumberField());
    assertEquals(0, ii.getControlNumber());

    ii = new IlluminaReadId("NB500892:10:H3YL2AFXX:1:11101:1108:1044 1:Y:0:1");
    assertTrue(ii.isControlNumberField());
    assertEquals(0, ii.getControlNumber());

    ii = new IlluminaReadId(
        "SRR1577083.1 HWI-ST1160:266:D0H3RACXX:6:1315:4634:59858 length=50");
    assertFalse(ii.isControlNumberField());
    assertEquals(-1, ii.getControlNumber());
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
