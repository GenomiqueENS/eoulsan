package fr.ens.biologie.genomique.eoulsan.bio;

import java.util.Iterator;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;

/**
 * This class allow the easily get fields of Nanopore reads ids.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class NanoporeReadId {

  public enum SequenceType {
    TEMPLATE, COMPLEMENT, CONSENSUS
  }

  private final Splitter spaceSplitter = Splitter.on(' ').omitEmptyStrings();
  private final Splitter equalsSplitter = Splitter.on('=');

  private String readId;
  private String runId;
  private int readNumber;
  private int channel;
  private String startTime;
  private String barcode;

  //
  // Getters
  //

  /**
   * Get the read id.
   * @return the read id
   */
  public String getReadId() {
    return this.readId;
  }

  public SequenceType getSequenceType() {

    if (this.readId == null) {
      return null;
    }

    if (this.readId.endsWith("_t")) {
      return SequenceType.TEMPLATE;
    }

    if (this.readId.endsWith("_c")) {
      return SequenceType.COMPLEMENT;
    }

    if (this.readId.indexOf('_') == -1) {
      return SequenceType.CONSENSUS;
    }

    return null;
  }

  /**
   * Get the run id.
   * @return the run id
   */
  public String getRunId() {
    return this.runId;
  }

  /**
   * Get the read number.
   * @return the read
   */
  public int getReadNumber() {
    return this.readNumber;
  }

  /**
   * Get the channel of the read.
   * @return the channel of the read
   */
  public int getChannel() {
    return this.channel;
  }

  /**
   * Get the start time.
   * @return the start time
   */
  public String getStartTime() {
    return this.startTime;
  }

  /**
   * Get the barcode
   * @return the barcode
   */
  public String getBarcode() {
    return this.barcode;
  }

  /**
   * Test if the run is barcoded.
   * @return true if the run is barcoded
   */
  public boolean isBarcoded() {

    return this.barcode != null;
  }

  //
  // Parsing
  //

  /**
   * Parse a Nanopore id string.
   * @param readId String with the Nanoore id
   */
  public void parse(String readId) {

    if (readId == null) {
      throw new NullPointerException("The string to parse is null");
    }

    // clean values
    this.readId = null;
    this.runId = null;
    this.readNumber = -1;
    this.channel = -1;
    this.startTime = null;
    this.barcode = null;

    boolean first = true;
    for (String s : this.spaceSplitter.split(readId)) {

      if (first) {

        this.readId = s;
        first = false;
      } else {

        Iterator<String> it = this.equalsSplitter.split(s).iterator();

        if (!it.hasNext()) {
          continue;
        }

        String key = it.next();

        if (!it.hasNext()) {
          continue;
        }

        String value = it.next();

        switch (key) {
        case "runid":
          this.runId = value;
          break;

        case "read":
          this.readNumber = Integer.parseInt(value);
          break;

        case "ch":
          this.channel = Integer.parseInt(value);
          break;

        case "start_time":
          this.startTime = value;
          break;

        case "barcode":
          this.barcode = value;
          break;

        default:
          break;
        }

      }

    }

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param readId String with Nanopore id to parse
   * @throws EoulsanException if the id is not an Nanopore id
   */
  public NanoporeReadId(final String readId) throws EoulsanException {

    if (readId == null) {
      throw new NullPointerException("The string to parse is null");
    }

    parse(readId);
  }

  /**
   * Public constructor.
   * @param sequence sequence witch name must be parsed
   * @throws EoulsanException if the id is not an Illumina id
   */
  public NanoporeReadId(final Sequence sequence) throws EoulsanException {

    if (sequence == null) {
      throw new NullPointerException("The sequence is null");
    }

    final String readId = sequence.getName();

    if (readId == null) {
      throw new NullPointerException("The string to parse is null");
    }

    parse(readId);
  }

}
