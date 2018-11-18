package ctbrec.sites.mfc;

import java.util.HashMap;
import java.util.Map;

public class Share {

    private Integer albums;
    private Integer follows;
    private Integer tmAlbum;
    private Integer things;
    private Integer clubs;
    private Integer collections;
    private Integer stores;
    private Integer goals;
    private Integer polls;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Integer getAlbums() {
        return albums;
    }

    public void setAlbums(Integer albums) {
        this.albums = albums;
    }

    public Integer getFollows() {
        return follows;
    }

    public void setFollows(Integer follows) {
        this.follows = follows;
    }

    public Integer getTmAlbum() {
        return tmAlbum;
    }

    public void setTmAlbum(Integer tmAlbum) {
        this.tmAlbum = tmAlbum;
    }

    public Integer getThings() {
        return things;
    }

    public void setThings(Integer things) {
        this.things = things;
    }

    public Integer getClubs() {
        return clubs;
    }

    public void setClubs(Integer clubs) {
        this.clubs = clubs;
    }

    public Integer getCollections() {
        return collections;
    }

    public void setCollections(Integer collections) {
        this.collections = collections;
    }

    public Integer getStores() {
        return stores;
    }

    public void setStores(Integer stores) {
        this.stores = stores;
    }

    public Integer getGoals() {
        return goals;
    }

    public void setGoals(Integer goals) {
        this.goals = goals;
    }

    public Integer getPolls() {
        return polls;
    }

    public void setPolls(Integer polls) {
        this.polls = polls;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public void merge(Share share) {
        if (share == null) {
            return;
        }

        albums = share.albums != null ? share.albums : albums;
        follows = share.follows != null ? share.follows : follows;
        tmAlbum = share.tmAlbum != null ? share.tmAlbum : tmAlbum;
        things = share.things != null ? share.things : things;
        clubs = share.clubs != null ? share.clubs : clubs;
        collections = share.collections != null ? share.collections : collections;
        stores = share.stores != null ? share.stores : stores;
        goals = share.goals != null ? share.goals : goals;
        polls = share.polls != null ? share.polls : polls;
        additionalProperties.putAll(share.additionalProperties);
    }

}
