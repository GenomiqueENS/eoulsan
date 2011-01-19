/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.design;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class allow to easyly build Design object from files paths.
 * @author Laurent Jourdren
 */
public class DesignBuilder {

  private List<File> fastqList = new ArrayList<File>();
  private File genomeFile;
  private File gffFile;

  /**
   * Add a file to the design builder
   * @param file file to add
   */
  public void addFile(final File file) {

    if (file == null || !file.exists() || !file.isFile())
      return;

    if (file.getName().endsWith(DataFormats.READS_FASTQ.getDefaultExtention()))
      this.fastqList.add(file);
    else if (file.getName().endsWith(
        DataFormats.GENOME_FASTA.getDefaultExtention()))
      this.genomeFile = file;
    else if (file.getName().endsWith(
        DataFormats.ANNOTATION_GFF.getDefaultExtention()))
      this.gffFile = file;
  }

  /**
   * Add a filename to the design builder
   * @param filename filename of the file to add
   */
  public void addFile(final String filename) {

    if (filename == null)
      return;

    addFile(new File(filename));
  }

  /**
   * Create design object.
   * @return a new Design object
   */
  public Design getDesign() {

    final Design result = DesignFactory.createEmptyDesign();

    for (File fq : this.fastqList) {

      final String sampleName = StringUtils.basename(fq.getName());

      // Create the sample
      result.addSample(sampleName);
      final Sample s = result.getSample(sampleName);
      final SampleMetadata smd = s.getMetadata();

      // Set the fastq file of the sample
      s.setSource(fq.toString());

      // Set the genome file if exists
      if (this.genomeFile != null)
        smd.setGenome(this.genomeFile.toString());

      // Set the Annaotion file
      if (this.gffFile != null)
        smd.setAnnotation(this.gffFile.toString());

      smd.setCondition(sampleName);
      smd.setReplicatType("T");
      smd.setUUID(UUID.randomUUID().toString());

    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param filenames filenames to add
   */
  public DesignBuilder(final String[] filenames) {

    if (filenames == null)
      return;

    for (String filename : filenames)
      addFile(filename);
  }

}
