/*
 * The MIT License
 *
 * Copyright (c) 2009 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.sf.samtools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;

/**
 * This allow to parse a entry of a SAM file. This code comes from the picard
 * project.
 * @see http://picard.sourceforge.net/
 */
@SuppressWarnings("all")
public class SAMParser {

  // From SAM specification
  private static final int QNAME_COL = 0;
  private static final int FLAG_COL = 1;
  private static final int RNAME_COL = 2;
  private static final int POS_COL = 3;
  private static final int MAPQ_COL = 4;
  private static final int CIGAR_COL = 5;
  private static final int MRNM_COL = 6;
  private static final int MPOS_COL = 7;
  private static final int ISIZE_COL = 8;
  private static final int SEQ_COL = 9;
  private static final int QUAL_COL = 10;

  private static final int NUM_REQUIRED_FIELDS = 11;

  // Read string must contain only these characters
  private static final Pattern VALID_BASES =
      Pattern.compile("^[acgtnACGTN.=]+$");

  private SAMFileReader.ValidationStringency validationStringency =
      SAMFileReader.ValidationStringency.DEFAULT_STRINGENCY;

  private final TextTagCodec tagCodec = new TextTagCodec();

  /**
   * Allocate this once rather than for every line as a performance
   * optimization. The size is arbitrary -- merely large enough to handle the
   * maximum number of fields we might expect from a reasonable SAM file.
   */
  private final String[] mFields = new String[10000];

  private SAMFileHeader mFileHeader = new SAMFileHeader();

  private String makeErrorString(final String reason) {

    return "Error parsing text SAM file. " + reason;
  }

  private RuntimeException reportFatalErrorParsingLine(final String reason) {
    return new SAMFormatException(makeErrorString(reason));
  }

  private void reportErrorParsingLine(final String reason) {
    final String errorMessage = makeErrorString(reason);

    if (validationStringency == SAMFileReader.ValidationStringency.STRICT) {
      throw new SAMFormatException(errorMessage);
    } else if (validationStringency == SAMFileReader.ValidationStringency.LENIENT) {
      System.err
          .println("Ignoring SAM validation error due to lenient parsing:");
      System.err.println(errorMessage);
    }
  }

  private void reportErrorParsingLine(final Exception e) {
    final String errorMessage = makeErrorString(e.getMessage());
    if (validationStringency == SAMFileReader.ValidationStringency.STRICT) {
      throw new SAMFormatException(errorMessage);
    } else if (validationStringency == SAMFileReader.ValidationStringency.LENIENT) {
      System.err
          .println("Ignoring SAM validation error due to lenient parsing:");
      System.err.println(errorMessage);
    }
  }

  int parseInt(final String s, final String fieldName) {
    final int ret;
    try {
      ret = Integer.parseInt(s);
    } catch (NumberFormatException e) {
      throw reportFatalErrorParsingLine("Non-numeric value in "
          + fieldName + " column");
    }
    return ret;
  }

  void validateReferenceName(final String rname, final String fieldName) {

    if (rname.equals("=")) {
      if (fieldName.equals("MRNM")) {
        return;
      }
      reportErrorParsingLine("= is not a valid value for "
          + fieldName + " field.");
    }
    // if (getFileHeader().getSequenceDictionary().size() != 0) {
    // if (getFileHeader().getSequence(rname) == null) {
    // reportErrorParsingLine(fieldName
    // + " '" + rname + "' not found in any SQ record");
    // }
    // }
  }

  /**
   * Split the string into tokens separated by the given delimiter. Profiling
   * has revealed that the standard string.split() method typically takes > 1/2
   * the total time when used for parsing ascii files. Note that if tokens arg
   * is not large enough to all the tokens in the string, excess tokens are
   * discarded.
   * @param aString the string to split
   * @param tokens an array to hold the parsed tokens
   * @param delim character that delimits tokens
   * @return the number of tokens parsed
   */
  public static int split(final String aString, final String[] tokens,
      final char delim) {

    final int maxTokens = tokens.length;
    int nTokens = 0;
    int start = 0;
    int end = aString.indexOf(delim);
    if (end < 0) {
      tokens[nTokens++] = aString;
      return nTokens;
    }
    while ((end > 0) && (nTokens < maxTokens)) {
      tokens[nTokens++] = aString.substring(start, end);
      start = end + 1;
      end = aString.indexOf(delim, start);

    }
    // Add the trailing string, if there is room and if it is not empty.
    if (nTokens < maxTokens) {
      final String trailingString = aString.substring(start);
      if (trailingString.length() > 0) {
        tokens[nTokens++] = trailingString;
      }
    }
    return nTokens;
  }

  public SAMRecord parseLine(final String mCurrentLine) {

    final int numFields = split(mCurrentLine, mFields, '\t');

    if (numFields < NUM_REQUIRED_FIELDS) {
      throw reportFatalErrorParsingLine("Not enough fields");
    }
    if (numFields == mFields.length) {
      reportErrorParsingLine("Too many fields in SAM text record.");
    }
    for (int i = 0; i < numFields; ++i) {
      if (mFields[i].length() == 0) {
        reportErrorParsingLine("Empty field at position " + i + " (zero-based)");
      }
    }
    final SAMRecord samRecord = new SAMRecord(mFileHeader);

    samRecord.setValidationStringency(this.validationStringency);

    samRecord.setReadName(mFields[QNAME_COL]);

    final int flags = parseInt(mFields[FLAG_COL], "FLAG");
    samRecord.setFlags(flags);

    String rname = mFields[RNAME_COL];
    if (!rname.equals("*")) {
      rname = SAMSequenceRecord.truncateSequenceName(rname);
      validateReferenceName(rname, "RNAME");
      samRecord.setReferenceName(rname);
    } else if (!samRecord.getReadUnmappedFlag()) {
      reportErrorParsingLine("RNAME is not specified but flags indicate mapped");
    }

    final int pos = parseInt(mFields[POS_COL], "POS");
    final int mapq = parseInt(mFields[MAPQ_COL], "MAPQ");
    final String cigar = mFields[CIGAR_COL];
    if (!SAMRecord.NO_ALIGNMENT_REFERENCE_NAME.equals(samRecord
        .getReferenceName())) {
      if (pos == 0) {
        reportErrorParsingLine("POS must be non-zero if RNAME is specified");
      }
      if (!samRecord.getReadUnmappedFlag() && cigar.equals("*")) {
        reportErrorParsingLine("CIGAR must not be '*' if RNAME is specified");
      }
    } else {
      if (pos != 0) {
        reportErrorParsingLine("POS must be zero if RNAME is not specified");
      }
      if (mapq != 0) {
        reportErrorParsingLine("MAPQ must be zero if RNAME is not specified");
      }
      if (!cigar.equals("*")) {
        reportErrorParsingLine("CIGAR must be '*' if RNAME is not specified");
      }
    }
    samRecord.setAlignmentStart(pos);
    samRecord.setMappingQuality(mapq);
    samRecord.setCigarString(cigar);

    String mateRName = mFields[MRNM_COL];
    if (mateRName.equals("*")) {
      if (samRecord.getReadPairedFlag() && !samRecord.getMateUnmappedFlag()) {
        reportErrorParsingLine("MRNM not specified but flags indicate mate mapped");
      }
    } else {
      if (!samRecord.getReadPairedFlag()) {
        reportErrorParsingLine("MRNM specified but flags indicate unpaired");
      }
      if (!"=".equals(mateRName)) {
        mateRName = SAMSequenceRecord.truncateSequenceName(mateRName);
      }
      validateReferenceName(mateRName, "MRNM");
      if (mateRName.equals("=")) {
        if (samRecord.getReferenceName() == null) {
          reportErrorParsingLine("MRNM is '=', but RNAME is not set");
        }
        samRecord.setMateReferenceName(samRecord.getReferenceName());
      } else {
        samRecord.setMateReferenceName(mateRName);
      }
    }

    final int matePos = parseInt(mFields[MPOS_COL], "MPOS");
    final int isize = parseInt(mFields[ISIZE_COL], "ISIZE");
    if (!samRecord.getMateReferenceName().equals(
        SAMRecord.NO_ALIGNMENT_REFERENCE_NAME)) {
      if (matePos == 0) {
        reportErrorParsingLine("MPOS must be non-zero if MRNM is specified");
      }
    } else {
      if (matePos != 0) {
        reportErrorParsingLine("MPOS must be zero if MRNM is not specified");
      }
      if (isize != 0) {
        reportErrorParsingLine("ISIZE must be zero if MRNM is not specified");
      }
    }
    samRecord.setMateAlignmentStart(matePos);
    samRecord.setInferredInsertSize(isize);
    if (!mFields[SEQ_COL].equals("*")) {
      validateReadBases(mFields[SEQ_COL]);
      samRecord.setReadString(mFields[SEQ_COL]);
    } else {
      samRecord.setReadBases(SAMRecord.NULL_SEQUENCE);
    }
    if (!mFields[QUAL_COL].equals("*")) {
      if (samRecord.getReadBases() == SAMRecord.NULL_SEQUENCE) {
        reportErrorParsingLine("QUAL should not be specified if SEQ is not specified");
      }
      if (samRecord.getReadString().length() != mFields[QUAL_COL].length()) {
        reportErrorParsingLine("length(QUAL) != length(SEQ)");
      }
      samRecord.setBaseQualityString(mFields[QUAL_COL]);
    } else {
      samRecord.setBaseQualities(SAMRecord.NULL_QUALS);
    }

    for (int i = NUM_REQUIRED_FIELDS; i < numFields; ++i) {
      parseTag(samRecord, mFields[i]);
    }

    final List<SAMValidationError> validationErrors = samRecord.isValid();
    if (validationErrors != null) {
      for (final SAMValidationError errorMessage : validationErrors) {
        reportErrorParsingLine(errorMessage.getMessage());
      }
    }
    return samRecord;
  }

  private void validateReadBases(final String bases) {
    if (!VALID_BASES.matcher(bases).matches()) {
      reportErrorParsingLine("Invalid character in read bases");
    }
  }

  private void parseTag(final SAMRecord samRecord, final String tag) {
    Map.Entry<String, Object> entry = null;
    try {
      entry = tagCodec.decode(tag);
    } catch (SAMFormatException e) {
      reportErrorParsingLine(e);
    }
    if (entry != null) {
      samRecord.setAttribute(entry.getKey(), entry.getValue());
    }
  }

  //
  // Code added by Laurent Jourdren
  //

  /**
   * Set the list of the chromosomes and the size of the chromosomes from a
   * GenomeDescription object.
   * @param genomeDescription The genome description
   */
  public void setGenomeDescription(final GenomeDescription genomeDescription) {

    if (genomeDescription == null) {

      return;
    }

    final List<SAMSequenceRecord> sequences =
        new ArrayList<SAMSequenceRecord>();

    for (String sequenceName : genomeDescription.getSequencesNames()) {

      final SAMSequenceRecord sequenceRecord =
          new SAMSequenceRecord(sequenceName, genomeDescription
              .getSequenceLength(sequenceName));
      sequences.add(sequenceRecord);
    }

    this.mFileHeader
        .setSequenceDictionary(new SAMSequenceDictionary(sequences));
  }

  /**
   * Get the SAMFileHeader object.
   * @return the SAMFileHeader object
   */
  public SAMFileHeader getFileHeader() {

    return this.mFileHeader;
  }

  /**
   * Public constructor.
   */
  public SAMParser() {

    final List<SAMSequenceRecord> sequences =
        new ArrayList<SAMSequenceRecord>();

    this.mFileHeader
        .setSequenceDictionary(new SAMSequenceDictionary(sequences));

  }

}
