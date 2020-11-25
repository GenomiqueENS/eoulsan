package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

public class GGGHeadReadFilterTest {

  @Test
  public void testAcceptReadSequenceReadSequence() {

    ReadFilter filter = new GGGHeadReadFilter();

    ReadSequence r = new ReadSequence("96f7d3fa-263e-465d-b2e9-bbe2998426f4",
        "GGGNNNNNCCC", "!!!!!!!!!!!");

    assertTrue(filter.accept(r));
    assertEquals(
        "96f7d3fa-263e-465d-b2e9-bbe2998426f4 start_sequence=GGG start_G_count=3 end_sequence=CCC end_C_count=3",
        r.getName());

    r = new ReadSequence("96f7d3fa-263e-465d-b2e9-bbe2998426f4", "CCC", "!!!");

    assertTrue(filter.accept(r));
    assertEquals(
        "96f7d3fa-263e-465d-b2e9-bbe2998426f4 start_sequence=CCC start_G_count=0 end_sequence=CCC end_C_count=3",
        r.getName());

    r = new ReadSequence("96f7d3fa-263e-465d-b2e9-bbe2998426f4", "C", "!");

    assertTrue(filter.accept(r));
    assertEquals(
        "96f7d3fa-263e-465d-b2e9-bbe2998426f4 start_sequence=C start_G_count=0 end_sequence=C end_C_count=1",
        r.getName());

    r = new ReadSequence("96f7d3fa-263e-465d-b2e9-bbe2998426f4", "", "");

    assertTrue(filter.accept(r));
    assertEquals(
        "96f7d3fa-263e-465d-b2e9-bbe2998426f4 start_sequence= start_G_count=0 end_sequence= end_C_count=0",
        r.getName());
  }

}

