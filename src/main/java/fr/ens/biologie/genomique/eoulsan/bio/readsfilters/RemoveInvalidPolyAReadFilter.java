package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;
import fr.ens.biologie.genomique.eoulsan.bio.readsfilters.PolyATailReadFilter.TailType;

/**
 * This class define a read filter that remove reads without a valid polyA tail.
 * @since 2.4
 * @author Laurent Jourdren
 */
public class RemoveInvalidPolyAReadFilter extends AbstractReadFilter {

  public static final String FILTER_NAME = "removeinvalidpolya";
  private Splitter splitter = Splitter.on(' ').omitEmptyStrings().trimResults();
  private Set<String> allowed = new HashSet<>(Arrays.asList("polya", "polyt"));

  @Override
  public String getName() {

    return FILTER_NAME;
  }

  @Override
  public String getDescription() {

    return "PolyA invalid type filter";
  }

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    switch (key) {

    case "allowed.tail.type":

      this.allowed.clear();

      for (String type : Splitter.on(',').omitEmptyStrings().trimResults()
          .split(value.toLowerCase())) {

        TailType t = TailType.parse(type);

        if (t == null) {
          throw new EoulsanException("Unknown value for "
              + getName() + "." + key + " parameter: " + type);
        }

        this.allowed.add(t.toString().toLowerCase());
      }

      break;

    default:
      throw new EoulsanException(
          "Unknown parameter for " + getName() + " read filter: " + key);
    }

  }

  @Override
  public boolean accept(ReadSequence read) {

    String readName = read.getName();

    for (String s : this.splitter.splitToList(readName)) {

      s = s.toLowerCase().replace("\"", "").replace(" ", "");

      if (!s.startsWith("tail_type=")) {
        continue;
      }

      s = s.substring(s.indexOf('=') + 1);

      if (allowed.contains(s)) {
        return true;
      }

    }

    return false;
  }

}
