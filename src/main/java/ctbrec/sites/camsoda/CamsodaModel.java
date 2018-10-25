package ctbrec.sites.camsoda;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;

import ctbrec.AbstractModel;
import ctbrec.recorder.download.StreamSource;
import ctbrec.sites.Site;

public class CamsodaModel extends AbstractModel {

    @Override
    public boolean isOnline() throws IOException, ExecutionException, InterruptedException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isOnline(boolean ignoreCache) throws IOException, ExecutionException, InterruptedException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getOnlineState(boolean failFast) throws IOException, ExecutionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<StreamSource> getStreamSources() throws IOException, ExecutionException, ParseException, PlaylistException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void invalidateCacheEntries() {
        // TODO Auto-generated method stub

    }

    @Override
    public void receiveTip(int tokens) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public int[] getStreamResolution(boolean failFast) throws ExecutionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean follow() throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean unfollow() throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setSite(Site site) {
        // TODO Auto-generated method stub

    }

}
