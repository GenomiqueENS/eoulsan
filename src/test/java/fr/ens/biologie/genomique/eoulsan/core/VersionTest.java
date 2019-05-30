package fr.ens.biologie.genomique.eoulsan.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.collect.Lists;

public class VersionTest {

  @Test
  public void testGetMajor() {

    Version v = new Version();

    assertEquals(0, v.getMajor());
    assertEquals(0, v.getMinor());
    assertEquals(0, v.getRevision());
    assertEquals("", v.getType());

    v.setMajor(2);
    assertEquals(2, v.getMajor());
    assertEquals(0, v.getMinor());
    assertEquals(0, v.getRevision());
    assertEquals("", v.getType());
  }

  @Test
  public void testGetMinor() {

    Version v = new Version();

    assertEquals(0, v.getMajor());
    assertEquals(0, v.getMinor());
    assertEquals(0, v.getRevision());
    assertEquals("", v.getType());

    v.setMinor(2);
    assertEquals(0, v.getMajor());
    assertEquals(2, v.getMinor());
    assertEquals(0, v.getRevision());
    assertEquals("", v.getType());
  }

  @Test
  public void testGetRevision() {

    Version v = new Version();

    assertEquals(0, v.getMajor());
    assertEquals(0, v.getMinor());
    assertEquals(0, v.getRevision());
    assertEquals("", v.getType());

    v.setRevision(3);
    assertEquals(0, v.getMajor());
    assertEquals(0, v.getMinor());
    assertEquals(3, v.getRevision());
    assertEquals("", v.getType());
  }

  @Test
  public void testGetType() {

    Version v = new Version();

    assertEquals(0, v.getMajor());
    assertEquals(0, v.getMinor());
    assertEquals(0, v.getRevision());
    assertEquals("", v.getType());

    v.setType("-beta");
    assertEquals(0, v.getMajor());
    assertEquals(0, v.getMinor());
    assertEquals(0, v.getRevision());
    assertEquals("-beta", v.getType());
  }

  @Test
  public void testToString() {

    assertEquals("0.0", new Version().toString());
    assertEquals("1.0", new Version(1, 0, 0).toString());
    assertEquals("1.2", new Version(1, 2, 0).toString());
    assertEquals("1.2.3", new Version(1, 2, 3).toString());
    assertEquals("1.2-beta", new Version(1, 2, 0, "-beta").toString());
    assertEquals("1.2.3-beta", new Version(1, 2, 3, "-beta").toString());
  }

  @Test
  public void testSetVersionIntIntInt() {

    Version v = new Version();

    assertEquals(0, v.getMajor());
    assertEquals(0, v.getMinor());
    assertEquals(0, v.getRevision());
    assertEquals("", v.getType());

    v.setVersion(1, 2, 3);

    assertEquals(1, v.getMajor());
    assertEquals(2, v.getMinor());
    assertEquals(3, v.getRevision());
    assertEquals("", v.getType());

  }

  @Test
  public void testSetVersionIntIntIntString() {

    Version v = new Version();

    assertEquals(0, v.getMajor());
    assertEquals(0, v.getMinor());
    assertEquals(0, v.getRevision());
    assertEquals("", v.getType());

    v.setVersion(1, 2, 3, "beta");

    assertEquals(1, v.getMajor());
    assertEquals(2, v.getMinor());
    assertEquals(3, v.getRevision());
    assertEquals("beta", v.getType());
  }

  @Test
  public void testSetVersionString() {

    Version v = new Version();

    assertEquals(0, v.getMajor());
    assertEquals(0, v.getMinor());
    assertEquals(0, v.getRevision());
    assertEquals("", v.getType());

    v.setVersion("1.2.3beta");

    assertEquals(1, v.getMajor());
    assertEquals(2, v.getMinor());
    assertEquals(3, v.getRevision());
    assertEquals("beta", v.getType());
  }

  @Test
  public void testCompareTo() {

    Version v1 = new Version(0, 0, 0, "");
    Version v2 = new Version(1, 0, 0, "");
    Version v3 = new Version(2, 0, 0, "");

    assertTrue(v2.compareTo(v1) > 0);
    assertEquals(0, v2.compareTo(v2));
    assertTrue(v2.compareTo(v3) < 0);

    v1.setVersion(1, 0, 0, "");
    v2.setVersion(1, 1, 0, "");
    v3.setVersion(1, 2, 0, "");

    assertTrue(v2.compareTo(v1) > 0);
    assertEquals(0, v2.compareTo(v2));
    assertTrue(v2.compareTo(v3) < 0);

    v1.setVersion(1, 1, 0, "");
    v2.setVersion(1, 1, 1, "");
    v3.setVersion(1, 1, 2, "");

    assertTrue(v2.compareTo(v1) > 0);
    assertEquals(0, v2.compareTo(v2));
    assertTrue(v2.compareTo(v3) < 0);

    v1.setVersion(1, 1, 1, "alpha");
    v2.setVersion(1, 1, 1, "beta");
    v3.setVersion(1, 1, 1, "rc");

    assertTrue(v2.compareTo(v1) > 0);
    assertEquals(0, v2.compareTo(v2));
    assertTrue(v2.compareTo(v3) < 0);
  }

  @Test
  public void testLessThan() {

    Version v1 = new Version(1, 0, 0, "");
    Version v2 = new Version(2, 0, 0, "");
    Version v1bis = new Version(1, 0, 0, "");

    assertTrue(v1.lessThan(v2));
    assertFalse(v2.lessThan(v1));
    assertFalse(v1.lessThan(v1bis));
    assertFalse(v1.lessThan(null));
  }

  @Test
  public void testLessThanOrEqualTo() {

    Version v1 = new Version(1, 0, 0, "");
    Version v2 = new Version(2, 0, 0, "");
    Version v1bis = new Version(1, 0, 0, "");

    assertTrue(v1.lessThanOrEqualTo(v2));
    assertFalse(v2.lessThanOrEqualTo(v1));
    assertTrue(v1.lessThanOrEqualTo(v1bis));
    assertFalse(v1.lessThanOrEqualTo(null));
  }

  @Test
  public void testGreaterThan() {

    Version v1 = new Version(1, 0, 0, "");
    Version v2 = new Version(2, 0, 0, "");
    Version v1bis = new Version(1, 0, 0, "");

    assertTrue(v2.greaterThan(v1));
    assertFalse(v1.greaterThan(v2));
    assertFalse(v1.greaterThan(v1bis));
    assertTrue(v1.greaterThan(null));
  }

  @Test
  public void testGreaterThanOrEqualTo() {

    Version v1 = new Version(1, 0, 0, "");
    Version v2 = new Version(2, 0, 0, "");
    Version v1bis = new Version(1, 0, 0, "");

    assertTrue(v2.greaterThanOrEqualTo(v1));
    assertFalse(v1.greaterThanOrEqualTo(v2));
    assertTrue(v1.greaterThanOrEqualTo(v1bis));
    assertTrue(v1.greaterThanOrEqualTo(null));
  }

  @Test
  public void testGetMinimalVersion() {

    assertEquals(new Version(0, 0, 0, ""),
        Version.getMinimalVersion(Lists.newArrayList(new Version(1, 0, 0, ""),
            new Version(0, 0, 0, ""), new Version(2, 0, 0, ""))));

  }

  @Test
  public void testGetMaximalVersion() {

    assertEquals(new Version(2, 0, 0, ""),
        Version.getMaximalVersion(Lists.newArrayList(new Version(1, 0, 0, ""),
            new Version(0, 0, 0, ""), new Version(2, 0, 0, ""))));
  }

  @Test
  public void testEqualsObject() {

    assertEquals(new Version(), new Version(0, 0, 0, ""));
    assertEquals(new Version(0, 0, 0, ""), new Version(0, 0, 0, ""));
    assertEquals(new Version(1, 0, 0, ""), new Version(1, 0, 0, ""));
    assertEquals(new Version(0, 1, 0, ""), new Version(0, 1, 0, ""));
    assertEquals(new Version(0, 0, 1, ""), new Version(0, 0, 1, ""));
    assertEquals(new Version(0, 0, 0, "beta"), new Version(0, 0, 0, "beta"));
    assertEquals(new Version(1, 2, 3, "beta"), new Version(1, 2, 3, "beta"));

    assertNotEquals(new Version(1, 0, 0, ""), new Version(2, 0, 0, ""));
    assertNotEquals(new Version(0, 1, 0, ""), new Version(0, 2, 0, ""));
    assertNotEquals(new Version(0, 0, 1, ""), new Version(0, 0, 2, ""));
    assertNotEquals(new Version(0, 0, 0, "beta"),
        new Version(0, 0, 0, "alpha"));
    assertNotEquals(new Version(1, 2, 3, "beta"),
        new Version(3, 0, 1, "alpha"));
  }

  @Test
  public void testVersionIntIntInt() {

    Version v = new Version(3, 2, 1);
    assertEquals(3, v.getMajor());
    assertEquals(2, v.getMinor());
    assertEquals(1, v.getRevision());
  }

  @Test
  public void testVersionIntIntIntString() {

    Version v = new Version(3, 2, 1, "beta");
    assertEquals(3, v.getMajor());
    assertEquals(2, v.getMinor());
    assertEquals(1, v.getRevision());
    assertEquals("beta", v.getType());
  }

  @Test
  public void testVersionString() {

    assertEquals(new Version(1, 2, 3, ""), new Version("1.2.3"));
    assertEquals(new Version(1, 2, 0, ""), new Version("1.2"));
    assertEquals(new Version(1, 0, 0, ""), new Version("1."));
    assertEquals(new Version(1, 0, 0, ""), new Version("1."));
    assertEquals(new Version(0, 0, 0, ""), new Version(""));
    assertEquals(new Version(0, 0, 0, ""), new Version("."));
    assertEquals(new Version(11, 22, 33, ""), new Version("11.22.33"));
    assertEquals(new Version(11, 22, 0, ""), new Version("11.22"));
    assertEquals(new Version(11, 22, 33, "-beta"),
        new Version("11.22.33-beta"));
    assertEquals(new Version(11, 22, 0, "-beta"), new Version("11.22-beta"));
    assertEquals(new Version(11, 0, 0, "-beta"), new Version("11-beta"));
  }

}
