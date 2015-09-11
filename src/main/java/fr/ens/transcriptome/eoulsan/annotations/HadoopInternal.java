package fr.ens.transcriptome.eoulsan.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is used to mark a plug-in class as usable in internal hadoop
 * mode.
 * @since 2.0
 * @author Laurent Jourdren
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface HadoopInternal {

}
