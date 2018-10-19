package ctbrec.recorder.download;

import java.text.DecimalFormat;

public class StreamSource implements Comparable<StreamSource> {
    public int bandwidth;
    public int width;
    public int height;
    public String mediaPlaylistUrl;

    public int getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getMediaPlaylistUrl() {
        return mediaPlaylistUrl;
    }

    public void setMediaPlaylistUrl(String mediaPlaylistUrl) {
        this.mediaPlaylistUrl = mediaPlaylistUrl;
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("0.00");
        float mbit = bandwidth / 1024.0f / 1024.0f;
        return height + "p (" + df.format(mbit) + " Mbit/s)";
    }

    /**
     * First compares the sources by height, if the heights are the same
     * it compares the bandwidth values
     */
    @Override
    public int compareTo(StreamSource o) {
        int heightDiff = height - o.height;
        if(heightDiff != 0) {
            return heightDiff;
        } else {
            return bandwidth - o.bandwidth;
        }
    }
}
