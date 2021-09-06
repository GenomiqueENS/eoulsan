package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

public class RemoveInvalidPolyAReadFilterTest {

  @Test
  public void testAcceptReadSequence() {

    ReadFilter filter = new RemoveInvalidPolyAReadFilter();

    assertTrue(filter.accept(new ReadSequence(
        "0327f6d9-8ec8-4369-9763-869bf4c30353 runid=93f969536487e5b7e7f11d78f5b14fcf7708f8f5 sampleid=GVUMLC1_A2018 read=13 ch=143 start_time=2018-09-27T14:45:38Z barcode=barcode07 tail_type=polyT start_sequence=TTT start_G_count=0 end_sequence=AAA end_C_count=0",
        "ATGC", "!!!!")));

    assertFalse(filter.accept(new ReadSequence(
        "d35c4c91-a387-4a88-b472-067d0caf0603 runid=93f969536487e5b7e7f11d78f5b14fcf7708f8f5 sampleid=TOTO_A2018 read=9046 ch=337 start_time=2018-09-27T16:29:42Z",
        "ATGC", "!!!!")));

    assertTrue(filter.accept(new ReadSequence(
        "0327f6d9-8ec8-4369-9763-869bf4c30353 runid=93f969536487e5b7e7f11d78f5b14fcf7708f8f5 sampleid=GVUMLC1_A2018 read=13 ch=143 start_time=2018-09-27T14:45:38Z barcode=barcode07 tail_type=polyA start_sequence=TTT start_G_count=0 end_sequence=AAA end_C_count=0",
        "ATGC", "!!!!")));

    assertFalse(filter.accept(new ReadSequence(
        "0327f6d9-8ec8-4369-9763-869bf4c30353 runid=93f969536487e5b7e7f11d78f5b14fcf7708f8f5 sampleid=GVUMLC1_A2018 read=13 ch=143 start_time=2018-09-27T14:45:38Z barcode=barcode07 tail_type=invalid start_sequence=TTT start_G_count=0 end_sequence=AAA end_C_count=0",
        "ATGC", "!!!!")));

    assertFalse(filter.accept(new ReadSequence(
        "0327f6d9-8ec8-4369-9763-869bf4c30353 runid=93f969536487e5b7e7f11d78f5b14fcf7708f8f5 sampleid=GVUMLC1_A2018 read=13 ch=143 start_time=2018-09-27T14:45:38Z barcode=barcode07 tail_type=ambiguous start_sequence=TTT start_G_count=0 end_sequence=AAA end_C_count=0",
        "ATGC", "!!!!")));

    assertTrue(filter.accept(new ReadSequence(
        "0327f6d9-8ec8-4369-9763-869bf4c30353 runid=93f969536487e5b7e7f11d78f5b14fcf7708f8f5 sampleid=GVUMLC1_A2018 read=13 ch=143 start_time=2018-09-27T14:45:38Z barcode=barcode07 tail_type=polyt start_sequence=TTT start_G_count=0 end_sequence=AAA end_C_count=0",
        "ATGC", "!!!!")));

    assertTrue(filter.accept(new ReadSequence(
        "0327f6d9-8ec8-4369-9763-869bf4c30353 runid=93f969536487e5b7e7f11d78f5b14fcf7708f8f5 sampleid=GVUMLC1_A2018 read=13 ch=143 start_time=2018-09-27T14:45:38Z barcode=barcode07 tail_type=polya start_sequence=TTT start_G_count=0 end_sequence=AAA end_C_count=0",
        "ATGC", "!!!!")));

    assertFalse(filter.accept(new ReadSequence(
        "0327f6d9-8ec8-4369-9763-869bf4c30353 runid=93f969536487e5b7e7f11d78f5b14fcf7708f8f5 sampleid=GVUMLC1_A2018 read=13 ch=143 start_time=2018-09-27T14:45:38Z barcode=barcode07 tail_type= start_sequence=TTT start_G_count=0 end_sequence=AAA end_C_count=0",
        "ATGC", "!!!!")));
  }
}