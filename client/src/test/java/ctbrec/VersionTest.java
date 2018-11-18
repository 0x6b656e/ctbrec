package ctbrec;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VersionTest {
    @Test
    public void testOf() {
        Version v = Version.of("1.0.0");
        assertEquals(1, v.major);
        assertEquals(0, v.minor);
        assertEquals(0, v.revision);

        v = Version.of("12.123.1234");
        assertEquals(12, v.major);
        assertEquals(123, v.minor);
        assertEquals(1234, v.revision);

        v = Version.of("1.0.0-SNAPSHOT");
        assertEquals(1, v.major);
        assertEquals(0, v.minor);
        assertEquals(0, v.revision);
        assertEquals("SNAPSHOT", v.designator);
    }

    @Test
    public void testCompareTo() {
        Version a = Version.of("1.0.0");
        Version b = Version.of("1.0.0");
        assertEquals(0, a.compareTo(b));

        a = Version.of("1.0.0");
        b = Version.of("1.0.1");
        assertEquals(-1, a.compareTo(b));

        a = Version.of("1.0.0");
        b = Version.of("1.1.0");
        assertEquals(-1, a.compareTo(b));

        a = Version.of("1.0.0");
        b = Version.of("2.0.0");
        assertEquals(-1, a.compareTo(b));

        a = Version.of("1.0.0-SNAPSHOT");
        b = Version.of("1.0.0");
        assertEquals(-1, a.compareTo(b));

        a = Version.of("1.0.0-beta1");
        b = Version.of("1.0.0");
        assertEquals(-1, a.compareTo(b));
    }
}
