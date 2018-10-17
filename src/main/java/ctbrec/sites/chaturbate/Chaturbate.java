package ctbrec.sites.chaturbate;

import ctbrec.Model;
import ctbrec.Site;
import ctbrec.recorder.Recorder;
import ctbrec.ui.TabProvider;

public class Chaturbate implements Site {

    private Recorder recorder;

    @Override
    public String getName() {
        return "Chaturbate";
    }

    @Override
    public String getBaseUrl() {
        return "https://chaturbate.com";
    }

    @Override
    public String getAffiliateLink() {
        return getBaseUrl() + "/in/?track=default&tour=LQps&campaign=55vTi&room=0xb00bface";
    }

    @Override
    public TabProvider getTabProvider() {
        return new ChaturbateTabProvider(this, recorder);
    }

    @Override
    public void setRecorder(Recorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public Model createModel(String name) {
        ChaturbateModel m = new ChaturbateModel();
        m.setName(name);
        m.setUrl(getBaseUrl() + '/' + name + '/');
        return m;
    }
}
