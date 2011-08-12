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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.IlluminaReadId;
import fr.ens.transcriptome.eoulsan.bio.io.FastqReader;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.data.DataType;
import fr.ens.transcriptome.eoulsan.data.DataTypes;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class allow to easyly build Design object from files paths.
 * @author Laurent Jourdren
 */
public class DesignBuilder {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final int MAX_FASTQ_ENTRIES_TO_READ = 10000;

  private DataFormatRegistry dfr = DataFormatRegistry.getInstance();
  private List<DataFile> fastqList = Lists.newArrayList();
  private DataFile genomeFile;
  private DataFile gffFile;

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

    if (isDataTypeExtension(DataTypes.READS, extension)) {

      // Don't add previously added file
      if (!this.fastqList.contains(file))
        this.fastqList.add(file);

    } else if (isDataTypeExtension(DataTypes.GENOME, extension))
      this.genomeFile = file;

    else if (isDataTypeExtension(DataTypes.ANNOTATION, extension))
      this.gffFile = file;

  }

  /**
   * Add a filename to the design builder
   * @param filename filename of the file to add
   * @throws EoulsanException if the file does not exists
   */
  public void addFile(final String filename) throws EoulsanException {

    if (filename == null)
      return;

    LOGGER.info("Add file " + filename + " to design.");
    addFile(new DataFile(filename));
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

    for (List<DataFile> fqs : findPairEndFiles(pairEndMode)) {

      // Convert the list of DataFiles to a list of filenames
      final List<String> filenames = Lists.newArrayList();
      for (DataFile f : fqs)
        filenames.add(f.getSource());

      final String sampleName = StringUtils.basename(fqs.get(0).getName());

      // Create the sample
      result.addSample(sampleName);
      final Sample s = result.getSample(sampleName);
      final SampleMetadata smd = s.getMetadata();

      // Set the fastq file of the sample

      smd.setReads(filenames);

      // Set the genome file if exists
      if (this.genomeFile != null)
        smd.setGenome(this.genomeFile.toString());

      // Set the Annotation file
      if (this.gffFile != null)
        smd.setAnnotation(this.gffFile.toString());

      // Identify Fastq format
      FastqFormat format = null;

      try {
        LOGGER.info("Check fastq format for " + fqs.get(0));
        format =
            FastqFormat.identifyFormat(fqs.get(0).open(),
                MAX_FASTQ_ENTRIES_TO_READ);
      } catch (IOException e) {
        throw new EoulsanException(e.getMessage());
      } catch (BadBioEntryException e) {
        throw new EoulsanException(e.getMessage());
      }

      smd.setFastqFormat(format == null ? defaultFastqFormat : format);
      smd.setCondition(sampleName);
      smd.setReplicatType("T");
      smd.setUUID(UUID.randomUUID().toString());
    }

    return result;
  }

  private boolean isDataTypeExtension(final DataType dataType,
      final String extension) {

    return dfr.getDataFormatFromExtension(dataType, extension) != null;
  }

  /**
   * Group pair end files.
   * @param pairEnd true if files must be grouped as pair end files
   * @return a list of 1-2 pair end files
   * @throws EoulsanException if an error occurs while getting the id of first
   *           read of the fastq files
   */
  private List<List<DataFile>> findPairEndFiles(final boolean pairEnd)
      throws EoulsanException {

    final Map<String, List<DataFile>> mapPrefix = Maps.newHashMap();
    final Map<DataFile, Integer> mapPair = Maps.newHashMap();
    final List<List<DataFile>> result = Lists.newArrayList();

    for (DataFile f : this.fastqList) {

      // If not pair-end mode don't check files
      if (!pairEnd) {

        result.add(Collections.singletonList(f));
        continue;
      }

      final String readId = getFirstReadSeqId(f);

      String prefix = readId;

      try {
        IlluminaReadId irid = new IlluminaReadId(readId);
        prefix =
            irid.getInstrumentId()
                + "\t" + irid.getFlowCellLane() + "\t"
                + irid.getTileNumberInFlowCellLane() + "\t"
                + irid.getXClusterCoordinateInTile() + "\t"
                + irid.getYClusterCoordinateInTile();

        mapPair.put(f, irid.getPairMember());

      } catch (EoulsanException e) {

        if (readId.endsWith("/1")) {
          prefix = readId.substring(0, readId.length() - 3);
          mapPair.put(f, 1);
        } else if (readId.endsWith("/2")) {
          prefix = readId.substring(0, readId.length() - 3);
          mapPair.put(f, 2);
        } else
          mapPair.put(f, 1);
      }

      final List<DataFile> list;

      if (mapPrefix.containsKey(prefix))
        list = mapPrefix.get(prefix);
      else {
        list = Lists.newArrayList();
        mapPrefix.put(prefix, list);
        result.add(list);
      }

      list.add(f);
    }

    // Order the pair end files
    for (List<DataFile> list : result) {

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

          final DataFile tmp = list.get(0);
          list.set(0, list.get(1));
          list.set(1, tmp);
        }
      }

    }

    return result;
  }

  /**
   * Get the identifier of the first read of a fastq file.
   * @param f the input file
   * @return the identifier of the first read of a fastq file as a string
   * @throws EoulsanException if an error occurs while reading the file or if
   *           the read format is invalid
   */
  private String getFirstReadSeqId(final DataFile f) throws EoulsanException {

    final FastqReader reader;
    try {
      reader = new FastqReader(f.open());

      if (!reader.readEntry())
        throw new EoulsanException("Fastq file is empty: " + f.getSource());

    } catch (IOException e) {
      throw new EoulsanException(e.getMessage());
    } catch (BadBioEntryException e) {
      throw new EoulsanException(e.getMessage());
    }

    return reader.getName();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param filenames filenames to add
   * @throws EoulsanException if a file to add to the design does not exist or
   *           is not handled
   */
  public DesignBuilder(final String[] filenames) throws EoulsanException {

    if (filenames == null)
      return;

    for (String filename : filenames)
      addFile(filename);
  }

}
