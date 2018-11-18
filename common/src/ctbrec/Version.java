package ctbrec;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {
    int major = 0;
    int minor = 0;
    int revision = 0;
    String designator = "";

    public static Version of(String s) {
        Pattern p = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(?:-(.+))?");
        Matcher m = p.matcher(s);
        if(m.matches()) {
            Version v = new Version();
            v.major = Integer.parseInt(m.group(1));
            v.minor = Integer.parseInt(m.group(2));
            v.revision = Integer.parseInt(m.group(3));
            if(m.group(4) != null) {
                v.designator = m.group(4);
            }
            return v;
        } else {
            throw new IllegalArgumentException("Version format has to be x.x.x");
        }
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getRevision() {
        return revision;
    }

    public String getDesignator() {
        return designator;
    }

    @Override
    public String toString() {
        String version = major + "." + minor + "." + revision;
        if(!designator.isEmpty()) {
            version += "-" + designator;
        }
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((designator == null) ? 0 : designator.hashCode());
        result = prime * result + major;
        result = prime * result + minor;
        result = prime * result + revision;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Version other = (Version) obj;
        if (designator == null) {
            if (other.designator != null)
                return false;
        } else if (!designator.equals(other.designator))
            return false;
        if (major != other.major)
            return false;
        if (minor != other.minor)
            return false;
        if (revision != other.revision)
            return false;
        return true;
    }

    @Override
    public int compareTo(Version o) {
        int result = 0;
        if(major == o.major) {
            if(minor == o.minor) {
                if(revision == o.revision) {
                    if(!designator.isEmpty() && o.designator.isEmpty()) {
                        result = -1;
                    } else if(designator.isEmpty() && !o.designator.isEmpty()) {
                        result = 1;
                    } else {
                        result = 0;
                    }
                } else {
                    result = revision - o.revision;
                }
            } else {
                result = minor - o.minor;
            }
        } else {
            result = major - o.major;
        }
        return result;
    }
}
