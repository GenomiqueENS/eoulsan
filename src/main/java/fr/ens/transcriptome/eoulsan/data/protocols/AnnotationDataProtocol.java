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

package fr.ens.transcriptome.eoulsan.data.protocols;

import java.util.List;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.data.DataFormats;

/**
 * This class define a annotation protocol.
 * @since 1.1
 * @author Laurent Jourdren
 */
@HadoopCompatible
public class AnnotationDataProtocol extends StorageDataProtocol {

  @Override
  public String getName() {

    return "annotation";
  }

  @Override
  protected List<String> getExtensions() {

    return DataFormats.ANNOTATION_GFF.getExtensions();
  }

  @Override
  protected String getBasePath() {

    return EoulsanRuntime.getSettings().getAnnotationStoragePath();
  }

}
