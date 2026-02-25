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

package fr.ens.biologie.genomique.eoulsan.modules.fastqc;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import uk.ac.babraham.FastQC.Modules.QCModule;
import uk.ac.babraham.FastQC.Report.HTMLReportArchiveNeigborClass;
import uk.ac.babraham.FastQC.Utilities.ImageToBase64;

/**
 * This class to patch FastQC to be compatible with Hadoop.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class FastQCRuntimePatcher {

  private static final String CLASS_NAME =
      "uk.ac.babraham.FastQC.Report.HTMLReportArchive";

  /**
   * Patch FastQC for hadoop mode.
   * @throws EoulsanException if an error occurs while patching FASTQC
   */
  public static synchronized void patchFastQC() throws EoulsanException {

    try {

      // Create the ClassPool and add the FastQC jar classpath in the ClassPool
      // classpath using a random class of the jar
      final ClassClassPath ccpath = new ClassClassPath(QCModule.class);
      final ClassPool pool = ClassPool.getDefault();
      pool.insertClassPath(ccpath);

      // Get the class to modify
      final CtClass cc = pool.get(CLASS_NAME);

      // Check if the class is not frozen
      if (cc != null && !cc.isFrozen()) {

        // Remove the old base64ForIcon method
        final CtMethod oldBase64ForIconMethod =
            cc.getDeclaredMethod("base64ForIcon");
        cc.removeMethod(oldBase64ForIconMethod);

        // Create the new base64ForIcon method
        final CtMethod newBase64ForIconMethod =
            CtNewMethod.make("private String base64ForIcon (String path) { "
                + " return fr.ens.biologie.genomique.eoulsan.modules.fastqc."
                + "FastQCRuntimePatcher.base64ForIcon(path, this.getClass());"
                + " } ", cc);
        cc.addMethod(newBase64ForIconMethod);

        // Load the class by the ClassLoader
        if (isJavaModuleSystemAvailable()) {
          cc.toClass(HTMLReportArchiveNeigborClass.class);
        } else {
          cc.toClass();
        }
      }

    } catch (NotFoundException | CannotCompileException e) {
      throw new EoulsanException(e);
    }
  }

  /**
   * Check if the version of Java use module system.
   * @return true if the version of Java use module system
   */
  private static boolean isJavaModuleSystemAvailable() {

    String javaSpecVersion =
        System.getProperty("java.vm.specification.version");

    // Is Java > 8
    return javaSpecVersion.indexOf('.') == -1;
  }

  /**
   * This method replace the HTMLReportArchive.base64ForIcon() method.
   * @param path path of the resource to load
   * @param clazz the class that call the method
   * @return an image encoded in base64
   */
  public static String base64ForIcon(String path, @SuppressWarnings("rawtypes")
  Class clazz) {

    try {
      BufferedImage b =
          ImageIO.read(clazz.getResourceAsStream("/Templates/" + path));
      return ImageToBase64.imageToBase64(b);
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return "Failed";
    }
  }

}
