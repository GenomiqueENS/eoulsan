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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.bio;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import fr.ens.biologie.genomique.eoulsan.util.Utils;

/**
 * This class define a genomic array. TODO more doc and rename attributes and
 * field of the inner classes
 * @since 1.2
 * @author Laurent Jourdren
 */
public class GenomicArray<T> implements Serializable {

  private static final long serialVersionUID = 539825064205425262L;

  private Map<String, ChromosomeZones<T>> chromosomes = new HashMap<>();

  /**
   * This class define a zone in a ChromosomeZone object.
   * @author Laurent Jourdren
   */
  private static final class Zone<T> implements Serializable {

    private static final long serialVersionUID = 3581472137861260840L;

    private final int start;
    private int end;
    private final char strand;

    private Set<T> _values;
    private T _value;
    private int valueCount;

    /**
     * Add a value to the zone.
     * @param value Exon to add
     */
    public void addExon(final T value) {

      if (value == null) {
        throw new NullPointerException("value argument cannot be null");
      }

      if (this.valueCount == 0) {
        this._value = value;
        this.valueCount = 1;
      } else {

        if (this.valueCount == 1) {

          if (value == this._value
              || this._value.hashCode() == value.hashCode()) {
            return;
          }

          this._values = new HashSet<>();
          this._values.add(this._value);
          this._value = null;
        }

        this._values.add(value);
        this.valueCount = this._values.size();
      }
    }

    /**
     * Add values to the zone.
     * @param values values to add
     */
    private void addExons(final Set<T> values) {

      if (values == null) {
        return;
      }

      final int len = values.size();

      if (len == 0) {
        return;
      }

      if (len == 1) {
        this._value = values.iterator().next();
        this.valueCount = this._value == null ? 0 : 1;
      } else {
        this._values = new HashSet<>(values);
        this.valueCount = len;
      }

    }

    /**
     * Get the values of the zone.
     * @return a set with the values of the zone
     */
    public Set<T> getValues() {

      if (this.valueCount == 0) {
        return null;
      }

      if (this.valueCount == 1) {
        return Collections.singleton(this._value);
      }

      return this._values;
    }

    /**
     * Test if a position is before, in or after the zone.
     * @param position to test
     * @return -1 if position is before the zone, 0 if the position is in the
     *         zone and 1 of the position is after the zone
     */
    public int compareTo(final int position) {

      if (position >= this.start && position <= this.end) {
        return 0;
      }

      return position < this.start ? -1 : 1;
    }

    @Override
    public String toString() {

      Set<String> r = new HashSet<>();
      if (getValues() != null) {
        for (T e : getValues()) {
          r.add(e.toString());
        }
      }

      return this.getClass().getSimpleName()
          + "{" + this.start + "," + this.end + "," + r + "}";
    }

    @Override
    public boolean equals(final Object o) {

      if (o == this) {
        return true;
      }

      if (!(o instanceof Zone<?>)) {
        return false;
      }

      final Zone<?> that = (Zone<?>) o;

      if (!(Utils.equal(this.valueCount, that.valueCount)
          && Utils.equal(this.start, that.start)
          && Utils.equal(this.end, that.end)
          && Utils.equal(this.strand, that.strand))) {
        return false;
      }

      switch (this.valueCount) {

      case 0:
        return true;

      case 1:
        return Utils.equal(this._value, that._value);

      default:
        return Utils.equal(this._values, that._values);
      }

    }

    @Override
    public int hashCode() {

      return Utils.hashCode(this._value, this._values, this.start, this.end,
          this.strand, this.valueCount);
    }

    //
    // Constructor
    //

    /**
     * Constructor that create a zone
     * @param start start position of the zone
     * @param end end position of the zone
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
     * @param end end position of the zone
     * @param strand strand of the zone
     * @param exons of the zone
     */
    public Zone(final int start, final int end, final char strand,
        final Set<T> exons) {

      this(start, end, strand);
      addExons(exons);
    }

  }

  /**
   * This class define an object that contains all the stranded zones of a
   * chromosome.
   * @author Laurent Jourdren
   */
  private static final class ChromosomeStrandedZones<T>
      implements Serializable {

    private static final long serialVersionUID = 8073207058699194059L;

    private final String chromosomeName;
    private int length = 0;
    private final List<Zone<T>> zones = new ArrayList<>();

    private Zone<T> get(final int index) {

      return this.zones.get(index);
    }

    /**
     * Add a zone.
     * @param zone zone to add
     */
    private void add(final Zone<T> zone) {

      this.zones.add(zone);
    }

    /**
     * Add a zone.
     * @param index index where add the zone
     * @param zone the zone to add
     */
    private void add(final int index, final Zone<T> zone) {

      this.zones.add(index, zone);
    }

    /**
     * Find the zone index for a position.
     * @param pos the position on the chromosome
     * @return the index of the zone or -1 if the position if lower than 1 or
     *         greater than the length of the chromosome
     */
    private int findIndexPos(final int pos) {

      if (pos < 1 || pos > this.length) {
        return -1;
      }

      int minIndex = 0;
      int maxIndex = this.zones.size() - 1;
      int index = 0;

      while (true) {

        final int diff = maxIndex - minIndex;
        index = minIndex + diff / 2;

        if (diff == 1) {

          if (get(minIndex).compareTo(pos) == 0) {
            return minIndex;
          }
          if (get(maxIndex).compareTo(pos) == 0) {
            return maxIndex;
          }

          assert (false);
        }

        final Zone<T> z = get(index);

        final int comp = z.compareTo(pos);
        if (comp == 0) {
          return index;
        }

        if (comp < 0) {
          maxIndex = index;
        } else {
          minIndex = index;
        }
      }
    }

    /**
     * Split a zone in two zone.
     * @param zone zone to split
     * @param pos position of the split
     * @return a new zone object
     */
    private Zone<T> splitZone(final Zone<T> zone, final int pos) {

      final Zone<T> result =
          new Zone<>(pos, zone.end, zone.strand, zone.getValues());
      zone.end = pos - 1;

      return result;
    }

    /**
     * Add an entry.
     * @param interval interval of the entry
     * @param value value to add
     */
    public void addEntry(final GenomicInterval interval, final T value) {

      if (interval == null) {
        throw new NullPointerException("interval argument cannot be null");
      }

      if (value == null) {
        throw new NullPointerException("value argument cannot be null");
      }

      final int intervalStart = interval.getStart();
      final int intervalEnd = interval.getEnd();

      // Create an empty zone if the interval is after the end of the
      // last chromosome zone
      if (intervalEnd > this.length) {
        add(new Zone<>(this.length + 1, intervalEnd, interval.getStrand()));
        this.length = intervalEnd;
      }

      final int indexStart = findIndexPos(intervalStart);
      final int indexEnd = findIndexPos(intervalEnd);

      final Zone<T> z1 = get(indexStart);
      final Zone<T> z1b;
      final int count1b;

      if (z1.start == intervalStart) {
        z1b = z1;
        count1b = 0;
      } else {
        z1b = splitZone(z1, intervalStart);
        count1b = 1;
      }

      // Same index
      if (indexStart == indexEnd) {

        if (z1b.end == intervalEnd) {
          z1b.addExon(value);
        } else {

          final Zone<T> z1c = splitZone(z1b, intervalEnd + 1);
          add(indexStart + 1, z1c);
        }

        if (z1 != z1b) {
          z1b.addExon(value);
          add(indexStart + 1, z1b);

        } else {
          z1.addExon(value);
        }

      } else {

        final Zone<T> z2 = get(indexEnd);
        final Zone<T> z2b;

        if (z2.end != intervalEnd) {
          z2b = splitZone(z2, intervalEnd + 1);
        } else {
          z2b = z2;
        }

        if (z1 != z1b) {
          add(indexStart + 1, z1b);
        }

        if (z2 != z2b) {
          add(indexEnd + 1 + count1b, z2b);
        }

        for (int i = indexStart + count1b; i <= indexEnd + count1b; i++) {
          get(i).addExon(value);
        }
      }
    }

    /**
     * Get entries.
     * @param start start of the interval
     * @param stop end of the interval
     * @return a map with the values
     */
    public Map<GenomicInterval, Set<T>> getEntries(final int start,
        final int stop) {

      final int indexStart = findIndexPos(start);
      final int indexEnd = findIndexPos(stop);

      if (indexStart == -1) {
        return null;
      }

      final int from = indexStart;
      final int to = indexEnd == -1 ? this.zones.size() - 1 : indexEnd;

      Map<GenomicInterval, Set<T>> result = null;

      for (int i = from; i <= to; i++) {

        final Zone<T> zone = get(i);

        // Really needed ?
        if (intersect(start, stop, zone.start, zone.end)) {

          final GenomicInterval iv = new GenomicInterval(this.chromosomeName,
              zone.start, zone.end, zone.strand);

          final Set<T> r = zone.getValues();

          if (result == null) {
            result = new HashMap<>();
          }

          if (r != null) {
            result.put(iv, Collections.unmodifiableSet(r));
          } else {
            result.put(iv, new HashSet<>());
          }
        }
      }

      if (stop > get(to).end && start > get(to).start) {
        result.put(new GenomicInterval(this.chromosomeName, start, stop,
            get(to).strand), new HashSet<>());
      } else if (stop > get(to).end) {
        result.put(new GenomicInterval(this.chromosomeName, get(to).end + 1,
            stop, get(to).strand), new HashSet<>());
      }

      return result;
    }

    /**
     * Test if an interval intersect a zone.
     * @param start start of the interval
     * @param end end of the interval
     * @param startZone start of the zone
     * @param endZone end of the zone
     * @return true if the interval intersect a zone
     */
    private static boolean intersect(final int start, final int end,
        final int startZone, final int endZone) {

      return (start >= startZone && start <= endZone)
          || (end >= startZone && end <= endZone)
          || (start < startZone && end > endZone);
    }

    @Override
    public boolean equals(final Object o) {

      if (o == this) {
        return true;
      }

      if (!(o instanceof ChromosomeStrandedZones<?>)) {
        return false;
      }

      final ChromosomeStrandedZones<?> that = (ChromosomeStrandedZones<?>) o;

      return Utils.equal(this.chromosomeName, that.chromosomeName)
          && Utils.equal(this.length, that.length)
          && Utils.equal(this.zones, that.zones);
    }

    @Override
    public int hashCode() {

      return Utils.hashCode(this.chromosomeName, this.length, this.zones);
    }

    @Override
    public String toString() {

      return this.getClass().getSimpleName()
          + "{chromosomeName=" + this.chromosomeName + ", length=" + this.length
          + ", zones=" + this.zones + "}";
    }

    //
    // Constructor
    //

    /**
     * Public constructor.
     * @param chromosomeName name of the chromosome
     */
    public ChromosomeStrandedZones(final String chromosomeName) {

      if (chromosomeName == null) {
        throw new NullPointerException(
            "chromosomeName argument cannot be null");
      }

      this.chromosomeName = chromosomeName;
    }
  }

  /**
   * This class define an object that contains all the zones of a chromosome.
   * These zones are stranded if "yes" or "reverse".
   * @author Claire Wallon
   */
  private static final class ChromosomeZones<T> implements Serializable {

    private static final long serialVersionUID = -6312870823086177216L;

    private final ChromosomeStrandedZones<T> plus;
    private final ChromosomeStrandedZones<T> minus;

    /**
     * Add a stranded entry.
     * @param interval interval of the entry
     * @param value value to add
     */
    public void addEntry(final GenomicInterval interval, final T value) {

      if (interval == null) {
        throw new NullPointerException("interval argument cannot be null");
      }

      if (value == null) {
        throw new NullPointerException("value argument cannot be null");
      }

      if (interval.getStrand() == '+' || interval.getStrand() == '.') {
        this.plus.addEntry(interval, value);
      } else if (interval.getStrand() == '-') {
        this.minus.addEntry(interval, value);
      }
    }

    /**
     * Get stranded entries.
     * @param start start of the interval
     * @param stop end of the interval
     * @return a map with the values
     */
    public Map<GenomicInterval, Set<T>> getEntries(final int start,
        final int stop) {

      final Map<GenomicInterval, Set<T>> result = new HashMap<>();

      final Map<GenomicInterval, Set<T>> interPlus =
          this.plus.getEntries(start, stop);

      if (interPlus != null) {
        result.putAll(interPlus);
      }

      final Map<GenomicInterval, Set<T>> interMinus =
          this.minus.getEntries(start, stop);

      if (interMinus != null) {
        result.putAll(interMinus);
      }

      return result;
    }

    @Override
    public boolean equals(final Object o) {

      if (o == this) {
        return true;
      }

      if (!(o instanceof ChromosomeZones<?>)) {
        return false;
      }

      final ChromosomeZones<?> that = (ChromosomeZones<?>) o;

      return Utils.equal(this.minus, that.minus)
          && Utils.equal(this.plus, that.plus);
    }

    @Override
    public int hashCode() {

      return Utils.hashCode(this.minus, this.plus);
    }

    @Override
    public String toString() {

      return this.getClass().getSimpleName()
          + "{minus=" + this.minus + ", plus=" + this.plus + "}";
    }

    //
    // Constructor
    //

    /**
     * Public constructor.
     * @param chromosomeName name of the chromosome
     */
    public ChromosomeZones(final String chromosomeName) {

      if (chromosomeName == null) {
        throw new NullPointerException(
            "chromosomeName argument cannot be null");
      }

      this.plus = new ChromosomeStrandedZones<>(chromosomeName);
      this.minus = new ChromosomeStrandedZones<>(chromosomeName);
    }
  }

  /**
   * Add an entry on the genomic array.
   * @param interval genomic interval
   * @param value value to add
   */
  public void addEntry(final GenomicInterval interval, final T value) {

    if (interval == null) {
      throw new NullPointerException("interval argument cannot be null");
    }

    if (value == null) {
      throw new NullPointerException("value argument cannot be null");
    }

    final String chromosomeName = interval.getChromosome();

    // Create a ChromosomeZones if it does not exist yet
    if (!this.chromosomes.containsKey(chromosomeName)) {
      addChromosome(chromosomeName);
    }

    // Add the GenomicInterval to the ChromosomeZones
    this.chromosomes.get(chromosomeName).addEntry(interval, value);
  }

  /**
   * Add a chromosome.
   * @param chromosomeName name of the chromosome to add
   */
  public void addChromosome(final String chromosomeName) {

    if (chromosomeName == null) {
      throw new NullPointerException("chromosomeName argument cannot be null");
    }

    if (containsChromosome(chromosomeName)) {
      return;
    }

    this.chromosomes.put(chromosomeName, new ChromosomeZones<>(chromosomeName));
  }

  /**
   * Add chromosomes from the list of sequence in a GenomeDescription object.
   * @param gd genome description
   */
  public void addChromosomes(final GenomeDescription gd) {

    if (gd == null) {
      throw new NullPointerException("gd argument cannot be null");
    }

    for (String chromosomeName : gd.getSequencesNames()) {
      addChromosome(chromosomeName);
    }
  }

  /**
   * Get entries in an interval.
   * @param interval the genomic interval
   * @return a map with the values
   */
  public Map<GenomicInterval, Set<T>> getEntries(
      final GenomicInterval interval) {

    if (interval == null) {
      throw new NullPointerException("interval argument cannot be null");
    }

    return getEntries(interval.getChromosome(), interval.getStart(),
        interval.getEnd());
  }

  /**
   * Get entries in an interval
   * @param chromosome chromosome of the interval
   * @param start start of the interval
   * @param end end of the interval
   * @return a map with the values
   */
  public Map<GenomicInterval, Set<T>> getEntries(final String chromosome,
      final int start, final int end) {

    if (chromosome == null) {
      throw new NullPointerException("chromosome argument cannot be null");
    }

    final ChromosomeZones<T> chr = this.chromosomes.get(chromosome);

    if (chr == null) {
      return null;
    }

    return chr.getEntries(start, end);
  }

  /**
   * Test if the GenomicArray contains a chromosome.
   * @param chromosomeName name of the chromosome to test
   * @return true if the GenomicArray contains the chromosome
   */
  public boolean containsChromosome(final String chromosomeName) {

    if (chromosomeName == null) {
      return false;
    }

    return this.chromosomes.containsKey(chromosomeName);
  }

  /**
   * Get a set with zone identifiers.
   * @return a set of strings with identifiers
   */
  public Set<String> getFeaturesIds() {

    Set<String> results = new TreeSet<>();

    for (Map.Entry<String, ChromosomeZones<T>> strandedZone : this.chromosomes
        .entrySet()) {

      // Process plus zones
      for (Zone<T> zone : strandedZone.getValue().plus.zones) {
        if (zone.valueCount != 0) {
          for (T value : zone.getValues()) {
            results.add(String.valueOf(value));
          }
        }
      }

      // Process minus zones
      for (Zone<T> zone : strandedZone.getValue().minus.zones) {
        if (zone.valueCount != 0) {
          for (T value : zone.getValues()) {
            results.add(String.valueOf(value));
          }
        }
      }

    }

    return results;
  }

  /**
   * Get the names of the chromosomes that contains the GenomicArray.
   * @return a set with the name of the chromosomes
   */
  public Set<String> getChromosomesNames() {

    return Collections.unmodifiableSet(this.chromosomes.keySet());
  }

  //
  // Other
  //

  public void clear() {

    this.chromosomes.clear();

  }

  //
  // Object methods
  //

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof GenomicArray)) {
      return false;
    }

    final GenomicArray<?> that = (GenomicArray<?>) o;

    return Utils.equal(this.chromosomes, that.chromosomes);
  }

  @Override
  public int hashCode() {

    return Utils.hashCode(this.chromosomes);
  }

  @Override
  public String toString() {

    return this.getClass().getSimpleName()
        + "{chromosomes=" + this.chromosomes + "}";
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   */
  public GenomicArray() {
  }

  /**
   * Public constructor.
   * @param gd The genome description.
   */
  public GenomicArray(final GenomeDescription gd) {

    this();
    addChromosomes(gd);
  }

}
