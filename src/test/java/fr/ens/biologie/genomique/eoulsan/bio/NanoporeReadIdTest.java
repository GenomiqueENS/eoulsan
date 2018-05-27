package fr.ens.biologie.genomique.eoulsan.bio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.NanoporeReadId.SequenceType;

public class NanoporeReadIdTest {

  @Test
  public void testParse() throws EoulsanException {

    NanoporeReadId nrid =
        new NanoporeReadId("e122e34a-80bf-4fd6-bdbf-d1d3cb44f3bd_t "
            + "runid=dacde12d52d5856ad2b13a3eea62711f4189e4bb "
            + "read=1 ch=104 start_time=2017-02-10T10:39:42Z barcode=barcode03");

    assertEquals("e122e34a-80bf-4fd6-bdbf-d1d3cb44f3bd_t", nrid.getReadId());
    assertEquals(SequenceType.TEMPLATE, nrid.getSequenceType());
    assertEquals("dacde12d52d5856ad2b13a3eea62711f4189e4bb", nrid.getRunId());
    assertEquals(1, nrid.getReadNumber());
    assertEquals(104, nrid.getChannel());
    assertEquals("2017-02-10T10:39:42Z", nrid.getStartTime());
    assertEquals("barcode03", nrid.getBarcode());
    assertTrue(nrid.isBarcoded());

    nrid = new NanoporeReadId("e122e34a-80bf-4fd6-bdbf-d1d3cb44f3bd_c "
        + "runid=dacde12d52d5856ad2b13a3eea62711f4189e4bb "
        + "read=1 ch=104 start_time=2017-02-10T10:39:42Z barcode=barcode03");

    assertEquals("e122e34a-80bf-4fd6-bdbf-d1d3cb44f3bd_c", nrid.getReadId());
    assertEquals(SequenceType.COMPLEMENT, nrid.getSequenceType());

    nrid = new NanoporeReadId("e122e34a-80bf-4fd6-bdbf-d1d3cb44f3bd "
        + "runid=dacde12d52d5856ad2b13a3eea62711f4189e4bb "
        + "read=1 ch=104 start_time=2017-02-10T10:39:42Z barcode=barcode03");

    assertEquals("e122e34a-80bf-4fd6-bdbf-d1d3cb44f3bd", nrid.getReadId());
    assertEquals(SequenceType.CONSENSUS, nrid.getSequenceType());

    nrid.parse("c5ab7dea-df8c-4a9e-b31e-58d01c35fe5f "
        + "runid=e2332b19f68f9529ed158f757dabb37905cae82c "
        + "read=4597 ch=236 start_time=2017-02-10T22:22:44Z barcode=barcode02");
    assertEquals("c5ab7dea-df8c-4a9e-b31e-58d01c35fe5f", nrid.getReadId());
    assertEquals("e2332b19f68f9529ed158f757dabb37905cae82c", nrid.getRunId());
    assertEquals(4597, nrid.getReadNumber());
    assertEquals(236, nrid.getChannel());
    assertEquals("2017-02-10T22:22:44Z", nrid.getStartTime());
    assertEquals("barcode02", nrid.getBarcode());
    assertTrue(nrid.isBarcoded());

    try {
      nrid.parse(null);
      assertFalse(true);
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    nrid.parse("");
    assertNull(nrid.getReadId());
    assertNull(nrid.getRunId());
    assertEquals(-1, nrid.getReadNumber());
    assertEquals(-1, nrid.getChannel());
    assertNull(nrid.getStartTime());
    assertNull(nrid.getBarcode());
    assertFalse(nrid.isBarcoded());

  }

}
