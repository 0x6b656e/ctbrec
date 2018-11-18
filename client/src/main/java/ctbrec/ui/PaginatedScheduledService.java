package ctbrec.ui;

import java.util.List;

import ctbrec.Model;
import javafx.concurrent.ScheduledService;

public abstract class PaginatedScheduledService extends ScheduledService<List<Model>> {

    protected int page = 1;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
