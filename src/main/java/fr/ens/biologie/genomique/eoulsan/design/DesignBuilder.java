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

package fr.ens.biologie.genomique.eoulsan.design;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ADDITIONAL_ANNOTATION_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATION_GFF;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATION_GTF;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_FASTA;
import static java.util.regex.Pattern.compile;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.core.Naming;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFileMetadata;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.bio.BadBioEntryException;
import fr.ens.biologie.genomique.kenetre.bio.FastqFormat;
import fr.ens.biologie.genomique.kenetre.bio.IlluminaReadId;
import fr.ens.biologie.genomique.kenetre.bio.io.FastqReader;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.kenetre.illumina.samplesheet.io.SampleSheetCSVReader;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;

/**
 * This class allow to easily build Design object from files paths.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DesignBuilder {

  private static final int MAX_FASTQ_ENTRIES_TO_READ = 10000;
  private static final Pattern ILLUMINA_FASTQ_FILENAME_PATTERN =
      compile("^(.+)_\\w+_L\\d\\d\\d_R\\d_\\d\\d\\d$");

  private final DataFormatRegistry dfr = DataFormatRegistry.getInstance();
  private final Map<String, List<FastqEntry>> fastqMap = new LinkedHashMap<>();
  private final Map<String, String> prefixMap = new HashMap<>();
  private DataFile genomeFile;
  private DataFile gffFile;
  private DataFile gtfFile;
  private DataFile additionalAnnotationFile;

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
    private final String sampleId;
    private final String sampleName;
    private final String sampleDesc;
    private final String sampleOperator;
    private final String sampleDate;
    private final String firstReadId;
    private final String prefix;
    private final int pairMember;

    private static String getDate(final DataFile file) {

      try {
        long last = file.getMetaData().getLastModified();

        return Instant.ofEpochMilli(last).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern(DATE_FORMAT));

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
          throw new EmptyFastqException(
              "Fastq file is empty: " + f.getSource());
        }

        reader.close();
        reader.throwException();

        return reader.next().getName();
      } catch (IOException | BadBioEntryException e) {
        throw new EoulsanException(e);
      }

    }

    private Object[] initPairedEnd() {

      String prefix = this.firstReadId;
      int pairMember = -1;

      try {
        IlluminaReadId irid = new IlluminaReadId(this.firstReadId);
        prefix = irid.getInstrumentId()
            + "\t" + irid.getFlowCellLane() + "\t"
            + irid.getTileNumberInFlowCellLane() + "\t"
            + irid.getXClusterCoordinateInTile() + "\t"
            + irid.getYClusterCoordinateInTile();

        pairMember = irid.getPairMember();

      } catch (KenetreException e) {

        if (this.firstReadId.endsWith("/1")) {
          prefix = this.firstReadId.substring(0, this.firstReadId.length() - 3);
          pairMember = 1;
        } else if (this.firstReadId.endsWith("/2")) {
          prefix = this.firstReadId.substring(0, this.firstReadId.length() - 3);
          pairMember = 2;
        } else {
          pairMember = 1;
        }
      }

      return new Object[] {prefix, pairMember};
    }

    //
    // Object methods
    //

    @Override
    public boolean equals(final Object obj) {

      if (obj == this) {
        return true;
      }

      if (!(obj instanceof FastqEntry)) {
        return false;
      }

      final FastqEntry that = (FastqEntry) obj;

      return this.path.equals(that.path);
    }

    @Override
    public int hashCode() {

      return this.path.hashCode();
    }

    @Override
    public String toString() {

      final StringBuilder sb = new StringBuilder();

      sb.append("FastqEntry(Sample: ");
      sb.append(this.sampleId);

      if (this.sampleDesc != null) {
        sb.append(", Description: ");
        sb.append(this.sampleDesc);
      }

      if (this.sampleOperator != null) {
        sb.append(", Operator: ");
        sb.append(this.sampleOperator);
      }

      sb.append(", Path: ");
      sb.append(this.path);
      sb.append(")");

      return sb.toString();
    }

    private static String defineSampleName(DataFile path) {

      String basename = StringUtils.basename(path.getName());

      // Check if filename is a Bcl2fastq output file
      final Matcher matcher = ILLUMINA_FASTQ_FILENAME_PATTERN.matcher(basename);

      if (matcher.matches()) {
        basename = matcher.group(1);
      }

      return basename;
    }

    //
    // Constructors
    //

    public FastqEntry(final DataFile path) throws EoulsanException {

      this.path = path;
      this.sampleName = defineSampleName(path);
      this.sampleId = Naming.toValidName(this.sampleName);
      this.sampleDesc = null;
      this.sampleOperator = null;
      this.sampleDate = getDate(path);
      this.firstReadId = getFirstReadSeqId(path);
      final Object[] array = initPairedEnd();
      this.prefix = (String) array[0];
      this.pairMember = (Integer) array[1];
    }

    public FastqEntry(final DataFile path, final String sampleId,
        final String sampleName, final String sampleDesc,
        final String sampleOperator) throws EoulsanException {

      this.path = path;
      this.sampleId = sampleId;
      this.sampleName = sampleName;
      this.sampleDesc = sampleDesc;
      this.sampleOperator = sampleOperator;
      this.sampleDate = getDate(path);
      this.firstReadId = getFirstReadSeqId(path);
      final Object[] array = initPairedEnd();
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

    if (file == null) {
      return;
    }

    if (!file.exists()) {
      throw new EoulsanException(
          "File " + file + " does not exist or is not a regular file.");
    }

    final String extension =
        StringUtils.extensionWithoutCompressionExtension(file.getName());

    DataFileMetadata md = null;

    try {
      md = file.getMetaData();
    } catch (IOException e) {
      // Do nothing if metadata cannot be retrieved
    }

    if (isDataFormatExtension(DataFormats.READS_FASTQ, extension, md)) {

      final FastqEntry entry;

      try {
        entry = new FastqEntry(file);
      } catch (EmptyFastqException e) {
        getLogger().warning(e.getMessage());
        return;
      }

      final String sampleId;

      if (this.prefixMap.containsKey(entry.prefix)) {
        sampleId = this.prefixMap.get(entry.prefix);
      } else {
        sampleId = entry.sampleId;
        this.prefixMap.put(entry.prefix, sampleId);
      }

      final List<FastqEntry> sampleEntries;

      if (!this.fastqMap.containsKey(sampleId)) {
        sampleEntries = new ArrayList<>();
        this.fastqMap.put(sampleId, sampleEntries);
      } else {
        sampleEntries = this.fastqMap.get(sampleId);
      }

      // Don't add previously added file
      if (!sampleEntries.contains(entry)) {
        sampleEntries.add(entry);
      }

    } else if (isDataFormatExtension(GENOME_FASTA, extension, md)) {
      this.genomeFile = file;
    } else if (isDataFormatExtension(ANNOTATION_GFF, extension, md)) {
      this.gffFile = file;
    } else if (isDataFormatExtension(ANNOTATION_GTF, extension, md)) {
      this.gtfFile = file;
    } else if (isDataFormatExtension(ADDITIONAL_ANNOTATION_TSV, extension,
        md)) {
      this.additionalAnnotationFile = file;
    } else {
      throw new EoulsanException("Unknown file type: " + file);
    }

  }

  /**
   * Add a filename to the design builder
   * @param filename filename of the file to add
   * @throws EoulsanException if the file does not exists
   */
  public void addFile(final String filename) throws EoulsanException {

    if (filename == null) {
      return;
    }

    getLogger().info("Add file " + filename + " to design.");
    addFile(new DataFile(filename));
  }

  /**
   * Add filenames to the design builder
   * @param filenames array with the filenames to add
   * @throws EoulsanException if the file does not exists
   */
  public void addFiles(final String[] filenames) throws EoulsanException {

    if (filenames == null) {
      return;
    }

    for (String filename : filenames) {
      addFile(filename);
    }
  }

  /**
   * Add filenames to the design builder
   * @param filenames array with the filenames to add
   * @throws EoulsanException if the file does not exists
   */
  public void addFiles(final List<String> filenames) throws EoulsanException {

    if (filenames == null) {
      return;
    }

    for (String filename : filenames) {
      addFile(filename);
    }
  }

  /**
   * Add all the sample from a Bclfastq samplesheet.
   * @param samplesheet The Bcl2fastq samplesheet object
   * @param projectName name of the project
   * @param bcl2fastqOutputDir the output directory of Bcl2fastq demultiplexing
   * @throws EoulsanException if an error occurs while adding the Bcl2fastq
   *           samplesheet
   */
  public void addBcl2FastqSamplesheetProject(final SampleSheet samplesheet,
      final String projectName, final File bcl2fastqOutputDir)
      throws EoulsanException {

    addBcl2FastqSamplesheetProject(samplesheet, projectName,
        bcl2fastqOutputDir.toPath());
  }

  /**
   * Add all the sample from a Bclfastq samplesheet.
   * @param samplesheet The Bcl2fastq samplesheet object
   * @param projectName name of the project
   * @param bcl2fastqOutputDir the output directory of Bcl2fastq demultiplexing
   * @throws EoulsanException if an error occurs while adding the Bcl2fastq
   *           samplesheet
   */
  public void addBcl2FastqSamplesheetProject(final SampleSheet samplesheet,
      final String projectName, final Path bcl2fastqOutputDir)
      throws EoulsanException {

    if (samplesheet == null || bcl2fastqOutputDir == null) {
      return;
    }

    if (!Files.exists(bcl2fastqOutputDir) || !Files.isDirectory(bcl2fastqOutputDir)) {
      throw new EoulsanException(
          "The Bcl2fastq output directory does not exists: "
              + bcl2fastqOutputDir);
    }

    final boolean Bcl2Fastq1 = Files.isDirectory(
        Path.of(bcl2fastqOutputDir.toString() + "/Project_" + projectName));

    for (fr.ens.biologie.genomique.kenetre.illumina.samplesheet.Sample sample : samplesheet) {

      final String sampleProject = sample.getSampleProject();
      final String sampleId = sample.getSampleId();
      final String sampleName = sample.getSampleName();
      final String sampleDesc = sample.getDescription();
      final String sampleOperator = sample.get("Operator");
      final int sampleLane = sample.getLane();

      // Check if sample id field exist for sample
      if (sampleId == null) {
        throw new EoulsanException(
            "No sample Id field found for sample: " + sample);
      }

      final String samplePrefix = sampleName == null ? sampleId : sampleName;

      // Select only project samples
      if (projectName != null && !projectName.equals(sampleProject)) {
        continue;
      }
      Path dataDir;
      if (Bcl2Fastq1) {
        dataDir = Path.of(bcl2fastqOutputDir.toString()
            + "/Project_" + sampleProject + "/Sample_" + sampleId);
      } else {

        dataDir = bcl2fastqOutputDir.resolve(sampleProject);

        // Check if a sample sub directory may exist
        String subdir = defineSampleSubDirName(sampleId, sampleName);

        if (!"".equals(subdir)) {
          dataDir = dataDir.resolve(subdir);
        }
      }
      // Test if the directory with fastq files exists
      if (!Files.exists(dataDir) || !Files.isDirectory(dataDir)) {
        continue;
      }

      final String laneKey =
          sampleLane == -1 ? "_L" : String.format("_L%03d_", sampleLane);

      // List the input FASTQ files
      final File[] files = dataDir.toFile().listFiles(f -> {

        final String filename =
            StringUtils.filenameWithoutCompressionExtension(f.getName());

        if ((filename.endsWith(".fastq") || filename.endsWith(".fq"))
            && filename.contains(laneKey)
            && samplePrefix.equals(parseSampleNameFromFilename(filename))) {
          return true;
        }

        return false;
      });

      // Sort the list of input FASTQ files
      Arrays.sort(files);

      for (File fastqFile : files) {

        final List<FastqEntry> list;
        final String normalizedSampleId = Naming.toValidName(sampleId);

        if (this.fastqMap.containsKey(normalizedSampleId)) {
          list = this.fastqMap.get(normalizedSampleId);
        } else {
          list = new ArrayList<>();
          this.fastqMap.put(normalizedSampleId, list);
        }

        try {
          list.add(new FastqEntry(new DataFile(fastqFile), normalizedSampleId,
              samplePrefix, sampleDesc, sampleOperator));
        } catch (EmptyFastqException e) {
          getLogger().warning(e.getMessage());
        }
      }
    }

  }

  /**
   * Add all the samples from a Bcl2Fastq samplesheet.
   * @param samplesheetFile the path to the Casava design
   * @param projectName the name of the project
   * @throws EoulsanException if an error occurs while reading the Casava design
   */
  public void addBcl2FastqSamplesheetProject(final File samplesheetFile,
      final String projectName) throws EoulsanException {

    addBcl2FastqSamplesheetProject(samplesheetFile.toPath(), projectName);
  }

  /**
   * Add all the samples from a Bcl2Fastq samplesheet.
   * @param samplesheetFile the path to the Casava design
   * @param projectName the name of the project
   * @throws EoulsanException if an error occurs while reading the Casava design
   */
  public void addBcl2FastqSamplesheetProject(final Path samplesheetFile,
      final String projectName) throws EoulsanException {

    if (samplesheetFile == null) {
      return;
    }

    getLogger().info("Add Bcl2fastq samplesheet file "
        + samplesheetFile + " to design with " + (projectName == null
            ? "no project filter." : projectName + " project filter."));

    final Path baseDir;
    final Path file;

    if (!Files.exists(samplesheetFile)) {
      throw new EoulsanException(
          "The Bcl2fastq samplesheet file does not exists: " + samplesheetFile);
    }

    if (Files.isDirectory(samplesheetFile)) {
      baseDir = samplesheetFile;

      final File[] files = baseDir.toFile().listFiles((dir, filename) -> {
        if (filename.endsWith(".csv")) {
          return true;
        }
        return false;
      });

      if (files == null || files.length == 0) {
        throw new EoulsanException(
            "No Bcl2fastq samplesheet file found in directory: " + baseDir);
      }

      if (files.length > 1) {
        throw new EoulsanException(
            "More than one Bcl2fastq samplesheet found in directory: "
                + baseDir);
      }

      file = files[0].toPath();
    } else {
      baseDir = samplesheetFile.getParent();
      file = samplesheetFile;
    }

    try (SampleSheetCSVReader reader = new SampleSheetCSVReader(file)) {
      addBcl2FastqSamplesheetProject(reader.read(), projectName, baseDir);
    } catch (IOException e) {
      throw new EoulsanException(e);
    }
  }

  /**
   * Create design object.
   * @param pairedEndMode true if the paired end mode is enabled
   * @return a new Design object
   * @throws EoulsanException if an error occurs while analyzing input files
   */
  public Design getDesign(final boolean pairedEndMode) throws EoulsanException {

    final Design result = DesignFactory.createEmptyDesign();
    result.addExperiment("exp1");

    final FastqFormat defaultFastqFormat =
        EoulsanRuntime.getSettings().getDefaultFastqFormat();

    for (Map.Entry<String, List<FastqEntry>> e : this.fastqMap.entrySet()) {

      final String sampleId = e.getKey();
      final List<List<FastqEntry>> files = findPairedEndFiles(e.getValue());
      int count = 0;

      for (List<FastqEntry> fes : files) {

        final String sampleName = fes.get(0).sampleName;
        final String desc = fes.get(0).sampleDesc;
        final String date = fes.get(0).sampleDate;
        final String operator = fes.get(0).sampleOperator;
        final String condition = fes.get(0).sampleName;

        if (pairedEndMode) {

          final String finalSampleId = files.size() == 1
              ? sampleId : sampleId + StringUtils.toLetter(count);
          final String finalSampleName = files.size() == 1
              ? sampleName : sampleName + StringUtils.toLetter(count);

          // Convert the list of DataFiles to a list of filenames
          final List<String> filenames = new ArrayList<>();
          for (FastqEntry fe : fes) {
            filenames.add(fe.path.getSource());
          }

          addSample(result, finalSampleId, finalSampleName, desc, condition,
              date, operator, defaultFastqFormat, filenames, fes.get(0).path);
          count++;

        } else {

          for (FastqEntry fe : fes) {

            final String finalSampleId = e.getValue().size() == 1
                ? sampleId : sampleId + StringUtils.toLetter(count);
            final String finalSampleName = e.getValue().size() == 1
                ? sampleName : sampleName + StringUtils.toLetter(count);

            addSample(result, finalSampleId, finalSampleName, desc, condition,
                date, operator, defaultFastqFormat,
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
   * @param sampleId the id of the sample
   * @param sampleName the name of the sample
   * @param desc description of the sample
   * @param condition condition
   * @param date date of the sample
   * @param operator operator for the sample
   * @param defaultFastqFormat default fastq format
   * @param filenames list of the fastq files for the sample
   * @param fileToCheck DataFile of the file to use to check fastq format
   * @throws EoulsanException if an error occurs while adding the sample
   */
  private void addSample(final Design design, final String sampleId,
      final String sampleName, final String desc, final String condition,
      final String date, final String operator,
      final FastqFormat defaultFastqFormat, final List<String> filenames,
      final DataFile fileToCheck) throws EoulsanException {

    if (design == null) {
      return;
    }

    // Create the sample
    design.addSample(sampleId);
    final Sample s = design.getSample(sampleId);
    if (sampleName != null) {
      s.setName(sampleName);
    }

    final SampleMetadata smd = s.getMetadata();

    // Set the fastq file of the sample
    smd.setReads(filenames);

    // Set the description of the sample if exists
    if (desc != null) {
      smd.setDescription(desc);
    } else if (s.getMetadata().containsDescription()) {
      smd.setDescription("no description");
    }

    // Set the date of the sample if exists
    if (date != null) {
      smd.setDate(date);
    }

    // Set the operator of the sample if exists
    if (operator != null) {
      smd.setOperator(operator);
    } else if (s.getMetadata().containsOperator()) {
      smd.setOperator("unknown operator");
    }

    // Set the genome file if exists
    if (this.genomeFile != null) {
      design.getMetadata().setGenomeFile(this.genomeFile.toString());
    }

    // Set the GFF Annotation file
    if (this.gffFile != null) {
      design.getMetadata().setGffFile(this.gffFile.toString());
    }

    // Set the GTF Annotation file
    if (this.gtfFile != null) {
      design.getMetadata().setGtfFile(this.gtfFile.toString());
    }

    // Set additional annotation file
    if (this.additionalAnnotationFile != null) {
      design.getMetadata().setAdditionalAnnotationFile(
          this.additionalAnnotationFile.toString());
    }

    // Identify Fastq format
    FastqFormat format = null;

    try {
      getLogger().info("Check fastq format for " + fileToCheck);
      format = FastqFormat.identifyFormat(fileToCheck.open(),
          MAX_FASTQ_ENTRIES_TO_READ);
    } catch (IOException | BadBioEntryException e) {
      throw new EoulsanException(e);
    }
    smd.setFastqFormat(format == null ? defaultFastqFormat : format);

    // Set replicate technical group
    smd.setRepTechGroup(condition);

    // Set UUID for the sample
    smd.setUUID(UUID.randomUUID().toString());

    // Get the experiement sample of the unique experiment of the design
    final Experiment exp = design.getExperiments().get(0);
    final ExperimentSample es = exp.addSample(s);

    // Set the condition
    es.getMetadata().setCondition(condition);

    // Set the default reference
    es.getMetadata().setReference(false);
  }

  private boolean isDataFormatExtension(final DataFormat dataFormat,
      final String extension, final DataFileMetadata md) {

    if (md != null && md.getDataFormat() != null) {
      return dataFormat.equals(md.getDataFormat());
    }

    for (DataFormat df : this.dfr.getDataFormatsFromExtension(extension)) {

      if (df == dataFormat) {
        return true;
      }
    }

    return false;
  }

  /**
   * Group paired end files.
   * @return a list of 1-2 paired end files
   * @throws EoulsanException if an error occurs while getting the id of first
   *           read of the fastq files
   */
  private List<List<FastqEntry>> findPairedEndFiles(
      final List<FastqEntry> files) throws EoulsanException {

    final Map<String, List<FastqEntry>> mapPrefix = new HashMap<>();
    final Map<FastqEntry, Integer> mapPaired = new HashMap<>();
    final List<List<FastqEntry>> result = new ArrayList<>();

    for (FastqEntry fe : files) {

      mapPaired.put(fe, fe.pairMember);

      final List<FastqEntry> list;

      if (mapPrefix.containsKey(fe.prefix)) {
        list = mapPrefix.get(fe.prefix);
      } else {
        list = new ArrayList<>();
        mapPrefix.put(fe.prefix, list);
        result.add(list);
      }

      list.add(fe);
    }

    // Order the paired end files
    for (List<FastqEntry> list : result) {

      // Check invalid number of files
      if (list.size() > 2) {
        throw new EoulsanException(
            "Found more than 2 files for a sample in paired-end mode: " + list);
      }

      if (list.size() == 2) {

        final int member1 = mapPaired.get(list.get(0));
        final int member2 = mapPaired.get(list.get(1));

        if (member1 == member2) {
          throw new EoulsanException(
              "Found two files with the same pair member: " + list);
        }

        if (member1 < 1 || member1 > 2) {
          throw new EoulsanException(
              "Invalid pair member for file: " + list.get(0));
        }

        if (member2 < 1 || member2 > 2) {
          throw new EoulsanException(
              "Invalid pair member for file: " + list.get(1));
        }

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

  /**
   * Parse the sample name from its filename.
   * @param filename the filename to parse
   * @return the sample name
   */
  private static String parseSampleNameFromFilename(final String filename) {

    if (filename == null) {
      return null;
    }

    final List<String> list =
        new ArrayList<>(Arrays.asList(filename.split("_")));

    final int size = list.size();

    if (size < 5) {
      return null;
    }

    StringBuilder sb = new StringBuilder();
    boolean first = true;

    for (String field : list.subList(0, size - 4)) {

      if (first) {
        first = false;
      } else {
        sb.append('_');
      }

      sb.append(field);
    }

    return sb.toString();
  }

  /**
   * Get the sample sub directory.
   * @param sampleId sample identifier
   * @param sampleName sample name
   * @return the sample sub directory or an empty string
   */
  public static String defineSampleSubDirName(final String sampleId,
      final String sampleName) {

    if (sampleId != null
        && !"".equals(sampleId.trim()) && sampleName != null
        && !"".equals(sampleName.trim())) {
      return sampleId.trim();
    }

    return "";
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
