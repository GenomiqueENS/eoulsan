package fr.ens.biologie.genomique.eoulsan.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is used to mark a step class that requires the all the previous step in the
 * workflow file.
 *
 * @since 2.0
 * @author Laurent Jourdren
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAllPreviousSteps {}
