package fr.ens.biologie.genomique.eoulsan.checkers;

/**
 * This class define a Checker on GTF annotation.
 *
 * @since 2.0
 * @author Laurent Jourdren
 */
public class GTFChecker extends GFFChecker {

  @Override
  public String getName() {

    return "gtf_checker";
  }

  //
  // Constructor
  //

  /** Public constructor. */
  public GTFChecker() {
    super(true);
  }
}
