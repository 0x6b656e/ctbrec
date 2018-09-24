package ctbrec.ui;


import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.collections.ObservableListWrapper;

import ctbrec.Config;
import ctbrec.HttpClient;
import ctbrec.Model;
import ctbrec.ModelParser;
import ctbrec.recorder.Recorder;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import okhttp3.Request;
import okhttp3.Response;

public class ThumbOverviewTab extends Tab implements TabSelectionListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(ThumbOverviewTab.class);

    static Set<Model> resolutionProcessing = Collections.synchronizedSet(new HashSet<>());
    static BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    static ExecutorService threadPool = new ThreadPoolExecutor(2, 2, 10, TimeUnit.MINUTES, queue);

    ScheduledService<List<Model>> updateService;
    Recorder recorder;
    List<ThumbCell> filteredThumbCells = Collections.synchronizedList(new ArrayList<>());
    List<ThumbCell> selectedThumbCells = Collections.synchronizedList(new ArrayList<>());
    String filter;
    FlowPane grid = new FlowPane();
    ReentrantLock gridLock = new ReentrantLock();
    ScrollPane scrollPane = new ScrollPane();
    String url;
    boolean loginRequired;
    HttpClient client = HttpClient.getInstance();
    int page = 1;
    TextField pageInput = new TextField(Integer.toString(page));
    Button pagePrev = new Button("◀");
    Button pageNext = new Button("▶");
    private volatile boolean updatesSuspended = false;
    ContextMenu popup;

    private ComboBox<Integer> thumbWidth;

    public ThumbOverviewTab(String title, String url, boolean loginRequired) {
        super(title);
        this.url = url;
        this.loginRequired = loginRequired;
        setClosable(false);
        createGui();
        initializeUpdateService();
    }

    private void createGui() {
        grid.setPadding(new Insets(5));
        grid.setHgap(5);
        grid.setVgap(5);

        TextField search = new TextField();
        search.setPromptText("Filter");
        search.textProperty().addListener( (observableValue, oldValue, newValue) -> {
            filter = search.getText();
            gridLock.lock();
            try {
                filter();
            } finally {
                gridLock.unlock();
            }
        });
        search.setTooltip(new Tooltip("Filter the models by their name, stream description or #hashtags.\n\n"+""
                + "If the display of stream resolution is enabled, you can even filter by resolution. Try \"1080\" or \">720\""));
        BorderPane.setMargin(search, new Insets(5));

        scrollPane.setContent(grid);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        BorderPane.setMargin(scrollPane, new Insets(5));

        HBox pagination = new HBox(5);
        pagination.getChildren().add(pagePrev);
        pagination.getChildren().add(pageNext);
        pagination.getChildren().add(pageInput);
        BorderPane.setMargin(pagination, new Insets(5));
        pageInput.setPrefWidth(50);
        pageInput.setOnAction((e) -> handlePageNumberInput());
        pagePrev.setOnAction((e) -> {
            page = Math.max(1, --page);
            pageInput.setText(Integer.toString(page));
            restartUpdateService();
        });
        pageNext.setOnAction((e) -> {
            page++;
            pageInput.setText(Integer.toString(page));
            restartUpdateService();
        });

        ThumbCell.width = Config.getInstance().getSettings().thumbWidth;
        HBox thumbSizeSelector = new HBox(5);
        Label l = new Label("Thumb Size");
        l.setPadding(new Insets(5,0,0,0));
        thumbSizeSelector.getChildren().add(l);
        List<Integer> thumbWidths = new ArrayList<>();
        thumbWidths.add(180);
        thumbWidths.add(200);
        thumbWidths.add(220);
        thumbWidths.add(270);
        thumbWidths.add(360);
        thumbWidth = new ComboBox<>(new ObservableListWrapper<>(thumbWidths));
        thumbWidth.getSelectionModel().select(new Integer(ThumbCell.width));
        thumbWidth.setOnAction((e) -> {
            int width = thumbWidth.getSelectionModel().getSelectedItem();
            ThumbCell.width = width;
            Config.getInstance().getSettings().thumbWidth = width;
            for (Node node : grid.getChildren()) {
                ThumbCell cell = (ThumbCell) node;
                cell.setThumbWidth(width);
            }
            for (ThumbCell cell : filteredThumbCells) {
                cell.setThumbWidth(width);
            }
        });
        thumbSizeSelector.getChildren().add(thumbWidth);
        BorderPane.setMargin(thumbSizeSelector, new Insets(5));


        BorderPane bottomPane = new BorderPane();
        bottomPane.setLeft(pagination);
        bottomPane.setRight(thumbSizeSelector);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(5));
        root.setTop(search);
        root.setCenter(scrollPane);
        root.setBottom(bottomPane);
        setContent(root);
    }

    private void handlePageNumberInput() {
        try {
            page = Integer.parseInt(pageInput.getText());
            page = Math.max(1, page);
            restartUpdateService();
        } catch(NumberFormatException e) {
        } finally {
            pageInput.setText(Integer.toString(page));
        }
    }

    private void restartUpdateService() {
        gridLock.lock();
        try {
            grid.getChildren().clear();
            filteredThumbCells.clear();
            deselected();
            selected();
        } finally {
            gridLock.unlock();
        }
    }

    void initializeUpdateService() {
        updateService = createUpdateService();
        updateService.setPeriod(new Duration(TimeUnit.SECONDS.toMillis(10)));
        updateService.setOnSucceeded((event) -> onSuccess());
        updateService.setOnFailed((event) -> onFail(event));
    }

    protected void onSuccess() {
        if(updatesSuspended) {
            return;
        }
        gridLock.lock();
        try {
            List<Model> models = updateService.getValue();
            ObservableList<Node> nodes = grid.getChildren();

            // first remove models, which are not in the updated list
            for (Iterator<Node> iterator = nodes.iterator(); iterator.hasNext();) {
                Node node = iterator.next();
                if (!(node instanceof ThumbCell)) continue;
                ThumbCell cell = (ThumbCell) node;
                if(!models.contains(cell.getModel())) {
                    iterator.remove();
                }
            }

            List<ThumbCell> positionChangedOrNew = new ArrayList<>();
            int index = 0;
            for (Model model : models) {
                boolean found = false;
                for (Iterator<Node> iterator = nodes.iterator(); iterator.hasNext();) {
                    Node node = iterator.next();
                    if (!(node instanceof ThumbCell)) continue;
                    ThumbCell cell = (ThumbCell) node;
                    if(cell.getModel().equals(model)) {
                        found = true;
                        cell.setModel(model);
                        if(index != cell.getIndex()) {
                            cell.setIndex(index);
                            positionChangedOrNew.add(cell);
                        }
                    }
                }
                if(!found) {
                    ThumbCell newCell = createThumbCell(this, model, recorder, client);
                    newCell.setIndex(index);
                    positionChangedOrNew.add(newCell);
                }
                index++;
            }
            for (ThumbCell thumbCell : positionChangedOrNew) {
                nodes.remove(thumbCell);
                if(thumbCell.getIndex() < nodes.size()) {
                    nodes.add(thumbCell.getIndex(), thumbCell);
                } else {
                    nodes.add(thumbCell);
                }
            }

            filteredThumbCells.clear();
            filter();
            moveActiveRecordingsToFront();
        } finally {
            gridLock.unlock();
        }

    }

    private ThumbCell createThumbCell(ThumbOverviewTab thumbOverviewTab, Model model, Recorder recorder2, HttpClient client2) {
        ThumbCell newCell = new ThumbCell(this, model, recorder, client);
        newCell.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            suspendUpdates(true);
            popup = createContextMenu(newCell);
            popup.show(newCell, event.getScreenX(), event.getScreenY());
            popup.setOnHidden((e) -> suspendUpdates(false));
            event.consume();
        });
        newCell.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if(popup != null) {
                popup.hide();
                popup = null;
                return;
            }
        });
        newCell.selectionProperty().addListener((obs, oldValue, newValue) -> {
            if(newValue) {
                selectedThumbCells.add(newCell);
            } else {
                selectedThumbCells.remove(newCell);
            }
        });
        newCell.setOnMouseClicked(mouseClickListener);
        return newCell;
    }

    private ContextMenu createContextMenu(ThumbCell cell) {
        MenuItem openInPlayer = new MenuItem("Open in Player");
        openInPlayer.setOnAction((e) -> startPlayer(cell));

        MenuItem start = new MenuItem("Start Recording");
        start.setOnAction((e) -> startStopAction(cell, true));
        MenuItem stop = new MenuItem("Stop Recording");
        stop.setOnAction((e) -> startStopAction(cell, false));
        MenuItem startStop = recorder.isRecording(cell.getModel()) ? stop : start;

        MenuItem follow = new MenuItem("Follow");
        follow.setOnAction((e) -> follow(cell, true));
        MenuItem unfollow = new MenuItem("Unfollow");
        unfollow.setOnAction((e) -> follow(cell, false));

        MenuItem copyUrl = new MenuItem("Copy URL");
        copyUrl.setOnAction((e) -> {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(cell.getModel().getUrl());
            clipboard.setContent(content);
        });

        // check, if other cells are selected, too. in that case, we have to disable menu item, which make sense only for
        // single selections. but only do that, if the popup has been triggered on a selected cell. otherwise remove the
        // selection and show the normal menu
        if (selectedThumbCells.size() > 1 || selectedThumbCells.size() == 1 && selectedThumbCells.get(0) != cell) {
            if(cell.isSelected()) {
                if(Config.getInstance().getSettings().singlePlayer) {
                    openInPlayer.setDisable(true);
                }
                copyUrl.setDisable(true);
            } else {
                removeSelection();
            }
        }

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setAutoHide(true);
        contextMenu.setHideOnEscape(true);
        contextMenu.setAutoFix(true);
        MenuItem followOrUnFollow = this instanceof FollowedTab ? unfollow : follow;
        contextMenu.getItems().addAll(openInPlayer, startStop , followOrUnFollow, copyUrl);
        return contextMenu;
    }

    private void follow(ThumbCell cell, boolean follow) {
        if(selectedThumbCells.isEmpty()) {
            cell.follow(follow);
        } else {
            for (ThumbCell thumbCell : selectedThumbCells) {
                thumbCell.follow(follow);
            }
        }
    }

    private void startStopAction(ThumbCell cell, boolean start) {
        if(selectedThumbCells.isEmpty()) {
            cell.startStopAction(start);
        } else {
            for (ThumbCell thumbCell : selectedThumbCells) {
                thumbCell.startStopAction(start);
            }
        }
    }

    private void startPlayer(ThumbCell cell) {
        if(selectedThumbCells.isEmpty()) {
            cell.startPlayer();
        } else {
            for (ThumbCell thumbCell : selectedThumbCells) {
                thumbCell.startPlayer();
            }
        }
    }

    private EventHandler<MouseEvent> mouseClickListener = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent e) {
            ThumbCell cell = (ThumbCell) e.getSource();
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                cell.setSelected(false);
                cell.startPlayer();
            } else if (e.getButton() == MouseButton.PRIMARY && e.isShiftDown()) {
                if(popup == null) {
                    cell.setSelected(!cell.isSelected());
                }
            } else if (e.getButton() == MouseButton.PRIMARY) {
                removeSelection();
            }
        }
    };

    protected void onFail(WorkerStateEvent event) {
        if(updatesSuspended) {
            return;
        }
        Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Couldn't fetch model list");
        if(event.getSource().getException() != null) {
            if(event.getSource().getException() instanceof SocketTimeoutException) {
                LOG.debug("Fetching model list timed out");
                return;
            } else {
                alert.setContentText(event.getSource().getException().getLocalizedMessage());
            }
        } else {
            alert.setContentText(event.getEventType().toString());
        }
        alert.showAndWait();
    }

    private void filter() {
        Collections.sort(filteredThumbCells, new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                ThumbCell c1 = (ThumbCell) o1;
                ThumbCell c2 = (ThumbCell) o2;

                if(c1.getIndex() < c2.getIndex()) return -1;
                if(c1.getIndex() > c2.getIndex()) return 1;
                return c1.getModel().getName().compareTo(c2.getModel().getName());
            }
        });

        if (filter == null || filter.isEmpty()) {
            for (ThumbCell thumbCell : filteredThumbCells) {
                insert(thumbCell);
            }
            filteredThumbCells.clear();
            return;
        }

        // remove the ones from grid, which don't match
        for (Iterator<Node> iterator = grid.getChildren().iterator(); iterator.hasNext();) {
            Node node = iterator.next();
            ThumbCell cell = (ThumbCell) node;
            Model m = cell.getModel();
            if(!matches(m, filter)) {
                iterator.remove();
                filteredThumbCells.add(cell);
                cell.setSelected(false);
            }
        }

        // add the ones, which might have been filtered before, but now match
        for (Iterator<ThumbCell> iterator = filteredThumbCells.iterator(); iterator.hasNext();) {
            ThumbCell thumbCell = iterator.next();
            Model m = thumbCell.getModel();
            if(matches(m, filter)) {
                iterator.remove();
                insert(thumbCell);
            }
        }
    }

    private void moveActiveRecordingsToFront() {
        List<Node> thumbsToMove = new ArrayList<>();
        ObservableList<Node> thumbs = grid.getChildren();
        for (int i = thumbs.size()-1; i > 0; i--) {
            ThumbCell thumb = (ThumbCell) thumbs.get(i);
            if(recorder.isRecording(thumb.getModel())) {
                thumbs.remove(i);
                thumbsToMove.add(0, thumb);
            }
        }
        thumbs.addAll(0, thumbsToMove);
    }

    private void insert(ThumbCell thumbCell) {
        if(grid.getChildren().contains(thumbCell)) {
            return;
        }

        if(thumbCell.getIndex() < grid.getChildren().size()-1) {
            grid.getChildren().add(thumbCell.getIndex(), thumbCell);
        } else {
            grid.getChildren().add(thumbCell);
        }
    }

    private boolean matches(Model m, String filter) {
        String[] tokens = filter.split(" ");
        StringBuilder searchTextBuilder = new StringBuilder(m.getName());
        searchTextBuilder.append(' ');
        for (String tag : m.getTags()) {
            searchTextBuilder.append(tag).append(' ');
        }
        searchTextBuilder.append(m.getStreamResolution());
        String searchText = searchTextBuilder.toString().trim();
        //LOG.debug("{} -> {}", m.getName(), searchText);
        boolean tokensMissing = false;
        for (String token : tokens) {
            if(token.matches(">\\d+")) {
                int res = Integer.parseInt(token.substring(1));
                if(m.getStreamResolution() < res) {
                    tokensMissing = true;
                }
            } else if(token.matches("<\\d+")) {
                int res = Integer.parseInt(token.substring(1));
                if(m.getStreamResolution() > res) {
                    tokensMissing = true;
                }
            } else if(!searchText.contains(token)) {
                tokensMissing = true;
            }
        }
        return !tokensMissing;
    }

    private ScheduledService<List<Model>> createUpdateService() {
        ScheduledService<List<Model>> updateService = new ScheduledService<List<Model>>() {
            @Override
            protected Task<List<Model>> createTask() {
                return new Task<List<Model>>() {
                    @Override
                    public List<Model> call() throws IOException {
                        String url = ThumbOverviewTab.this.url + "?page="+page+"&keywords=&_=" + System.currentTimeMillis();
                        LOG.debug("Fetching page {}", url);
                        Request request = new Request.Builder().url(url).build();
                        Response response = client.execute(request, loginRequired);
                        if (response.isSuccessful()) {
                            List<Model> models = ModelParser.parseModels(response.body().string());
                            response.close();
                            return models;
                        } else {
                            int code = response.code();
                            response.close();
                            throw new IOException("HTTP status " + code);
                        }
                    }
                };
            }
        };
        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("ThumbOverviewTab UpdateService");
                return t;
            }
        });
        updateService.setExecutor(executor);
        return updateService;
    }

    public void setRecorder(Recorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void selected() {
        if(updateService != null) {
            State s = updateService.getState();
            if (s != State.SCHEDULED && s != State.RUNNING) {
                updateService.reset();
                updateService.restart();
            }
        }
    }

    @Override
    public void deselected() {
        if(updateService != null) {
            updateService.cancel();
        }
    }

    void suspendUpdates(boolean suspend) {
        this.updatesSuspended = suspend;
    }

    private void removeSelection() {
        while(selectedThumbCells.size() > 0) {
            selectedThumbCells.get(0).setSelected(false);
        }
    }
}
