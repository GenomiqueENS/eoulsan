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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.util.Utils;

public class GenomicArray<T> {

  private final Map<String, ChromosomeZones<T>> chromosomes = Utils
      .newHashMap();

  /**
   * This class define a zone in a ChromosomeZone object
   * @author Laurent Jourdren
   */
  private static final class Zone<T> implements Serializable {

    private final int start;
    private int end;
    private final char strand;

    private Set<T> _exons;
    private T _exon;
    private int exonCount;

    /**
     * Add an exon to the zone.
     * @param exon Exon to add
     */
    public void addExon(final T exon) {

      if (exon == null)
        return;

      if (exonCount == 0) {
        this._exon = exon;
        this.exonCount = 1;
      } else {

        if (exonCount == 1) {

          if (exon == this._exon || this._exon.hashCode() == exon.hashCode())
            return;

          this._exons = new HashSet<T>();
          this._exons.add(this._exon);
          this._exon = null;
        }

        this._exons.add(exon);
        this.exonCount = this._exons.size();
      }
    }

    /**
     * Add exons to the zone.
     * @param exons Exons to add
     */
    private void addExons(final Set<T> exons) {

      if (exons == null)
        return;

      final int len = exons.size();

      if (len == 0)
        return;

      if (len == 1) {
        this._exon = exons.iterator().next();
        this.exonCount = this._exon == null ? 0 : 1;
      } else {
        this._exons = new HashSet<T>(exons);
        this.exonCount = len;
      }

    }

    /**
     * Get the exons of the zone
     * @return a set with the exons of the zone
     */
    public Set<T> getExons() {

      if (this.exonCount == 0)
        return null;

      if (this.exonCount == 1)
        return Collections.singleton(this._exon);

      return this._exons;
    }

    public int compareTo(final int position) {

      if (position >= this.start && position <= this.end)
        return 0;

      return position < this.start ? -1 : 1;
    }

    @Override
    public String toString() {

      Set<String> r = new HashSet<String>();
      if (getExons() != null)
        for (T e : getExons())
          r.add(e.toString());

      return "[" + this.start + "," + this.end + "," + r + "]";
    }

    //
    // Constructor
    //

    /**
     * Constructor that create a zone
     * @param start start position of the zone
     * @param end end postion of the zone
     * @param strand strand of the zone
     */
    public Zone(final int start, final int end, final char strand) {

      this.start = start;
      this.end = end;
      this.strand = strand;
    }

    /**
     * Constructor that create a zone
     * @param start start position of the zone
     * @param end end postion of the zone
     * @param strand strand of the zone
     * @param exons of the zone
     */
    public Zone(final int start, final int end, final char strand,
        final Set<T> exons) {

      this(start, end, strand);
      addExons(exons);
    }

  }

  private static final class ChromosomeZones<T> implements Serializable {

    private final String chromosomeName;
    private int length = 0;
    private final List<Zone<T>> zones = new ArrayList<Zone<T>>();

    private final Zone<T> get(int pos) {

      return this.zones.get(pos);
    }

    private final void add(final Zone<T> z) {

      this.zones.add(z);
    }

    private final void add(final int pos, final Zone<T> z) {

      this.zones.add(pos, z);
    }

    private int findIndexPos(final int pos) {

      if (pos < 1 || pos > this.length)
        return -1;

      int minIndex = 0;
      int maxIndex = zones.size() - 1;
      int index = 0;

      while (true) {

        final int diff = maxIndex - minIndex;
        index = minIndex + diff / 2;

        if (diff == 1) {

          if (get(minIndex).compareTo(pos) == 0)
            return minIndex;
          if (get(maxIndex).compareTo(pos) == 0)
            return maxIndex;

          assert (false);
        }

        final Zone<T> z = get(index);

        final int comp = z.compareTo(pos);
        if (comp == 0)
          return index;

        if (comp < 0)
          maxIndex = index;
        else
          minIndex = index;
      }
    }

    private Zone<T> splitZone(final Zone<T> zone, final int pos) {

      final Zone<T> result =
          new Zone<T>(pos, zone.end, zone.strand, zone.getExons());
      zone.end = pos - 1;

      return result;
    }

    public void addExon(final GenomicInterval exon, final T value) {

      final int exonStart = exon.getStart();
      final int exonEnd = exon.getEnd();

      // Create an empty zone if the exon is after the end of the chromosome
      if (exon.getEnd() > this.length) {
        add(new Zone<T>(this.length + 1, exonEnd, exon.getStrand()));
        this.length = exonEnd;
      }

      final int indexStart = findIndexPos(exonStart);
      final int indexEnd = findIndexPos(exonEnd);

      final Zone<T> z1 = get(indexStart);
      final Zone<T> z1b;
      final int count1b;

      if (z1.start == exonStart) {
        z1b = z1;
        count1b = 0;
      } else {
        z1b = splitZone(z1, exonStart);
        count1b = 1;
      }

      // Same index
      if (indexStart == indexEnd) {

        if (z1b.end == exonEnd) {
          z1b.addExon(value);
        } else {

          final Zone<T> z1c = splitZone(z1b, exonEnd + 1);
          add(indexStart + 1, z1c);
        }

        if (z1 != z1b) {
          z1b.addExon(value);
          add(indexStart + 1, z1b);

        } else
          z1.addExon(value);

      } else {

        final Zone<T> z2 = get(indexEnd);
        final Zone<T> z2b;

        if (z2.end != exonEnd)
          z2b = splitZone(z2, exonEnd + 1);
        else
          z2b = z2;

        if (z1 != z1b)
          add(indexStart + 1, z1b);
        if (z2 != z2b)
          add(indexEnd + 1 + count1b, z2b);

        for (int i = indexStart + count1b; i <= indexEnd + count1b; i++)
          get(i).addExon(value);

      }

    }

    public Map<GenomicInterval, T> findExons(final int start, final int stop) {

      final int indexStart = findIndexPos(start);
      final int indexEnd = findIndexPos(stop);

      if (indexStart == -1)
        return null;

      final int from = indexStart;
      final int to = indexEnd == -1 ? this.zones.size() - 1 : indexEnd;

      Map<GenomicInterval, T> result = null;

      for (int i = from; i <= to; i++) {

        final Zone<T> zone = get(i);
        final Set<T> r = zone.getExons();
        if (r != null) {

          for (T e : r)
            // Really needed ?
            if (intersect(start, stop, zone.start, zone.end)) {

              if (result == null)
                result = Utils.newHashMap();

              result.put(new GenomicInterval(this.chromosomeName, zone.start,
                  zone.end, zone.strand), e);
            }

        }
      }

      return result;
    }

    public static final boolean intersect(final int start, final int end,
        final int startZone, final int endZone) {

      return (start >= startZone && start <= endZone)
          || (end >= startZone && end <= endZone)
          || (start < startZone && end > endZone);
    }

    //
    // Constructor
    //

    public ChromosomeZones(final String chromosome) {
      this.chromosomeName = chromosome;
    }
  }

  public void addExon(final GenomicInterval exon, final T value) {

    if (exon == null)
      return;

    final String chromosomeName = exon.getChromosome();
    final ChromosomeZones<T> chr;

    if (!this.chromosomes.containsKey(chromosomeName)) {
      chr = new ChromosomeZones<T>(chromosomeName);
      this.chromosomes.put(chromosomeName, chr);
    } else
      chr = this.chromosomes.get(chromosomeName);

    chr.addExon(exon, value);
  }

  public Map<GenomicInterval, T> findExons(final String chromosome,
      final int start, final int stop) {

    final ChromosomeZones<T> chr = this.chromosomes.get(chromosome);

    if (chr == null)
      return null;

    return chr.findExons(start, stop);
  }

  public boolean containsChromosome(final String chromosomeName) {

    if (chromosomeName == null)
      return false;

    return this.chromosomes.containsKey(chromosomeName);
  }

  public String print() {

    final StringBuilder sb = new StringBuilder();

    final List<String> chrList =
        new ArrayList<String>(this.chromosomes.keySet());
    Collections.sort(chrList);

    for (final String chrName : chrList) {
      final ChromosomeZones<T> chr = this.chromosomes.get(chrName);

      for (Zone<T> z : chr.zones) {
        final String set;
        if (z.exonCount == 0)
          set = "[]";
        else if (z.exonCount == 1)
          set = "['" + z._exon + "']";
        else {
          StringBuilder sb2 = new StringBuilder();
          boolean first = true;
          List<String> list = new ArrayList<String>();
          for (T v : z._exons)
            list.add(v.toString());
          Collections.sort(list);
          for (String v : list) {
            if (first)
              first = false;
            else
              sb2.append(", ");
            sb2.append('\'');
            sb2.append(v);
            sb2.append('\'');
          }
          set = "[" + sb2.toString() + "]";
        }
        sb.append(chrName
            + ":[" + (z.start - 1) + "," + (z.end) + ")/" + z.strand + " "
            + set + "\n");
      }
    }
    // chr1:[3092096,3092206)/+ set(['ENSMUSG00000064842'])
    return sb.toString();
  }

}
