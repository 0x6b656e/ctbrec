package ctbrec.sites.mfc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Model {

    private Double camscore;
    private String continent;
    private Integer flags;
    private Boolean hidecs;
    private Integer kbit;
    private Integer lastnews;
    private Integer mg;
    private Integer missmfc;
    private Integer newModel;
    private Integer rank;
    private Integer rc;
    private Integer sfw;
    private String topic;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private Set<String> tags = new HashSet<>();

    public Double getCamscore() {
        return camscore;
    }

    public void setCamscore(Double camscore) {
        this.camscore = camscore;
    }

    public String getContinent() {
        return continent;
    }

    public void setContinent(String continent) {
        this.continent = continent;
    }

    public Integer getFlags() {
        return flags;
    }

    public void setFlags(Integer flags) {
        this.flags = flags;
    }

    public Boolean getHidecs() {
        return hidecs;
    }

    public void setHidecs(Boolean hidecs) {
        this.hidecs = hidecs;
    }

    public Integer getKbit() {
        return kbit;
    }

    public void setKbit(Integer kbit) {
        this.kbit = kbit;
    }

    public Integer getLastnews() {
        return lastnews;
    }

    public void setLastnews(Integer lastnews) {
        this.lastnews = lastnews;
    }

    public Integer getMg() {
        return mg;
    }

    public void setMg(Integer mg) {
        this.mg = mg;
    }

    public Integer getMissmfc() {
        return missmfc;
    }

    public void setMissmfc(Integer missmfc) {
        this.missmfc = missmfc;
    }

    public Integer getNewModel() {
        return newModel;
    }

    public void setNewModel(Integer newModel) {
        this.newModel = newModel;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Integer getRc() {
        return rc;
    }

    public void setRc(Integer rc) {
        this.rc = rc;
    }

    public Integer getSfw() {
        return sfw;
    }

    public void setSfw(Integer sfw) {
        this.sfw = sfw;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public void merge(Model m) {
        if(m == null) {
            return;
        }
        camscore = m.camscore != null ? m.camscore : camscore;
        continent = m.continent != null ? m.continent : continent;
        flags = m.flags != null ? m.flags : flags;
        hidecs = m.hidecs != null ? m.hidecs : hidecs;
        kbit = m.kbit != null ? m.kbit : kbit;
        lastnews = m.lastnews != null ? m.lastnews : lastnews;
        mg = m.mg != null ? m.mg : mg;
        missmfc = m.missmfc != null ? m.missmfc : missmfc;
        newModel = m.newModel != null ? m.newModel : newModel;
        rank = m.rank != null ? m.rank : rank;
        rc = m.rc != null ? m.rc : rc;
        sfw = m.sfw != null ? m.sfw : sfw;
        topic = m.topic != null ? m.topic : topic;
        additionalProperties.putAll(m.additionalProperties);
        tags.addAll(m.tags);
    }


}
