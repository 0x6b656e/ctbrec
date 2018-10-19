package ctbrec.sites.mfc;

import java.util.HashMap;
import java.util.Map;

public class User {

    private Integer avatar;
    private String blurb;
    private Integer camserv;
    private String chatColor;
    private Integer chatFont;
    private Integer chatOpt;
    private String country;
    private Integer creation;
    private String ethnic;
    private String occupation;
    private Integer photos;
    private Integer profile;
    private String status;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Integer getAvatar() {
        return avatar;
    }

    public void setAvatar(Integer avatar) {
        this.avatar = avatar;
    }

    public String getBlurb() {
        return blurb;
    }

    public void setBlurb(String blurb) {
        this.blurb = blurb;
    }

    public Integer getCamserv() {
        return camserv;
    }

    public void setCamserv(Integer camserv) {
        this.camserv = camserv;
    }

    public String getChatColor() {
        return chatColor;
    }

    public void setChatColor(String chatColor) {
        this.chatColor = chatColor;
    }

    public Integer getChatFont() {
        return chatFont;
    }

    public void setChatFont(Integer chatFont) {
        this.chatFont = chatFont;
    }

    public Integer getChatOpt() {
        return chatOpt;
    }

    public void setChatOpt(Integer chatOpt) {
        this.chatOpt = chatOpt;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getCreation() {
        return creation;
    }

    public void setCreation(Integer creation) {
        this.creation = creation;
    }

    public String getEthnic() {
        return ethnic;
    }

    public void setEthnic(String ethnic) {
        this.ethnic = ethnic;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public Integer getPhotos() {
        return photos;
    }

    public void setPhotos(Integer photos) {
        this.photos = photos;
    }

    public Integer getProfile() {
        return profile;
    }

    public void setProfile(Integer profile) {
        this.profile = profile;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public void merge(User u) {
        if (u == null) {
            return;
        }
        avatar = u.avatar != null ? u.avatar : avatar;
        blurb = u.blurb != null ? u.blurb : blurb;
        camserv = u.camserv != null ? u.camserv : camserv;
        chatColor = u.chatColor != null ? u.chatColor : chatColor;
        chatFont = u.chatFont != null ? u.chatFont : chatFont;
        chatOpt = u.chatOpt != null ? u.chatOpt : chatOpt;
        country = u.country != null ? u.country : country;
        creation = u.creation != null ? u.creation : creation;
        ethnic = u.ethnic != null ? u.ethnic : ethnic;
        occupation = u.occupation != null ? u.occupation : occupation;
        photos = u.photos != null ? u.photos : photos;
        profile = u.profile != null ? u.profile : profile;
        status = u.status != null ? u.status : status;
        additionalProperties.putAll(u.additionalProperties);
    }
}
