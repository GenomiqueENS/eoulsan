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

package fr.ens.transcriptome.eoulsan.design;

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.IlluminaReadId;
import fr.ens.transcriptome.eoulsan.bio.io.FastqReader;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFileMetadata;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.illumina.CasavaDesign;
import fr.ens.transcriptome.eoulsan.illumina.CasavaSample;
import fr.ens.transcriptome.eoulsan.illumina.io.CasavaDesignCSVReader;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class allow to easily build Design object from files paths.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DesignBuilder {

  private static final int MAX_FASTQ_ENTRIES_TO_READ = 10000;

  private final DataFormatRegistry dfr = DataFormatRegistry.getInstance();
  private final Map<String, List<FastqEntry>> fastqMap = Maps
      .newLinkedHashMap();
  private final Map<String, String> prefixMap = Maps.newHashMap();
  private DataFile genomeFile;
  private DataFile gffFile;

  /**
   * This class define a exception thrown when a fastq file is empty.
   * @author Laurent Jourdren
   */
  private static class EmptyFastqException extends EoulsanException {

    private static final long serialVersionUID = 5672764893232380662L;

    /**
     * Public constructor
     * @param msg exception message
     */
    public EmptyFastqException(final String msg) {

      super(msg);
    }

  }

  /**
   * This inner class define a fastq entry.
   * @author Laurent Jourdren
   */
  private static class FastqEntry {

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private final DataFile path;
    private final String sampleName;
    private final String sampleDesc;
    private final String sampleOperator;
    private final String sampleDate;
    private final String firstReadId;
    private final String prefix;
    private final int pairMember;

    private static final String getDate(DataFile file) {

      try {
        long last = file.getMetaData().getLastModified();

        return new SimpleDateFormat(DATE_FORMAT).format(new Date(last));

      } catch (IOException e) {
        return null;
      }

    }

    //
    // static methods
    //

    /**
     * Get the identifier of the first read of a fastq file.
     * @param f the input file
     * @return the identifier of the first read of a fastq file as a string
     * @throws EoulsanException if an error occurs while reading the file or if
     *           the read format is invalid
     */
    private static String getFirstReadSeqId(final DataFile f)
        throws EoulsanException {

      final FastqReader reader;
      try {
        reader = new FastqReader(f.open());

        if (!reader.hasNext()) {
          reader.close();
          reader.throwException();
          throw new EmptyFastqException("Fastq file is empty: " + f.getSource());
        }

        reader.close();
        reader.throwException();

        return reader.next().getName();
      } catch (IOException e) {
        throw new EoulsanException(e.getMessage());
      } catch (BadBioEntryException e) {
        throw new EoulsanException(e.getMessage());
      }

    }

    private Object[] initPairedend() {

      String prefix = this.firstReadId;
      int pairMember = -1;

      try {
        IlluminaReadId irid = new IlluminaReadId(firstReadId);
        prefix =
            irid.getInstrumentId()
                + "\t" + irid.getFlowCellLane() + "\t"
                + irid.getTileNumberInFlowCellLane() + "\t"
                + irid.getXClusterCoordinateInTile() + "\t"
                + irid.getYClusterCoordinateInTile();

        pairMember = irid.getPairMember();

      } catch (EoulsanException e) {

        if (firstReadId.endsWith("/1")) {
          prefix = firstReadId.substring(0, firstReadId.length() - 3);
          pairMember = 1;
        } else if (firstReadId.endsWith("/2")) {
          prefix = firstReadId.substring(0, firstReadId.length() - 3);
          pairMember = 2;
        } else
          pairMember = 1;
      }

      return new Object[] { prefix, pairMember };
    }

    //
    // Object methods
    //

    @Override
    public boolean equals(final Object obj) {

      if (obj == this)
        return true;

      if (!(obj instanceof FastqEntry))
        return false;

      final FastqEntry that = (FastqEntry) obj;

      return path.equals(that.path);
    }

    @Override
    public int hashCode() {

      return path.hashCode();
    }

    public String toString() {

      final StringBuilder sb = new StringBuilder();

      sb.append("FastqEntry(Sample: ");
      sb.append(sampleName);

      if (sampleDesc != null) {
        sb.append(", Description: ");
        sb.append(sampleDesc);
      }

      if (sampleOperator != null) {
        sb.append(", Operator: ");
        sb.append(sampleOperator);
      }

      sb.append(", Path: ");
      sb.append(path);
      sb.append(")");

      return sb.toString();
    }

    //
    // Constructors
    //

    public FastqEntry(final DataFile path) throws EoulsanException {

      this.path = path;
      this.sampleName = StringUtils.basename(path.getName());
      this.sampleDesc = null;
      this.sampleOperator = null;
      this.sampleDate = getDate(path);
      this.firstReadId = getFirstReadSeqId(path);
      final Object[] array = initPairedend();
      this.prefix = (String) array[0];
      this.pairMember = (Integer) array[1];
    }

    public FastqEntry(final DataFile path, final String sampleName,
        final String sampleDesc, final String sampleOperator)
        throws EoulsanException {

      this.path = path;
      this.sampleName = sampleName;
      this.sampleDesc = sampleDesc;
      this.sampleOperator = sampleOperator;
      this.sampleDate = getDate(path);
      this.firstReadId = getFirstReadSeqId(path);
      final Object[] array = initPairedend();
      this.prefix = (String) array[0];
      this.pairMember = (Integer) array[1];
    }

  }

  /**
   * Add a file to the design builder
   * @param file file to add
   * @throws EoulsanException if the file does not exist
   */
  public void addFile(final DataFile file) throws EoulsanException {

    if (file == null)
      return;

    if (!file.exists())
      throw new EoulsanException("File "
          + file + " does not exist or is not a regular file.");

    final String extension =
        StringUtils.extensionWithoutCompressionExtension(file.getName());

    DataFileMetadata md = null;

    try {
      md = file.getMetaData();
    } catch (IOException e) {
    }

    if (isDataFormatExtension(DataFormats.READS_FASTQ, extension, md)) {

      final FastqEntry entry;

      try {
        entry = new FastqEntry(file);
      } catch (EmptyFastqException e) {
        getLogger().warning(e.getMessage());
        return;
      }

      final String sampleName;

      if (this.prefixMap.containsKey(entry.prefix))
        sampleName = this.prefixMap.get(entry.prefix);
      else {
        sampleName = entry.sampleName;
        this.prefixMap.put(entry.prefix, sampleName);
      }

      final List<FastqEntry> sampleEntries;

      if (!this.fastqMap.containsKey(sampleName)) {
        sampleEntries = Lists.newArrayList();
        this.fastqMap.put(sampleName, sampleEntries);
      } else
        sampleEntries = this.fastqMap.get(sampleName);

      // Don't add previously added file
      if (!sampleEntries.contains(entry))
        sampleEntries.add(entry);

    } else if (isDataFormatExtension(DataFormats.GENOME_FASTA, extension, md)) {
      this.genomeFile = file;
    } else if (isDataFormatExtension(DataFormats.ANNOTATION_GFF, extension, md))
      this.gffFile = file;
    else
      throw new EoulsanException("Unknown file type: " + file);

  }

  /**
   * Add a filename to the design builder
   * @param filename filename of the file to add
   * @throws EoulsanException if the file does not exists
   */
  public void addFile(final String filename) throws EoulsanException {

    if (filename == null)
      return;

    getLogger().info("Add file " + filename + " to design.");
    addFile(new DataFile(filename));
  }

  /**
   * Add filenames to the design builder
   * @param filenames array with the filenames to add
   * @throws EoulsanException if the file does not exists
   */
  public void addFiles(final String[] filenames) throws EoulsanException {

    if (filenames == null)
      return;

    for (String filename : filenames)
      addFile(filename);
  }

  /**
   * Add filenames to the design builder
   * @param filenames array with the filenames to add
   * @throws EoulsanException if the file does not exists
   */
  public void addFiles(final List<String> filenames) throws EoulsanException {

    if (filenames == null)
      return;

    for (String filename : filenames)
      addFile(filename);
  }

  /**
   * Add all the sample from a Casava design.
   * @param casavaDesign The Casava design object
   * @param projectName name of the project
   * @param casavaOutputDir the output directory of Casava demultiplexing
   * @throws EoulsanException if an error occurs while adding the casava design
   */
  public void addCasavaDesignProject(final CasavaDesign casavaDesign,
      final String projectName, final File casavaOutputDir)
      throws EoulsanException {

    if (casavaDesign == null || casavaOutputDir == null)
      return;

    if (!casavaOutputDir.exists() || !casavaOutputDir.isDirectory())
      throw new EoulsanException(
          "The casava output directory does not exists: " + casavaOutputDir);

    for (CasavaSample sample : casavaDesign) {

      final String sampleProject = sample.getSampleProject();
      final String sampleName = sample.getSampleId();
      final String sampleDesc = sample.getDescription();
      final String sampleOperator = sample.getOperator();
      final int sampleLane = sample.getLane();

      // Select only project samples
      if (projectName != null && !projectName.equals(sampleProject))
        continue;

      final File dataDir =
          new File(casavaOutputDir.getPath()
              + "/Project_" + sampleProject + "/Sample_" + sampleName);

      // Test if the directory with fastq files exists
      if (!dataDir.exists() || !dataDir.isDirectory())
        continue;

      final String laneKey = String.format("_L%03d_", sampleLane);

      for (File fastqFile : dataDir.listFiles(new FileFilter() {

        @Override
        public boolean accept(File f) {

          final String filename =
              StringUtils.filenameWithoutCompressionExtension(f.getName());

          if (filename.startsWith(sampleName)
              && filename.contains(laneKey)
              && (filename.endsWith(".fastq") || filename.endsWith(".fq")))

            return true;

          return false;
        }
      })) {

        final List<FastqEntry> list;

        if (fastqMap.containsKey(sampleName))
          list = fastqMap.get(sampleName);
        else {
          list = new ArrayList<FastqEntry>();
          fastqMap.put(sampleName, list);
        }

        try {
          list.add(new FastqEntry(new DataFile(fastqFile), sampleName,
              sampleDesc, sampleOperator));
        } catch (EmptyFastqException e) {
          getLogger().warning(e.getMessage());
        }
      }
    }

  }

  /**
   * Add all the sample from a Casava design.
   * @param casavaDesignFile the path to the Casava design
   * @param projectName the name of the project
   * @throws EoulsanException if an error occurs while reading the Casava design
   */
  public void addCasavaDesignProject(final File casavaDesignFile,
      final String projectName) throws EoulsanException {

    if (casavaDesignFile == null)
      return;

    getLogger().info(
        "Add Casava design file "
            + casavaDesignFile
            + " to design with "
            + (projectName == null ? "no project filter." : projectName
                + " project filter."));

    final File baseDir;
    final File file;

    if (!casavaDesignFile.exists())
      throw new EoulsanException("The casava design file does not exists: "
          + casavaDesignFile);

    if (casavaDesignFile.isDirectory()) {
      baseDir = casavaDesignFile;

      final File[] files = baseDir.listFiles(new FilenameFilter() {

        @Override
        public boolean accept(File dir, String filename) {
          if (filename.endsWith(".csv"))
            return true;
          return false;
        }
      });

      if (files == null || files.length == 0)
        throw new EoulsanException("No Casava design file found in directory: "
            + baseDir);

      if (files.length > 1)
        throw new EoulsanException(
            "More than one Casava design file found in directory: " + baseDir);

      file = files[0];
    } else {
      baseDir = casavaDesignFile.getParentFile();
      file = casavaDesignFile;
    }

    try {
      CasavaDesignCSVReader reader = new CasavaDesignCSVReader(file);
      addCasavaDesignProject(reader.read(), projectName, baseDir);
    } catch (IOException e) {
      throw new EoulsanException(e.getMessage());
    }

  }

  /**
   * Create design object.
   * @param pairEndMode true if the pair end mode is enabled
   * @return a new Design object
   * @throws EoulsanException if an error occurs while analyzing input files
   */
  public Design getDesign(final boolean pairEndMode) throws EoulsanException {

    final Design result = DesignFactory.createEmptyDesign();
    final FastqFormat defaultFastqFormat =
        EoulsanRuntime.getSettings().getDefaultFastqFormat();

    for (Map.Entry<String, List<FastqEntry>> e : this.fastqMap.entrySet()) {

      final String sampleName = e.getKey();
      final List<List<FastqEntry>> files = findPairEndFiles(e.getValue());
      int count = 0;

      for (List<FastqEntry> fes : files) {

        final String desc = fes.get(0).sampleDesc;
        final String date = fes.get(0).sampleDate;
        final String operator = fes.get(0).sampleOperator;
        final String condition = sampleName;

        if (pairEndMode) {

          final String finalSampleName =
              files.size() == 1 ? sampleName : sampleName
                  + StringUtils.toLetter(count);

          // Convert the list of DataFiles to a list of filenames
          final List<String> filenames = Lists.newArrayList();
          for (FastqEntry fe : fes)
            filenames.add(fe.path.getSource());

          addSample(result, finalSampleName, desc, condition, date, operator,
              defaultFastqFormat, filenames, fes.get(0).path);
          count++;

        } else {

          for (FastqEntry fe : fes) {

            final String finalSampleName =
                e.getValue().size() == 1 ? sampleName : sampleName
                    + StringUtils.toLetter(count);

            addSample(result, finalSampleName, desc, condition, date, operator,
                defaultFastqFormat,
                Collections.singletonList(fe.path.getSource()), fe.path);
            count++;
          }

        }
      }

    }

    return result;
  }

  /**
   * Add a Sample to the Design object
   * @param design Design object
   * @param sampleName name of the sample
   * @param desc description of the sample
   * @param condition condition
   * @param date date of the sample
   * @param operator operator for the sample
   * @param defaultFastqFormat default fastq format
   * @param filenames list of the fastq files for the sample
   * @param fileToCheck DataFile of the file to use to check fastq format
   * @throws EoulsanException if an error occurs while adding the sample
   */
  private void addSample(final Design design, final String sampleName,
      final String desc, final String condition, final String date,
      final String operator, final FastqFormat defaultFastqFormat,
      final List<String> filenames, final DataFile fileToCheck)
      throws EoulsanException {

    if (design == null)
      return;

    // Create the sample
    design.addSample(sampleName);
    final Sample s = design.getSample(sampleName);
    final SampleMetadata smd = s.getMetadata();

    // Set the fastq file of the sample
    smd.setReads(filenames);

    // Set the description of the sample if exists
    if (desc != null)
      smd.setDescription(desc);
    else if (design.isMetadataField(SampleMetadata.DESCRIPTION_FIELD))
      smd.setDescription("no description");

    // Set the date of the sample if exists
    if (date != null)
      smd.setDate(date);

    // Set the operator of the sample if exists
    if (operator != null)
      smd.setOperator(operator);
    else if (design.isMetadataField(SampleMetadata.OPERATOR_FIELD))
      smd.setOperator("unknown operator");

    // Set the genome file if exists
    if (this.genomeFile != null)
      smd.setGenome(this.genomeFile.toString());

    // Set the Annotation file
    if (this.gffFile != null)
      smd.setAnnotation(this.gffFile.toString());

    // Identify Fastq format
    FastqFormat format = null;

    try {
      getLogger().info("Check fastq format for " + fileToCheck);
      format =
          FastqFormat.identifyFormat(fileToCheck.open(),
              MAX_FASTQ_ENTRIES_TO_READ);
    } catch (IOException e) {
      throw new EoulsanException(e.getMessage());
    } catch (BadBioEntryException e) {
      throw new EoulsanException(e.getMessage());
    }

    smd.setFastqFormat(format == null ? defaultFastqFormat : format);
    smd.setExperiment("exp1");
    smd.setCondition(condition);
    smd.setRepTechGroup(condition);
    smd.setReference(false);
    smd.setUUID(UUID.randomUUID().toString());

  }

  private boolean isDataFormatExtension(final DataFormat dataFormat,
      final String extension, DataFileMetadata md) {

    if (md != null && md.getDataFormat() != null) {
      return dataFormat.equals(md.getDataFormat());
    }

    return dfr.getDataFormatFromExtension(extension) == dataFormat;
  }

  /**
   * Group pair end files.
   * @return a list of 1-2 pair end files
   * @throws EoulsanException if an error occurs while getting the id of first
   *           read of the fastq files
   */
  private List<List<FastqEntry>> findPairEndFiles(final List<FastqEntry> files)
      throws EoulsanException {

    final Map<String, List<FastqEntry>> mapPrefix = Maps.newHashMap();
    final Map<FastqEntry, Integer> mapPair = Maps.newHashMap();
    final List<List<FastqEntry>> result = Lists.newArrayList();

    for (FastqEntry fe : files) {

      mapPair.put(fe, fe.pairMember);

      final List<FastqEntry> list;

      if (mapPrefix.containsKey(fe.prefix))
        list = mapPrefix.get(fe.prefix);
      else {
        list = Lists.newArrayList();
        mapPrefix.put(fe.prefix, list);
        result.add(list);
      }

      list.add(fe);
    }

    // Order the pair end files
    for (List<FastqEntry> list : result) {

      // Check invalid number of files
      if (list.size() > 2)
        throw new EoulsanException(
            "Found more than 2 files for a sample in pair-end mode: " + list);

      if (list.size() == 2) {

        final int member1 = mapPair.get(list.get(0));
        final int member2 = mapPair.get(list.get(1));

        if (member1 == member2)
          throw new EoulsanException(
              "Found two files with the same pair member: " + list);

        if (member1 < 1 || member1 > 2)
          throw new EoulsanException("Invalid pair member for file: "
              + list.get(0));

        if (member2 < 1 || member2 > 2)
          throw new EoulsanException("Invalid pair member for file: "
              + list.get(1));

        // Change the order of the file if necessary
        if (member1 == 2 && member2 == 1) {

          final FastqEntry tmp = list.get(0);
          list.set(0, list.get(1));
          list.set(1, tmp);
        }
      }

    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public DesignBuilder() {
  }

  /**
   * Public constructor.
   * @param filenames filenames to add
   * @throws EoulsanException if a file to add to the design does not exist or
   *           is not handled
   */
  public DesignBuilder(final String[] filenames) throws EoulsanException {

    addFiles(filenames);
  }

}
