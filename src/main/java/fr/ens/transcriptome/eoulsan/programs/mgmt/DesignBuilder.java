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

package fr.ens.transcriptome.eoulsan.programs.mgmt;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.DesignFactory;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.design.SampleMetadata;
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

    if (file.getName().endsWith(Common.FASTQ_EXTENSION))
      this.fastqList.add(file);
    else if (file.getName().endsWith(Common.FASTA_EXTENSION))
      this.genomeFile = file;
    else if (file.getName().endsWith(Common.GFF_EXTENSION))
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
      if (this.gffFile != null) {
        smd.setAnnotation(this.gffFile.toString());
        smd.setGenomicType("exon");
      }

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
  public DesignBuilder(String[] filenames) {

    if (filenames == null)
      return;

    for (String filename : filenames)
      addFile(filename);
  }

}
