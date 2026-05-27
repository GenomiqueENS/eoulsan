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

package fr.ens.biologie.genomique.eoulsan.checkers;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import java.util.Collections;
import java.util.Set;

/**
 * This class define a Checker on additional annotation files.
 *
 * @since 2.8
 * @author Laurent Jourdren
 */
public class AdditionalAnnotationChecker implements Checker {

  @Override
  public String getName() {

    return "additional_annotation_checker";
  }

  @Override
  public boolean isDesignChecker() {
    return false;
  }

  @Override
  public DataFormat getFormat() {
    return DataFormats.ADDITIONAL_ANNOTATION_TSV;
  }

  @Override
  public Set<DataFormat> getCheckersRequired() {
    return Collections.emptySet();
  }

  @Override
  public void configure(Set<Parameter> stepParameters) throws EoulsanException {}

  @Override
  public boolean check(Data data, CheckStore checkInfo) throws EoulsanException {

    final DataFile genomeFile = data.getDataFile();

    return !genomeFile.exists();
  }
}
