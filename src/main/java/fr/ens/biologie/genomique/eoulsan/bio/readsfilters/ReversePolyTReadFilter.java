package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

/**
 * This class define a read filter that reverse reads marked as "polyT"
 * @since 2.4
 * @author Laurent Jourdren
 */
public class ReversePolyTReadFilter extends AbstractReadFilter {

  public static final String FILTER_NAME = "reversepolyt";
  private Splitter splitter = Splitter.on(' ').omitEmptyStrings().trimResults();

  @Override
  public String getName() {

    return FILTER_NAME;
  }

  @Override
  public String getDescription() {

    return "Reverse complements polyT reads";
  }

  @Override
  public boolean accept(ReadSequence read) {

    String readName = read.getName();

    for (String s : this.splitter.splitToList(readName)) {

      s = s.toLowerCase().replace("\"", "").replace(" ", "");

      if ("tail_type=polyt".equals(s)) {
        read.reverseComplement();
      }
    }

    return true;
  }

}
