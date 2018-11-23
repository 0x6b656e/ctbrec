package ctbrec.ui;


import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Model;
import ctbrec.recorder.Recorder;
import ctbrec.sites.Site;
import ctbrec.sites.mfc.MyFreeCamsClient;
import ctbrec.sites.mfc.MyFreeCamsModel;
import ctbrec.ui.controls.SearchBox;
import ctbrec.ui.controls.SearchPopover;
import ctbrec.ui.controls.SearchPopoverTreeList;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import javafx.util.Duration;

public class ThumbOverviewTab extends Tab implements TabSelectionListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(ThumbOverviewTab.class);

    protected static BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    static ExecutorService threadPool = new ThreadPoolExecutor(2, 2, 10, TimeUnit.MINUTES, queue);
    static Set<Model> resolutionProcessing = Collections.synchronizedSet(new HashSet<>());

    protected FlowPane grid = new FlowPane();
    protected PaginatedScheduledService updateService;
    protected HBox pagination;
    protected List<ThumbCell> selectedThumbCells = Collections.synchronizedList(new ArrayList<>());

    List<ThumbCell> filteredThumbCells = Collections.synchronizedList(new ArrayList<>());
    Recorder recorder;
    String filter;
    ReentrantLock gridLock = new ReentrantLock();
    ScrollPane scrollPane = new ScrollPane();
    boolean loginRequired;
    TextField pageInput = new TextField(Integer.toString(1));
    Button pagePrev = new Button("◀");
    Button pageNext = new Button("▶");
    private volatile boolean updatesSuspended = false;
    ContextMenu popup;
    Site site;
    StackPane root = new StackPane();
    Task<List<Model>> searchTask;
    SearchPopover popover;
    SearchPopoverTreeList popoverTreelist = new SearchPopoverTreeList();

    private ComboBox<Integer> thumbWidth;

    public ThumbOverviewTab(String title, PaginatedScheduledService updateService, Site site) {
        super(title);
        this.updateService = updateService;
        this.site = site;
        setClosable(false);
        createGui();
        initializeUpdateService();
    }

    protected void createGui() {
        grid.setPadding(new Insets(5));
        grid.setHgap(5);
        grid.setVgap(5);

        SearchBox filterInput = new SearchBox(false);
        filterInput.setPromptText("Filter models on this page");
        filterInput.textProperty().addListener( (observableValue, oldValue, newValue) -> {
            filter = filterInput.getText();
            gridLock.lock();
            try {
                filter();
                moveActiveRecordingsToFront();
            } finally {
                gridLock.unlock();
            }
        });
        Tooltip filterTooltip = new Tooltip("Filter the models by their name, stream description or #hashtags.\n\n"
                + "If the display of stream resolution is enabled, you can even filter for public rooms or by resolution.\n\n"
                + "Try \"1080\" or \">720\" or \"public\"");
        filterInput.setTooltip(filterTooltip);
        filterInput.getStyleClass().remove("search-box-icon");
        BorderPane.setMargin(filterInput, new Insets(5));

        SearchBox searchInput = new SearchBox();
        searchInput.setPromptText("Search Model");
        searchInput.prefWidth(200);
        searchInput.textProperty().addListener(search());
        searchInput.addEventHandler(KeyEvent.KEY_PRESSED, evt -> {
            if(evt.getCode() == KeyCode.ESCAPE) {
                popover.hide();
            }
        });
        BorderPane.setMargin(searchInput, new Insets(5));

        popover = new SearchPopover();
        popover.maxWidthProperty().bind(popover.minWidthProperty());
        popover.prefWidthProperty().bind(popover.minWidthProperty());
        popover.setMinWidth(400);
        popover.maxHeightProperty().bind(popover.minHeightProperty());
        popover.prefHeightProperty().bind(popover.minHeightProperty());
        popover.setMinHeight(400);
        popover.pushPage(popoverTreelist);
        StackPane.setAlignment(popover, Pos.TOP_RIGHT);
        StackPane.setMargin(popover, new Insets(50, 50, 0, 0));

        HBox topBar = new HBox(5);
        HBox.setHgrow(filterInput, Priority.ALWAYS);
        topBar.getChildren().add(filterInput);
        if(site.supportsSearch()) {
            topBar.getChildren().add(searchInput);
        }

        scrollPane.setContent(grid);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        BorderPane.setMargin(scrollPane, new Insets(5));

        pagination = new HBox(5);
        pagination.getChildren().add(pagePrev);
        pagination.getChildren().add(pageNext);
        pagination.getChildren().add(pageInput);
        BorderPane.setMargin(pagination, new Insets(5));
        pageInput.setPrefWidth(50);
        pageInput.setOnAction((e) -> handlePageNumberInput());
        pagePrev.setOnAction((e) -> {
            int page = updateService.getPage();
            page = Math.max(1, --page);
            pageInput.setText(Integer.toString(page));
            updateService.setPage(page);
            restartUpdateService();
        });
        pageNext.setOnAction((e) -> {
            int page = updateService.getPage();
            page++;
            pageInput.setText(Integer.toString(page));
            updateService.setPage(page);
            restartUpdateService();
        });

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
        thumbWidth = new ComboBox<>(FXCollections.observableList(thumbWidths));
        thumbWidth.getSelectionModel().select(Integer.valueOf(Config.getInstance().getSettings().thumbWidth));
        thumbWidth.setOnAction((e) -> {
            int width = thumbWidth.getSelectionModel().getSelectedItem();
            Config.getInstance().getSettings().thumbWidth = width;
            updateThumbSize();
        });
        thumbSizeSelector.getChildren().add(thumbWidth);
        BorderPane.setMargin(thumbSizeSelector, new Insets(5));


        BorderPane bottomPane = new BorderPane();
        bottomPane.setLeft(pagination);
        bottomPane.setRight(thumbSizeSelector);

        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(5));
        borderPane.setTop(topBar);
        borderPane.setCenter(scrollPane);
        borderPane.setBottom(bottomPane);

        root.getChildren().add(borderPane);
        root.getChildren().add(popover);
        setContent(root);
    }

    private ChangeListener<? super String> search() {
        return (observableValue, oldValue, newValue) -> {
            if(searchTask != null) {
                searchTask.cancel(true);
            }

            if(newValue.length() < 2) {
                return;
            }


            searchTask = new Task<List<Model>>() {
                @Override
                protected List<Model> call() throws Exception {
                    if(site.searchRequiresLogin()) {
                        boolean loggedin = false;
                        try {
                            loggedin = SiteUiFactory.getUi(site).login();
                        } catch (IOException e) {
                            loggedin = false;
                        }
                        if(!loggedin) {
                            showError("Login failed", "Search won't work correctly without login", null);
                        }
                    }
                    return site.search(newValue);
                }

                @Override
                protected void failed() {
                    LOG.error("Search failed", getException());
                }

                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        List<Model> models = getValue();
                        LOG.debug("Search result {} {}", isCancelled(), models);
                        if(models.isEmpty()) {
                            popover.hide();
                        } else {
                            popoverTreelist.getItems().clear();
                            for (Model model : getValue()) {
                                popoverTreelist.getItems().add(model);
                            }
                            popover.show();
                        }
                    });
                }
            };
            new Thread(searchTask).start();
        };
    }

    private void updateThumbSize() {
        int width = Config.getInstance().getSettings().thumbWidth;
        thumbWidth.getSelectionModel().select(Integer.valueOf(width));
        for (Node node : grid.getChildren()) {
            if(node instanceof ThumbCell) {
                ThumbCell cell = (ThumbCell) node;
                cell.setThumbWidth(width);
            }
        }
        for (ThumbCell cell : filteredThumbCells) {
            cell.setThumbWidth(width);
        }
    }

    private void handlePageNumberInput() {
        try {
            int page = Integer.parseInt(pageInput.getText());
            page = Math.max(1, page);
            updateService.setPage(page);
            restartUpdateService();
        } catch(NumberFormatException e) {
        } finally {
            pageInput.setText(Integer.toString(updateService.getPage()));
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
        updateService.setPeriod(new Duration(TimeUnit.SECONDS.toMillis(10)));
        updateService.setOnSucceeded((event) -> onSuccess());
        updateService.setOnFailed((event) -> onFail(event));
    }

    protected void onSuccess() {
        if(updatesSuspended) {
            return;
        }
        List<Model> models = updateService.getValue();
        updateGrid(models);

    }

    protected void updateGrid(List<? extends Model> models) {
        gridLock.lock();
        try {
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
                    ThumbCell newCell = createThumbCell(this, model, recorder);
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

    ThumbCell createThumbCell(ThumbOverviewTab thumbOverviewTab, Model model, Recorder recorder) {
        ThumbCell newCell = new ThumbCell(this, model, recorder);
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
        openInPlayer.setOnAction((e) -> startPlayer(getSelectedThumbCells(cell)));

        MenuItem start = new MenuItem("Start Recording");
        start.setOnAction((e) -> startStopAction(getSelectedThumbCells(cell), true));
        MenuItem stop = new MenuItem("Stop Recording");
        stop.setOnAction((e) -> startStopAction(getSelectedThumbCells(cell), false));
        MenuItem startStop = recorder.isRecording(cell.getModel()) ? stop : start;

        MenuItem pause = new MenuItem("Pause Recording");
        pause.setOnAction((e) -> pauseResumeAction(getSelectedThumbCells(cell), true));
        MenuItem resume = new MenuItem("Resume Recording");
        resume.setOnAction((e) -> pauseResumeAction(getSelectedThumbCells(cell), false));
        MenuItem pauseResume = recorder.isSuspended(cell.getModel()) ? resume : pause;

        MenuItem follow = new MenuItem("Follow");
        follow.setOnAction((e) -> follow(getSelectedThumbCells(cell), true));
        MenuItem unfollow = new MenuItem("Unfollow");
        unfollow.setOnAction((e) -> follow(getSelectedThumbCells(cell), false));

        MenuItem copyUrl = new MenuItem("Copy URL");
        copyUrl.setOnAction((e) -> {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(cell.getModel().getUrl());
            clipboard.setContent(content);
        });

        MenuItem sendTip = new MenuItem("Send Tip");
        sendTip.setOnAction((e) -> {
            TipDialog tipDialog = new TipDialog(site, cell.getModel());
            tipDialog.showAndWait();
            String tipText = tipDialog.getResult();
            if(tipText != null) {
                if(tipText.matches("[1-9]\\d*")) {
                    int tokens = Integer.parseInt(tipText);
                    try {
                        SiteUiFactory.getUi(site).login();
                        cell.getModel().receiveTip(tokens);
                        Map<String, Object> event = new HashMap<>();
                        event.put("event", "tokens.sent");
                        event.put("amount", tokens);
                        CamrecApplication.bus.post(event);
                    } catch (Exception e1) {
                        showError("Couldn't send tip", "An error occured while sending tip:", e1);
                    }
                } else {
                    showError("Couldn't send tip", "You entered an invalid amount of tokens", null);
                }
            }
        });
        sendTip.setDisable(!site.credentialsAvailable());

        // check, if other cells are selected, too. in that case, we have to disable menu item, which make sense only for
        // single selections. but only do that, if the popup has been triggered on a selected cell. otherwise remove the
        // selection and show the normal menu
        if (selectedThumbCells.size() > 1 || selectedThumbCells.size() == 1 && selectedThumbCells.get(0) != cell) {
            if(cell.isSelected()) {
                if(Config.getInstance().getSettings().singlePlayer) {
                    openInPlayer.setDisable(true);
                }
                copyUrl.setDisable(true);
                sendTip.setDisable(true);
            } else {
                removeSelection();
            }
        }

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setAutoHide(true);
        contextMenu.setHideOnEscape(true);
        contextMenu.setAutoFix(true);
        contextMenu.getItems().addAll(openInPlayer, startStop);
        if(recorder.isRecording(cell.getModel())) {
            contextMenu.getItems().add(pauseResume);
        }
        if(site.supportsFollow()) {
            MenuItem followOrUnFollow = (this instanceof FollowedTab) ? unfollow : follow;
            followOrUnFollow.setDisable(!site.credentialsAvailable());
            contextMenu.getItems().add(followOrUnFollow);
        }
        if(site.supportsTips()) {
            contextMenu.getItems().add(sendTip);
        }
        contextMenu.getItems().addAll(copyUrl);
        if(cell.getModel() instanceof MyFreeCamsModel && Objects.equals(System.getenv("CTBREC_DEV"), "1")) {
            MenuItem debug = new MenuItem("debug");
            debug.setOnAction((e) -> {
                MyFreeCamsClient.getInstance().getSessionState(cell.getModel());
            });
            contextMenu.getItems().add(debug);
        }
        return contextMenu;
    }

    private List<ThumbCell> getSelectedThumbCells(ThumbCell cell) {
        if(selectedThumbCells.isEmpty()) {
            return Collections.singletonList(cell);
        } else {
            return selectedThumbCells;
        }
    }

    protected void follow(List<ThumbCell> selection, boolean follow) {
        for (ThumbCell thumbCell : selection) {
            thumbCell.follow(follow).thenAccept((success) -> {
                if(follow && success) {
                    showAddToFollowedAnimation(thumbCell);
                }
            });
        }
        if(!follow) {
            selectedThumbCells.clear();
        }
    }

    private void showAddToFollowedAnimation(ThumbCell thumbCell) {
        Platform.runLater(() -> {
            Transform tx = thumbCell.getLocalToParentTransform();
            ImageView iv = new ImageView();
            iv.setFitWidth(thumbCell.getWidth());
            root.getChildren().add(iv);
            StackPane.setAlignment(iv, Pos.TOP_LEFT);
            iv.setImage(thumbCell.getImage());
            double scrollPaneTopLeft = scrollPane.getVvalue() * (grid.getHeight() - scrollPane.getViewportBounds().getHeight());
            double offsetInViewPort = tx.getTy() - scrollPaneTopLeft;
            int duration = 500;
            TranslateTransition translate = new TranslateTransition(Duration.millis(duration), iv);
            translate.setFromX(0);
            translate.setFromY(0);
            translate.setByX(-tx.getTx() - 200);
            TabProvider tabProvider = SiteUiFactory.getUi(site).getTabProvider();
            Tab followedTab = tabProvider.getFollowedTab();
            translate.setByY(-offsetInViewPort + getFollowedTabYPosition(followedTab));
            StackPane.setMargin(iv, new Insets(offsetInViewPort, 0, 0, tx.getTx()));
            translate.setInterpolator(Interpolator.EASE_BOTH);
            FadeTransition fade = new FadeTransition(Duration.millis(duration), iv);
            fade.setFromValue(1);
            fade.setToValue(.3);
            ScaleTransition scale = new ScaleTransition(Duration.millis(duration), iv);
            scale.setToX(0.1);
            scale.setToY(0.1);
            ParallelTransition pt = new ParallelTransition(translate, scale);
            pt.play();
            pt.setOnFinished((evt) -> {
                root.getChildren().remove(iv);
            });

            String normalStyle = followedTab.getStyle();
            Color normal = Color.web("#f4f4f4");
            Color highlight = Color.web("#2b8513");
            Transition blink = new Transition() {
                {
                    setCycleDuration(Duration.millis(500));
                }
                @Override
                protected void interpolate(double frac) {
                    double rh = highlight.getRed();
                    double rn = normal.getRed();
                    double diff = rh - rn;
                    double r = (rn + diff * frac) * 255;
                    double gh = highlight.getGreen();
                    double gn = normal.getGreen();
                    diff = gh - gn;
                    double g = (gn + diff * frac) * 255;
                    double bh = highlight.getBlue();
                    double bn = normal.getBlue();
                    diff = bh - bn;
                    double b = (bn + diff * frac) * 255;
                    String style = "-fx-background-color: rgb(" + r + "," + g + "," + b + ")";
                    followedTab.setStyle(style);
                }
            };
            blink.setCycleCount(6);
            blink.setAutoReverse(true);
            blink.setOnFinished((evt) -> followedTab.setStyle(normalStyle));
            blink.play();
        });
    }

    private double getFollowedTabYPosition(Tab followedTab) {
        TabPane tabPane = getTabPane();
        int idx = tabPane.getTabs().indexOf(followedTab);
        for (Node node : tabPane.getChildrenUnmodifiable()) {
            Parent p = (Parent) node;
            for (Node child : p.getChildrenUnmodifiable()) {
                if(child.getStyleClass().contains("headers-region")) {
                    Parent tabContainer = (Parent) child;
                    Node tab = tabContainer.getChildrenUnmodifiable().get(tabContainer.getChildrenUnmodifiable().size() - idx - 1);
                    return tab.getLayoutX() - 85;
                }
            }
        }
        return 0;
    }

    private void startStopAction(List<ThumbCell> selection, boolean start) {
        for (ThumbCell thumbCell : selection) {
            thumbCell.startStopAction(start);
        }
    }

    private void pauseResumeAction(List<ThumbCell> selection, boolean pause) {
        for (ThumbCell thumbCell : selection) {
            thumbCell.pauseResumeAction(pause);
        }
    }

    private void startPlayer(List<ThumbCell> selection) {
        for (ThumbCell thumbCell : selection) {
            thumbCell.startPlayer();
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
            LOG.error("Couldn't update model list", event.getSource().getException());
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
        try {
            String[] tokens = filter.split(" ");
            StringBuilder searchTextBuilder = new StringBuilder(m.getName());
            searchTextBuilder.append(' ');
            for (String tag : m.getTags()) {
                searchTextBuilder.append(tag).append(' ');
            }
            int[] resolution = m.getStreamResolution(true);
            searchTextBuilder.append(resolution[1]);
            String searchText = searchTextBuilder.toString().trim();
            boolean tokensMissing = false;
            for (String token : tokens) {
                if(token.matches(">\\d+")) {
                    int res = Integer.parseInt(token.substring(1));
                    if(resolution[1] < res) {
                        tokensMissing = true;
                    }
                } else if(token.matches("<\\d+")) {
                    int res = Integer.parseInt(token.substring(1));
                    if(resolution[1] > res) {
                        tokensMissing = true;
                    }
                } else if(token.equals("public")) {
                    if(!m.getOnlineState(true).equals(token)) {
                        tokensMissing = true;
                    }
                } else if(!searchText.toLowerCase().contains(token.toLowerCase())) {
                    tokensMissing = true;
                }
            }
            return !tokensMissing;
        } catch (NumberFormatException | ExecutionException | IOException e) {
            LOG.error("Error while filtering model list", e);
            return false;
        }
    }

    public void setRecorder(Recorder recorder) {
        this.recorder = recorder;
        popoverTreelist.setRecorder(recorder);
    }

    @Override
    public void selected() {
        queue.clear();
        if(updateService != null) {
            State s = updateService.getState();
            if (s != State.SCHEDULED && s != State.RUNNING) {
                updateService.reset();
                updateService.restart();
            }
        }
        updateThumbSize();
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

    private void showError(String header, String text, Exception e) {
        Runnable r = () -> {
            Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(header);
            String content = text;
            if(e != null) {
                content += " " + e.getLocalizedMessage();
            }
            alert.setContentText(content);
            alert.showAndWait();
        };

        if(Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
