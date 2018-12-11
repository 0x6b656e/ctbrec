package ctbrec.ui.sites.myfreecams;

import java.io.IOException;
import java.util.Collection;

import ctbrec.sites.mfc.MyFreeCams;
import ctbrec.sites.mfc.MyFreeCamsClient;
import ctbrec.sites.mfc.SessionState;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;

public class TableUpdateService extends ScheduledService<Collection<SessionState>> {

    private MyFreeCams mfc;

    public TableUpdateService(MyFreeCams mfc) {
        this.mfc = mfc;
    }

    @Override
    protected Task<Collection<SessionState>> createTask() {
        return new Task<Collection<SessionState>>() {
            @Override
            public Collection<SessionState> call() throws IOException {
                MyFreeCamsClient client = mfc.getClient();
                return client.getSessionStates();
            }
        };
    }

}
