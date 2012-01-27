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

package fr.ens.transcriptome.eoulsan.checkers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.GFFEntry;
import fr.ens.transcriptome.eoulsan.bio.Sequence;
import fr.ens.transcriptome.eoulsan.bio.io.GFFFastaReader;
import fr.ens.transcriptome.eoulsan.bio.io.GFFReader;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.expression.AbstractExpressionStep;
import fr.ens.transcriptome.eoulsan.steps.expression.TranscriptAndExonFinder;

public class AnnotationChecker implements Checker {

  private String genomicType;

  @Override
  public String getName() {

    return "annotation_checker";
  }

  @Override
  public void configure(Set<Parameter> stepParameters) throws EoulsanException {
    for (Parameter p : stepParameters) {

      if (AbstractExpressionStep.GENOMIC_TYPE_PARAMETER_NAME
          .equals(p.getName()))
        this.genomicType = p.getStringValue();
      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());

    }

  }

  @Override
  public boolean check(final Design design, final Context context,
      final CheckStore checkInfo) throws EoulsanException {
    if (design == null)
      throw new NullPointerException("The design is null");

    if (context == null)
      throw new NullPointerException("The execution context is null");

    if (checkInfo == null)
      throw new NullPointerException("The check info info is null");

    final List<Sample> samples = design.getSamples();

    if (samples == null)
      throw new NullPointerException("The samples are null");

    if (samples.size() == 0)
      throw new EoulsanException("No samples found in design");

    final Sample s = samples.get(0);

    try {
      final DataFile file = context.getDataFile(DataFormats.ANNOTATION_GFF, s);

      if (!file.exists())
        return true;

      if (this.genomicType == null)
        partialValidationAndFastaSectionCheck(file);
      else
        fullValidationCheck(file, genomicType, design, context, checkInfo);

    } catch (IOException e) {
      throw new EoulsanException(
          "Error while reading annotation file for checking: " + e.getMessage());
    } catch (BadBioEntryException e) {
      throw new EoulsanException(
          "Bad entry in annotation file while checking: " + e.getEntry());
    }
    return false;
  }

  @SuppressWarnings("unused")
  private static void partialValidationAndFastaSectionCheck(final DataFile file)
      throws IOException, BadBioEntryException, EoulsanException {

    InputStream is = file.open();
    final GFFReader gffReader = new GFFReader(is);

    for (final GFFEntry e : gffReader)
      ;
    gffReader.throwException();
    is.close();

    // Check fasta section if exists
    if (gffReader.isFastaSectionFound()) {

      is = file.open();
      GFFFastaReader fastaReader = new GFFFastaReader(is);
      for (final Sequence sequence : fastaReader)
        ;
      is.close();
      fastaReader.throwException();

    }
  }

  private void fullValidationCheck(final DataFile file,
      final String genomicType, final Design design, final Context context,
      final CheckStore checkInfo) throws EoulsanException, IOException,
      BadBioEntryException {

    final InputStream is = file.open();

    final TranscriptAndExonFinder ef =
        new TranscriptAndExonFinder(is, genomicType);

    if (ef.getTranscriptsIds().size() == 0)
      throw new EoulsanException("No transcripts found for genomic type ("
          + genomicType + ") in annotation.");

    // TODO compare chromosomes names
    final Set<String> genomeChromosomes =
        getGenomeChromosomes(design, context, checkInfo);

    if (genomeChromosomes != null) {

      final Set<String> annotationChromosomes = ef.getChromosomesIds();

      for (String chr : annotationChromosomes)
        if (!genomeChromosomes.contains(chr))
          throw new EoulsanException("Chromosome "
              + chr + " not found in the list of genome chromosomes.");
    }

  }

  @SuppressWarnings("unchecked")
  private Set<String> getGenomeChromosomes(final Design design,
      final Context context, final CheckStore checkInfo)
      throws EoulsanException {

    Object o = checkInfo.get(GenomeChecker.INFO_CHROMOSOME);

    if (o == null) {

      DataFormats.GENOME_FASTA.getChecker().check(design, context, checkInfo);
      o = checkInfo.get(GenomeChecker.INFO_CHROMOSOME);

      if (o == null)
        return null;
    }

    return ((Map<String, Integer>) checkInfo.get(GenomeChecker.INFO_CHROMOSOME))
        .keySet();
  }

}
