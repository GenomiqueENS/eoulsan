package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

/**
 * This filter will search for GGG head and CCC tail and add additional fields
 * in read header fields.
 * @since 2.4
 * @author Laurent Jourdren
 */
public class GGGHeadReadFilter extends AbstractReadFilter {

  public static final String FILTER_NAME = "ggghead";

  private static final int ADDITIONAL_BASE_COUNT = 3;

  @Override
  public String getName() {

    return FILTER_NAME;
  }

  @Override
  public String getDescription() {

    return "GGG head identifier";
  }

  @Override
  public boolean accept(ReadSequence read) {

    String sequence = read.getSequence();
    int length = read.length();

    String start =
        sequence.substring(0, Math.min(ADDITIONAL_BASE_COUNT, length));
    String end =
        sequence.substring(Math.max(length - ADDITIONAL_BASE_COUNT, 0));

    read.setName(read.getName()
        + " start_sequence=" + start + " start_G_count=" + count(start, 'G')
        + " end_sequence=" + end + " end_C_count=" + count(end, 'C'));

    return true;
  }

  private static int count(String s, char c) {

    int result = 0;

    for (int i = 0; i < s.length(); i++) {

      if (s.charAt(i) == c) {
        result++;
      }
    }

    return result;
  }

}
