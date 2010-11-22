package fr.ens.transcriptome.eoulsan.bio.readsfilters;

import fr.ens.transcriptome.eoulsan.bio.ReadSequence;

public class PairCheckReadFilter extends AbstractReadFilter {

  @Override
  public boolean accept(final ReadSequence read1, final ReadSequence read2) {

    if (read1 == null)
      throw new NullPointerException("Read1 is null");

    if (read2 == null)
      throw new NullPointerException("Read2 is null");

    final String id1 = read1.getName();
    final String id2 = read2.getName();

    if (id1 == null)
      throw new NullPointerException("Read1 id is null");

    if (id2 == null)
      throw new NullPointerException("Read2 id is null");

    if (id1.equals(id2))
      return false;

    final int len1 = id1.length();
    final int len2 = id2.length();

    if (len1 != len2)
      return false;

    if (!id1.endsWith("/1"))
      return false;

    if (!id2.endsWith("/2"))
      return false;

    final int len = len1 - 2;
    final String prefix1 = id1.substring(0, len);
    final String prefix2 = id2.substring(0, len);

    return prefix1.equals(prefix2);
  }

  @Override
  public boolean accept(ReadSequence read) {
   
    return true;
  }

}
