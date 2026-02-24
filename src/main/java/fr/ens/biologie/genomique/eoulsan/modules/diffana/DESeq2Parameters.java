package fr.ens.biologie.genomique.eoulsan.modules.diffana;

import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;

/**
 * This class contains DESeq2 module parameters.
 * @author Laurent Jourdren
 * @since 2.7
 */
public class DESeq2Parameters {

  // Workflow options for DEseq2
  private boolean normFig = true;
  private boolean diffanaFig = true;
  private boolean normDiffana = true;
  private boolean diffana = true;
  private SizeFactorsType sizeFactorsType = SizeFactorsType.RATIO;
  private FitType fitType = FitType.PARAMETRIC;
  private StatisticTest statisticTest = StatisticTest.WALD;
  private boolean expHeader = true;
  private boolean weightContrast = false;
  private String logoUrl;
  private String authorName;
  private String authorEmail;

  private int easyContrastVersion = 2;

  private boolean frozen = false;

  /***
   * Enum for the sizeFactorsType option in DESeq2 related to the estimation of
   * the size factor.
   */
  public enum SizeFactorsType {

    RATIO, ITERATE;

    /**
     * Get the size factors type to be used in DESeq2.
     * @param value value to set
     * @return the size factors type value
     * @throws EoulsanException if the size factors type value is different from
     *           ratio or iterate
     */
    public static SizeFactorsType get(final String value)
        throws EoulsanException {

      requireNonNull(value, "parameter argument cannot be null");

      final String lowerName = value.toLowerCase(Globals.DEFAULT_LOCALE).trim();

      for (SizeFactorsType dem : SizeFactorsType.values()) {

        if (dem.name().toLowerCase(Globals.DEFAULT_LOCALE).equals(lowerName)) {
          return dem;
        }
      }

      return null;
    }

    /**
     * Convert the enum name into DESeq2 value.
     * @return DESeq2 value
     */
    public String toDESeq2Value() {

      return this.name().toLowerCase(Globals.DEFAULT_LOCALE);
    }
  }

  /**
   * Enum for the fitType option in DESeq2 related to the dispersion estimation.
   */
  public enum FitType {

    PARAMETRIC, LOCAL, MEAN;

    /**
     * Get the fit type to be used in DESeq2.
     * @param value value to set
     * @return the fit type value
     * @throws EoulsanException if the fit type value is different from
     *           parametric, local or mean
     */
    public static FitType get(final String value) throws EoulsanException {

      requireNonNull(value, "parameter argument cannot be null");

      final String lowerName = value.toLowerCase(Globals.DEFAULT_LOCALE).trim();

      for (FitType dem : FitType.values()) {

        if (dem.name().toLowerCase(Globals.DEFAULT_LOCALE).equals(lowerName)) {
          return dem;
        }
      }

      return null;
    }

    /**
     * Convert the enum name into DESeq2 value.
     * @return DESeq2 value
     */
    public String toDESeq2Value() {

      return this.name().toLowerCase(Globals.DEFAULT_LOCALE);
    }
  }

  /**
   * Enum for the statisticTest option in DESeq2 related to the statistic test
   * to be used during the differential expression analysis
   */
  public enum StatisticTest {

    WALD("Wald"), LRT("LRT");

    private final String name;

    public String toDESeq2Value() {

      return name;
    }

    /**
     * Get the statistic test to be used in DESeq2.
     * @param value value to set
     * @return the statistic test value
     * @throws EoulsanException if the statistic test value is different from
     *           Wald or LRT
     */
    public static StatisticTest get(final String value)
        throws EoulsanException {

      requireNonNull(value, "parameter argument annot be null");

      final String lowerName = value.toLowerCase(Globals.DEFAULT_LOCALE).trim();

      for (StatisticTest dem : StatisticTest.values()) {

        if (dem.toDESeq2Value().toLowerCase(Globals.DEFAULT_LOCALE)
            .equals(lowerName)) {
          return dem;
        }
      }

      return null;
    }

    /**
     * Constructor.
     * @param method, dispersion estimation method
     */
    StatisticTest(final String method) {

      this.name = method;
    }

  }

  //
  // Getters
  //

  /**
   * Get the normFig parameter value.
   * @return the normFig
   */
  public boolean isNormFig() {
    return this.normFig;
  }

  /**
   * Get the diffanaFig parameter value.
   * @return the diffanaFig
   */
  public boolean isDiffanaFig() {
    return diffanaFig;
  }

  /**
   * Get the normDiffana parameter value.
   * @return the normDiffana
   */
  public boolean isNormDiffana() {
    return this.normDiffana;
  }

  /**
   * Get the diffana parameter value.
   * @return the diffana
   */
  public boolean isDiffana() {
    return this.diffana;
  }

  /**
   * Get the sizeFactorsType parameter value.
   * @return the sizeFactorsType
   */
  public SizeFactorsType getSizeFactorsType() {
    return this.sizeFactorsType;
  }

  /**
   * Get the fitType parameter value.
   * @return the fitType
   */
  public FitType getFitType() {
    return this.fitType;
  }

  /**
   * Get the statisticTest parameter value.
   * @return the statisticTest
   */
  public StatisticTest getStatisticTest() {
    return this.statisticTest;
  }

  /**
   * Get the expHeader parameter value.
   * @return the expHeader
   */
  public boolean isExpHeader() {
    return this.expHeader;
  }

  /**
   * Get the easy contrasts version to use.
   * @return the easy contrasts version to use
   */
  public int getEasyContrastsVersion() {
    return this.easyContrastVersion;
  }

  /**
   * Get the weight contrast.
   * @return the weightContrast
   */
  public boolean isWeightContrast() {
    return weightContrast;
  }

  /**
   * Get the URL of the logo.
   * @return the logoUrl
   */
  public String getLogoUrl() {
    return logoUrl;
  }

  /**
   * Get the author Name.
   * @return the author name
   */
  public String getAuthorName() {
    return authorName;
  }

  /**
   * Get the author email
   * @return the author email
   */
  public String getAuthorEmail() {
    return authorEmail;
  }

  //
  // Setters
  //

  /**
   * Set the normFig parameter value.
   * @param normFig the normFig to set
   */
  public void setNormFig(boolean normFig) {

    if (this.frozen) {
      throw new IllegalStateException();
    }

    this.normFig = normFig;
  }

  /**
   * Set the diffanaFig parameter value.
   * @param diffanaFig the diffanaFig to set
   */
  public void setDiffanaFig(boolean diffanaFig) {

    if (this.frozen) {
      throw new IllegalStateException();
    }

    this.diffanaFig = diffanaFig;
  }

  /**
   * Set the normDiffana parameter value.
   * @param normDiffana the normDiffana to set
   */
  public void setNormDiffana(boolean normDiffana) {

    if (this.frozen) {
      throw new IllegalStateException();
    }

    this.normDiffana = normDiffana;
  }

  /**
   * Set the diffana parameter value.
   * @param diffana the diffana to set
   */
  public void setDiffana(boolean diffana) {

    if (this.frozen) {
      throw new IllegalStateException();
    }

    this.diffana = diffana;
  }

  /**
   * Set the size factors type parameter value.
   * @param parameterName parameter name
   * @param value value to set
   * @throws EoulsanException if the parameter value is not valid
   */
  public void setSizeFactorsType(String parameterName, String value)
      throws EoulsanException {

    if (this.frozen) {
      throw new IllegalStateException();
    }

    var result = SizeFactorsType.get(value);
    if (result == null) {
      throw new EoulsanException("The value: "
          + value + " is not an acceptable value for the " + parameterName
          + " parameter.");
    }

    this.sizeFactorsType = result;
  }

  /**
   * Set the fit type parameter value.
   * @param parameterName parameter name
   * @param value value to set
   * @throws EoulsanException if the parameter value is not valid
   */
  public void setFitType(String parameterName, String value)
      throws EoulsanException {

    if (this.frozen) {
      throw new IllegalStateException();
    }

    var result = FitType.get(value);
    if (result == null) {
      throw new EoulsanException("The value: "
          + value + " is not an acceptable value for the " + parameterName
          + " parameter.");
    }

    this.fitType = result;
  }

  /**
   * Set the statistic test parameter value.
   * @param parameterName parameter name
   * @param value value to set
   * @throws EoulsanException if the parameter value is not valid
   */
  public void setStatisticTest(String parameterName, String value)
      throws EoulsanException {

    if (this.frozen) {
      throw new IllegalStateException();
    }

    var result = StatisticTest.get(value);
    if (result == null) {
      throw new EoulsanException("The value: "
          + value + " is not an acceptable value for the " + parameterName
          + " parameter.");
    }

    this.statisticTest = result;
  }

  /**
   * Set the expHeader parameter value.
   * @param expHeader the expHeader to set
   */
  public void setExpHeader(boolean expHeader) {

    if (this.frozen) {
      throw new IllegalStateException();
    }

    this.expHeader = expHeader;
  }

  /**
   * Set the easy contrasts version to use.
   * @param version the version
   */
  public void setEasyContrastsVersion(int version) {

    if (this.frozen) {
      throw new IllegalStateException();
    }

    this.easyContrastVersion = version;
  }

  /**
   * Set the weight contrast to use.
   * @param weightContrast the weightContrast to set
   */
  public void setWeightContrast(boolean weightContrast) {

    if (this.frozen) {
      throw new IllegalStateException();
    }

    this.weightContrast = weightContrast;
  }

  /**
   * Set the URL of the logo to use.
   * @param logoUrl the logoUrl to set
   */
  public void setLogoUrl(String logoUrl) {

    requireNonNull(authorEmail);

    if (this.frozen) {
      throw new IllegalStateException();
    }

    this.logoUrl = logoUrl;
  }

  /**
   * Set the author name.
   * @param authorName the authorName to set
   */
  public void setAuthorName(String authorName) {

    requireNonNull(authorEmail);

    if (this.frozen) {
      throw new IllegalStateException();
    }

    this.authorName = authorName;
  }

  /**
   * Set the author email.
   * @param authorEmail the authormail to set
   */
  public void setAuthorEmail(String authorEmail) {

    requireNonNull(authorEmail);

    if (this.frozen) {
      throw new IllegalStateException();
    }

    this.authorEmail = authorEmail;
  }

  /**
   * Freeze the values in the object.
   */
  public void freeze() {
    this.frozen = true;
  }

  //
  // Other methods
  //

  public void check() {

    requireNonNull(this.sizeFactorsType,
        "sizeFactorsType argument cannot be null");
    requireNonNull(this.fitType, "fitType argument cannot be null");
    requireNonNull(this.statisticTest, "statisticTest argument cannot be null");

  }

}
