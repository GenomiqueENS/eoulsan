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

import static org.junit.Assert.*;

import org.junit.Test;

import fr.ens.transcriptome.eoulsan.EoulsanException;

public class IlluminaReadIdTest {

  @Test
  public void testGetInstrumentId() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");

    assertEquals("HWUSI-EAS100R", ii.getInstrumentId());
  }

  @Test
  public void testGetFlowCellLane() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");

    assertEquals(6, ii.getFlowCellLane());
  }

  @Test
  public void testGetTileNumberInFlowCellLane() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");

    assertEquals(73, ii.getTileNumberInFlowCellLane());
  }

  @Test
  public void testGetxClusterCoordinateInTile() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");

    assertEquals(941, ii.getXClusterCoordinateInTile());
  }

  @Test
  public void testGetyClusterCoordinateInTile() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");

    assertEquals(1973, ii.getYClusterCoordinateInTile());
  }

  @Test
  public void testGetMultiplexedSample() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");

    assertEquals(0, ii.getMultiplexedSample());
  }

  @Test
  public void testGetPairMember() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");

    assertEquals(1, ii.getPairMember());
  }

  @Test
  public void testParse() throws EoulsanException {

    IlluminaReadId ii = new IlluminaReadId("HWUSI-EAS100R:6:73:941:1973#0/1");

    assertEquals("HWUSI-EAS100R", ii.getInstrumentId());
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

  }

}
