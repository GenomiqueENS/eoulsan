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

package fr.ens.biologie.genomique.eoulsan.bio;

/**
 * This class define a method to compute tm for oligonucletides.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Sebastian Bassi <sbassi@genesdigitales.com>
 */
public class MeltingTemp {

  // universal gas constant in Cal/degrees C*Mol
  private static final float R = 1.987f;

  /**
   * Returns DNA tm using nearest neighbor thermodynamics. This method is
   * adapted from bioPython MeltingTemp script.
   * @param s Sequence
   * @param dnac DNA concentration [nM]
   * @param saltc salt concentration [mM]
   * @return the tm of the sequence
   */
  public static float tmstalucDNA(final String s, final float dnac,
      final float saltc) {

    float dh = 0; // DeltaH. Enthalpy
    float ds = 0; // deltaS Entropy

    final String sup = s.toUpperCase();

    final float[] tcRes = tercorrDNA(sup, ds, dh);
    // double vsTC = tcRes[0];
    float vs = tcRes[0];
    float vh = tcRes[1];

    // DNA/DNA
    // Allawi and SantaLucia (1997). Biochemistry 36 : 10581-10594
    vh = vh
        + (overcount(sup, "AA")) * 7.9f + (overcount(sup, "TT")) * 7.9f
        + (overcount(sup, "AT")) * 7.2f + (overcount(sup, "TA")) * 7.2f
        + (overcount(sup, "CA")) * 8.5f + (overcount(sup, "TG")) * 8.5f
        + (overcount(sup, "GT")) * 8.4f + (overcount(sup, "AC")) * 8.4f;
    vh = vh
        + (overcount(sup, "CT")) * 7.8f + (overcount(sup, "AG")) * 7.8f
        + (overcount(sup, "GA")) * 8.2f + (overcount(sup, "TC")) * 8.2f;
    vh = vh
        + (overcount(sup, "CG")) * 10.6f + (overcount(sup, "GC")) * 9.8f
        + (overcount(sup, "GG")) * 8f + (overcount(sup, "CC")) * 8f;
    vs = vs
        + (overcount(sup, "AA")) * 22.2f + (overcount(sup, "TT")) * 22.2f
        + (overcount(sup, "AT")) * 20.4f + (overcount(sup, "TA")) * 21.3f;
    vs = vs
        + (overcount(sup, "CA")) * 22.7f + (overcount(sup, "TG")) * 22.7f
        + (overcount(sup, "GT")) * 22.4f + (overcount(sup, "AC")) * 22.4f;
    vs = vs
        + (overcount(sup, "CT")) * 21.0f + (overcount(sup, "AG")) * 21.0f
        + (overcount(sup, "GA")) * 22.2f + (overcount(sup, "TC")) * 22.2f;
    vs = vs
        + (overcount(sup, "CG")) * 27.2f + (overcount(sup, "GC")) * 24.4f
        + (overcount(sup, "GG")) * 19.9f + (overcount(sup, "CC")) * 19.9f;
    ds = vs;
    dh = vh;

    ds = ds - 0.368f * (s.length() - 1f) * (float) Math.log(saltc / 1e3f);
    final float k = (dnac / 4.0f) * 1e-9f;

    return ((1000f * (-dh)) / (-ds + (R * ((float) Math.log(k))))) - 273.15f;
  }

  /**
   * Returns RNA tm using nearest neighbor thermodynamics. This method is
   * adapted from bioPython MeltingTemp script.
   * @param s Sequence
   * @param dnac DNA concentration [nM]
   * @param saltc salt concentration [mM]
   * @return the tm of the sequence
   */
  public static float tmstalucRNA(final String s, final int dnac,
      final int saltc) {

    float dh = 0; // DeltaH. Enthalpy
    float ds = 0; // deltaS Entropy

    final String sup = s.toUpperCase();

    final float[] tcRes = tercorrRNA(sup, ds, dh);
    // double vsTC = tcRes[0];
    float vs = tcRes[0];
    float vh = tcRes[1];

    // RNA/RNA hybridisation of Xia et al (1998)
    // Biochemistry 37: 14719-14735
    vh = vh
        + (overcount(sup, "AA")) * 6.82f + (overcount(sup, "TT")) * 6.6f
        + (overcount(sup, "AT")) * 9.38f + (overcount(sup, "TA")) * 7.69f
        + (overcount(sup, "CA")) * 10.44f + (overcount(sup, "TG")) * 10.5f
        + (overcount(sup, "GT")) * 11.4f + (overcount(sup, "AC")) * 10.2f;
    vh = vh
        + (overcount(sup, "CT")) * 10.48f + (overcount(sup, "AG")) * 7.6f
        + (overcount(sup, "GA")) * 12.44f + (overcount(sup, "TC")) * 13.3f;
    vh = vh
        + (overcount(sup, "CG")) * 10.64f + (overcount(sup, "GC")) * 14.88f
        + (overcount(sup, "GG")) * 13.39f + (overcount(sup, "CC")) * 12.2f;
    vs = vs
        + (overcount(sup, "AA")) * 19.0f + (overcount(sup, "TT")) * 18.4f
        + (overcount(sup, "AT")) * 26.7f + (overcount(sup, "TA")) * 20.5f;
    vs = vs
        + (overcount(sup, "CA")) * 26.9f + (overcount(sup, "TG")) * 27.8f
        + (overcount(sup, "GT")) * 29.5f + (overcount(sup, "AC")) * 26.2f;
    vs = vs
        + (overcount(sup, "CT")) * 27.1f + (overcount(sup, "AG")) * 19.2f
        + (overcount(sup, "GA")) * 32.5f + (overcount(sup, "TC")) * 35.5f;
    vs = vs
        + (overcount(sup, "CG")) * 26.7f + (overcount(sup, "GC")) * 36.9f
        + (overcount(sup, "GG")) * 32.7f + (overcount(sup, "CC")) * 29.7f;
    ds = vs;
    dh = vh;

    ds = ds - 0.368f * (s.length() - 1f) * (float) Math.log(saltc / 1e3f);

    final float k = (dnac / 4.0f) * 1e-9f;

    return ((1000f * (-dh)) / (-ds + (R * ((float) Math.log(k))))) - 273.15f;

  }

  private static float[] tercorrDNA(final String stri, final float ds,
      final float dh) {

    float deltah = 0;
    float deltas = 0;

    // DNA/DNA
    // Allawi and SantaLucia (1997). Biochemistry 36 : 10581-10594

    if (stri.startsWith("G") || stri.startsWith("C")) {
      deltah = deltah - 0.1f;
      deltas = deltas + 2.8f;
    } else if (stri.startsWith("A") || stri.startsWith("T")) {
      deltah = deltah - 2.3f;
      deltas = deltas - 4.1f;
    }
    if (stri.endsWith("G") || stri.endsWith("C")) {
      deltah = deltah - 0.1f;
      deltas = deltas + 2.8f;
    } else if (stri.endsWith("A") || stri.endsWith("T")) {
      deltah = deltah - 2.3f;
      deltas = deltas - 4.1f;
    }

    final float dhL = dh + deltah;
    final float dsL = ds + deltas;

    return new float[] {dsL, dhL};

  }

  private static float[] tercorrRNA(final String stri, final float ds,
      final float dh) {

    float deltah = 0;
    float deltas = 0;

    // RNA

    final char firstChar = stri.charAt(0);

    if (firstChar == 'G' || firstChar == 'C') {
      deltah = deltah - 3.61f;
      deltas = deltas - 1.5f;
    } else if (firstChar == 'A' || firstChar == 'T' || firstChar == 'U') {
      deltah = deltah - 3.72f;
      deltas = deltas + 10.5f;
    }

    if (stri.endsWith("G") || stri.endsWith("C")) {
      deltah = deltah - 3.61f;
      deltas = deltas - 1.5f;
    } else if (stri.endsWith("A") || stri.endsWith("T") || stri.endsWith("U")) {
      deltah = deltah - 3.72f;
      deltas = deltas + 10.5f;
    }

    final float dhL = dh + deltah;
    final float dsL = ds + deltas;

    return new float[] {dsL, dhL};
  }

  /**
   * Returns how many p are on st, works even for overlapping.
   */
  private static int overcount(final String st, final String p) {

    int ocu = 0;

    int x = 0;

    while (true) {

      final int i = st.indexOf(p, x);
      if (i == -1) {
        break;
      }

      ocu = ocu + 1;
      x = i + 1;
    }

    return ocu;
  }

}
