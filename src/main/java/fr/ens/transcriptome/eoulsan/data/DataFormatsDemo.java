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

package fr.ens.transcriptome.eoulsan.data;

import java.io.IOException;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeDebug;
import fr.ens.transcriptome.eoulsan.util.ServiceListLoader;

public class DataFormatsDemo {

  private static final String RESOURCE_PREFIX =
      "META-INF/services/xmldataformats/";

  private static void test(final DataFormat df1, final DataFormat df2) {

    final boolean result = df1.equals(df2);

    if (!result) {
      System.out.println(df1);
      System.out.println(df2);
      System.out.println();
    }

  }

  public static void main(String[] args) throws EoulsanException, IOException {

    EoulsanRuntimeDebug.initDebugEoulsanRuntime();
    
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();
    final DataFormatRegistry registry = DataFormatRegistry.getInstance();

    System.out
    .println(loader
        .getResource("META-INF/services/fr.ens.transcriptome.eoulsan.data.XMLDataFormat"));
    
    for (String filename : ServiceListLoader.load(XMLDataType.class.getName())) {

      final String resource = RESOURCE_PREFIX + filename;
      final DataFormat dfx =
          new XMLDataFormat(loader.getResourceAsStream(resource));
      final DataFormat dfs =
          registry.getDataFormatFromName(dfx.getFormatName());

      test(dfx, dfs);
    }

  }

}
