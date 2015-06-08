package fr.ens.transcriptome.eoulsan.steps.fastqc;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;
import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * This class redefine methods or constructors from FastQC to provide access to
 * files in fastqc jar. Add constructor on sequence file classes with input
 * stream, make compatible Hadoop mode execution.
 * @since 2.0
 * @author Laurent Jourdren
 * @author Sandrine Perrin
 */
public class RuntimePatchFastQC {

  public static void changeConstructorToEmpty(final String className)
      throws EoulsanException {

    try {
      // Get the class to modify
      final CtClass cc = ClassPool.getDefault().get(className);

      // Check class not frozen
      if (cc != null && !cc.isFrozen()) {

        // Retrieve the constructor
        final CtConstructor[] constructors = cc.getConstructors();

        if (constructors == null || constructors.length == 0) {
          throw new EoulsanException(
              "Step FastQC, patch code, not constructor found for class "
                  + className);
        }
        // Modify constructor, it does nothing
        constructors[0].setBody(null);

        // Load the class by the ClassLoader
        cc.toClass();
      }
    } catch (final NotFoundException e) {
      // Nothing to do
      throw new EoulsanException("Fail to found class "
          + className + " for patch code");

    } catch (final CannotCompileException e) {
      throw new EoulsanException(e);
    }
  }

  /**
   * Execute method who patch code from FastQC before call in FastQC step.
   * @throws EoulsanException throw an error occurs during modification bytecode
   *           fastqc
   */
  public static void runPatchFastQC() throws EoulsanException {

    changeConstructorToEmpty("uk.ac.babraham.FastQC.Sequence.FastQFile");
    changeConstructorToEmpty("uk.ac.babraham.FastQC.Sequence.BAMFile");
  }
}
