package ctbrec.sites.mfc;

import java.util.HashMap;
import java.util.Map;

public class X {

    private Fcext fcext;
    private Share share;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Fcext getFcext() {
        return fcext;
    }

    public void setFcext(Fcext fcext) {
        this.fcext = fcext;
    }

    public Share getShare() {
        return share;
    }

    public void setShare(Share share) {
        this.share = share;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public void merge(X x) {
        if(x == null) {
            return;
        }
        if (fcext != null) {
            fcext.merge(x.fcext);
        }
        if (share != null) {
            share.merge(x.share);
        }
        additionalProperties.putAll(x.additionalProperties);

    }

}
