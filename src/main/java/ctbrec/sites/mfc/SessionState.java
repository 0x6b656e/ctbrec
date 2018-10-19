package ctbrec.sites.mfc;

import java.util.HashMap;
import java.util.Map;

public class SessionState {

    private Integer lv;
    private String nm;
    private Integer pid;
    private Integer sid;
    private Integer uid;
    private Integer vs;
    private User u;
    private Model m;
    private X x;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Integer getLv() {
        return lv;
    }

    public void setLv(Integer lv) {
        this.lv = lv;
    }

    public String getNm() {
        return nm;
    }

    public void setNm(String nm) {
        this.nm = nm;
    }

    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }

    public Integer getSid() {
        return sid;
    }

    public void setSid(Integer sid) {
        this.sid = sid;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public Integer getVs() {
        return vs;
    }

    public void setVs(Integer vs) {
        this.vs = vs;
    }

    public User getU() {
        return u;
    }

    public void setU(User u) {
        this.u = u;
    }

    public Model getM() {
        return m;
    }

    public void setM(Model m) {
        this.m = m;
    }

    public X getX() {
        return x;
    }

    public void setX(X x) {
        this.x = x;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        return Integer.toString(uid) + " u:" + u + " m:" + m + " x:" + x + " " + nm;
    }

    public void merge(SessionState newState) {
        lv = newState.lv != null ? newState.lv : lv;
        nm = newState.nm != null ? newState.nm : nm;
        pid = newState.pid != null ? newState.pid : pid;
        sid = newState.sid != null ? newState.sid : sid;
        vs = newState.vs != null ? newState.vs : vs;
        additionalProperties.putAll(newState.additionalProperties);

        if (u != null) {
            u.merge(newState.u);
        } else {
            u = newState.u;
        }
        if (m != null) {
            m.merge(newState.m);
        } else {
            m = newState.m;
        }
        if (x != null) {
            x.merge(newState.x);
        } else {
            x = newState.x;
        }
    }
}
