package ctbrec.ui;

import ctbrec.sites.Site;
import ctbrec.sites.bonga.BongaCams;
import ctbrec.sites.cam4.Cam4;
import ctbrec.sites.camsoda.Camsoda;
import ctbrec.sites.chaturbate.Chaturbate;
import ctbrec.sites.mfc.MyFreeCams;
import ctbrec.sites.nood.Nood;
import ctbrec.sites.streamate.Streamate;
import ctbrec.ui.sites.bonga.BongaCamsSiteUi;
import ctbrec.ui.sites.cam4.Cam4SiteUi;
import ctbrec.ui.sites.camsoda.CamsodaSiteUi;
import ctbrec.ui.sites.chaturbate.ChaturbateSiteUi;
import ctbrec.ui.sites.myfreecams.MyFreeCamsSiteUi;
import ctbrec.ui.sites.nood.NoodSiteUi;
import ctbrec.ui.sites.streamate.StreamateSiteUi;

public class SiteUiFactory {

    private static BongaCamsSiteUi bongaSiteUi;
    private static Cam4SiteUi cam4SiteUi;
    private static CamsodaSiteUi camsodaSiteUi;
    private static ChaturbateSiteUi ctbSiteUi;
    private static MyFreeCamsSiteUi mfcSiteUi;
    private static NoodSiteUi noodSiteUi;
    private static StreamateSiteUi streamateSiteUi;

    public static synchronized SiteUI getUi(Site site) {
        if (site instanceof BongaCams) {
            if (bongaSiteUi == null) {
                bongaSiteUi = new BongaCamsSiteUi((BongaCams) site);
            }
            return bongaSiteUi;
        } else if (site instanceof Cam4) {
            if (cam4SiteUi == null) {
                cam4SiteUi = new Cam4SiteUi((Cam4) site);
            }
            return cam4SiteUi;
        } else if (site instanceof Camsoda) {
            if (camsodaSiteUi == null) {
                camsodaSiteUi = new CamsodaSiteUi((Camsoda) site);
            }
            return camsodaSiteUi;
        } else if (site instanceof Chaturbate) {
            if (ctbSiteUi == null) {
                ctbSiteUi = new ChaturbateSiteUi((Chaturbate) site);
            }
            return ctbSiteUi;
        } else if (site instanceof MyFreeCams) {
            if (mfcSiteUi == null) {
                mfcSiteUi = new MyFreeCamsSiteUi((MyFreeCams) site);
            }
            return mfcSiteUi;
        } else if (site instanceof Nood) {
            if (noodSiteUi == null) {
                noodSiteUi = new NoodSiteUi((Nood) site);
            }
            return noodSiteUi;
        } else if (site instanceof Streamate) {
            if (streamateSiteUi == null) {
                streamateSiteUi = new StreamateSiteUi((Streamate) site);
            }
            return streamateSiteUi;
        }
        throw new RuntimeException("Unknown site " + site.getName());
    }

}
