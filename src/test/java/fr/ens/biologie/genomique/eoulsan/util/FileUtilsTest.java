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

package fr.ens.biologie.genomique.eoulsan.util;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class FileUtilsTest {

  @Test
  public void relativizePathTest() {

    File r1 = FileUtils.relativizePath(new File("/usr/share/doc/toto.txt"),
        new File("/usr/share"));

    assertEquals(new File("doc/toto.txt"), r1);

    File r2 = FileUtils.relativizePath(new File("/usr/share/doc/toto.txt"),
        new File("/usr/share/X11/data"));

    assertEquals(new File("../../doc/toto.txt"), r2);

  }

}
