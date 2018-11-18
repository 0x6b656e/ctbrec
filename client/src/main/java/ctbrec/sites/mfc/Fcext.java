package ctbrec.sites.mfc;

import java.util.HashMap;
import java.util.Map;

public class Fcext {

    private String sm;
    private Integer sfw;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public String getSm() {
        return sm;
    }

    public void setSm(String sm) {
        this.sm = sm;
    }

    public Integer getSfw() {
        return sfw;
    }

    public void setSfw(Integer sfw) {
        this.sfw = sfw;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public void merge(Fcext fcext) {
        if(fcext == null) {
            return;
        }

        sm = fcext.sm != null ? fcext.sm : sm;
        sfw = fcext.sfw != null ? fcext.sfw : sfw;
        additionalProperties.putAll(fcext.additionalProperties);
    }

}
