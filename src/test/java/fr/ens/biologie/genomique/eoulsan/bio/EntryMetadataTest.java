package fr.ens.biologie.genomique.eoulsan.bio;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class EntryMetadataTest {

  @Test
  public void testAddStringString() {

    EntryMetadata e = new EntryMetadata();

    assertEquals(Collections.emptySet(), e.keySet());
    assertFalse(e.add("key", null));
    assertEquals(Collections.emptySet(), e.keySet());
    assertFalse(e.add(null, "value"));
    assertEquals(Collections.emptySet(), e.keySet());
    assertFalse(e.add(null, null));
    assertEquals(Collections.emptySet(), e.keySet());

    assertTrue(e.add("key", "val1"));
    assertEquals(Collections.singleton("key"), e.keySet());
    assertEquals(Collections.singletonList("val1"), e.get("key"));
    assertTrue(e.add("key", "val2"));
    assertEquals(Collections.singleton("key"), e.keySet());
    assertEquals(Arrays.asList("val1", "val2"), e.get("key"));

    assertTrue(e.add("key2", "val3"));
    assertEquals(new HashSet<>(Arrays.asList("key", "key2")), e.keySet());
    assertEquals(Collections.singletonList("val3"), e.get("key2"));
  }

  @Test
  public void testAddEntryMetadata() {

    EntryMetadata e = new EntryMetadata();
    assertTrue(e.add("key1", "value1"));
    assertEquals(Collections.singleton("key1"), e.keySet());

    assertFalse(e.add((EntryMetadata) null));

    EntryMetadata e2 = new EntryMetadata();
    e2.add("key2", "value2");
    e2.add("key3", "value3");
    assertTrue(e.add(e2));

    assertEquals(new HashSet<>(Arrays.asList("key1", "key2", "key3")),
        e.keySet());
  }

  @Test
  public void testAddMapOfStringListOfString() {

    EntryMetadata e = new EntryMetadata();

    assertFalse(e.add((Map<String, List<String>>) null));
    Map<String, List<String>> entries = new HashMap<>();
    assertTrue(e.add(entries));
    entries.put("key0", null);
    assertFalse(e.add(entries));
    entries.clear();
    List<String> l = new ArrayList<>();
    l.add(null);
    entries.put("key00", l);
    assertFalse(e.add(entries));
    entries.clear();
    entries.put("key1", Collections.singletonList("val1"));
    assertTrue(e.add(entries));
    entries.clear();
    entries.put("key2", Arrays.asList("val2", "val3"));
    assertTrue(e.add(entries));
  }

  @Test
  public void testContainsKey() {

    EntryMetadata e = new EntryMetadata();

    assertFalse(e.containsKey("toto"));

    assertFalse(e.containsKey("key0"));
    assertEquals(0, e.keySet().size());

    e.add("key1", "val1");
    assertFalse(e.containsKey("key0"));
    assertTrue(e.containsKey("key1"));

    e.add("key2", "val2");
    assertFalse(e.containsKey("key0"));
    assertTrue(e.containsKey("key1"));
    assertTrue(e.containsKey("key2"));
  }

  @Test
  public void testGet() {

    EntryMetadata e = new EntryMetadata();

    assertEquals(Collections.emptyList(), e.get(null));
    assertEquals(Collections.emptyList(), e.get("toto"));

    e.add("key1", "val1");
    assertEquals(1, e.get("key1").size());
    assertEquals("val1", e.get("key1").get(0));

    e.add("key2", "val2");
    assertEquals(1, e.get("key1").size());
    assertEquals("val1", e.get("key1").get(0));
    assertEquals(1, e.get("key2").size());
    assertEquals("val2", e.get("key2").get(0));
  }

  @Test
  public void testKeySet() {

    EntryMetadata e = new EntryMetadata();

    assertEquals(Collections.emptySet(), e.keySet());

    assertEquals(0, e.keySet().size());

    e.add("key1", "val1");
    assertEquals(1, e.keySet().size());

    assertFalse(e.keySet().contains("key0"));
    assertTrue(e.keySet().contains("key1"));

    e.add("key2", "val2");
    assertEquals(2, e.keySet().size());

    assertFalse(e.keySet().contains("key0"));
    assertTrue(e.keySet().contains("key1"));
    assertTrue(e.keySet().contains("key2"));
  }

  @Test
  public void testEntries() {

    EntryMetadata e = new EntryMetadata();

    assertEquals(Collections.emptyMap(), e.entries());

    e.add("key1", "value1");
    e.add("key2", "value2");
    e.add("key2", "value3");
    e.add("key3", "value4");

    Map<String, List<String>> map = new HashMap<>();
    map.put("key1", Collections.singletonList("value1"));
    map.put("key2", Arrays.asList("value2", "value3"));
    map.put("key3", Collections.singletonList("value4"));

    assertEquals(map, e.entries());
  }

  @Test
  public void testRemove() {

    EntryMetadata e = new EntryMetadata();

    assertEquals(Collections.emptySet(), e.keySet());
    e.add("key1", "value1");
    assertEquals(Collections.singleton("key1"), e.keySet());
    e.add("key2", "value2");
    assertEquals(new HashSet<>(asList("key1", "key2")), e.keySet());
    assertFalse(e.remove("key3"));
    assertEquals(new HashSet<>(asList("key1", "key2")), e.keySet());
    assertFalse(e.remove(null));
    assertEquals(new HashSet<>(asList("key1", "key2")), e.keySet());
    assertTrue(e.remove("key1"));
    assertEquals(Collections.singleton("key2"), e.keySet());
  }

  @Test
  public void testClear() {

    EntryMetadata e = new EntryMetadata();

    assertEquals(Collections.emptySet(), e.keySet());
    e.add("key1", "value1");
    assertEquals(Collections.singleton("key1"), e.keySet());
    e.clear();
    assertEquals(Collections.emptySet(), e.keySet());
  }

  @Test
  public void testEqualsObject() {

    EntryMetadata e1 = new EntryMetadata();
    EntryMetadata e2 = new EntryMetadata();

    assertEquals(e1, e1);
    assertEquals(e1, e1);

    assertNotEquals(null, e1);
    assertNotEquals("toto", e1);
    assertEquals(e1, e2);
    assertEquals(e1, e2);

    e1.add("key", "value");
    assertNotEquals(e1, e2);
    assertNotEquals(e1, e2);

    e2.add("key", "value");
    assertEquals(e1, e2);
    assertEquals(e1, e2);
  }

  @Test
  public void testHashCode() {

    EntryMetadata e1 = new EntryMetadata();
    EntryMetadata e2 = new EntryMetadata();

    assertEquals(e1.hashCode(), e2.hashCode());

    e1.add("key", "value");
    assertNotEquals(e1.hashCode(), e2.hashCode());
  }

  @Test
  public void testEntryMetadataEntryMetadata() {

    try {
      new EntryMetadata(null);
      fail();
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    EntryMetadata e2 = new EntryMetadata();
    e2.add("key2", "value2");
    e2.add("key3", "value3");

    EntryMetadata e = new EntryMetadata(e2);
    assertEquals(new HashSet<>(Arrays.asList("key2", "key3")),
        e.keySet());
  }

}
