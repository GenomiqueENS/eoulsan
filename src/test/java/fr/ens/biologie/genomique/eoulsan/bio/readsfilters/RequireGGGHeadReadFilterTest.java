package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

public class RequireGGGHeadReadFilterTest {

  @Test
  public void testAcceptReadSequenceReadSequence() {

    ReadFilter filter = new RequireGGGHeadReadFilter();
    try {
      filter.setParameter("allow.mismatch", "false");
    } catch (EoulsanException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    ReadSequence r;

    r = new ReadSequence("seqname", "GGGNNNNNCCC", "!!!!!!!!!!!");
    assertTrue(filter.accept(r));

    r = new ReadSequence("seqname", "GGNNNNNNCCC", "!!!!!!!!!!!");
    assertFalse(filter.accept(r));

    r = new ReadSequence("seqname", "GNNNNNNNCCC", "!!!!!!!!!!!");
    assertFalse(filter.accept(r));

    filter = new RequireGGGHeadReadFilter();
    try {
      filter.setParameter("allow.mismatch", "true");
    } catch (EoulsanException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    r = new ReadSequence("seqname", "GGGNNNNNCCC", "!!!!!!!!!!!");
    assertTrue(filter.accept(r));

    r = new ReadSequence("seqname", "GGNNNNNNCCC", "!!!!!!!!!!!");
    assertTrue(filter.accept(r));

    r = new ReadSequence("seqname", "GNNNNNNNCCC", "!!!!!!!!!!!");
    assertFalse(filter.accept(r));

  }

}
