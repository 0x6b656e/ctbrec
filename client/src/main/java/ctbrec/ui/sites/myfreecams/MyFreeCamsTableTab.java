package ctbrec.ui.sites.myfreecams;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.StringUtil;
import ctbrec.sites.mfc.MyFreeCams;
import ctbrec.sites.mfc.SessionState;
import ctbrec.ui.TabSelectionListener;
import ctbrec.ui.controls.SearchBox;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

public class MyFreeCamsTableTab extends Tab implements TabSelectionListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(MyFreeCamsTableTab.class);
    private ScrollPane scrollPane = new ScrollPane();
    private TableView<SessionState> table = new TableView<SessionState>();
    private ObservableList<SessionState> filteredModels = FXCollections.observableArrayList();
    private ObservableList<SessionState> observableModels = FXCollections.observableArrayList();
    private TableUpdateService updateService;
    private MyFreeCams mfc;
    private ReentrantLock lock = new ReentrantLock();
    private SearchBox filterInput;
    private Label count = new Label("models");
    private List<TableColumn<SessionState, ?>> columns = new ArrayList<>();

    public MyFreeCamsTableTab(MyFreeCams mfc) {
        this.mfc = mfc;
        setText("Tabular");
        setClosable(false);
        initUpdateService();
        createGui();
        restoreState();
        filter(filterInput.getText());
    }

    private void initUpdateService() {
        updateService = new TableUpdateService(mfc);
        updateService.setPeriod(new Duration(TimeUnit.SECONDS.toMillis(1)));
        updateService.setOnSucceeded(this::onSuccess);
        updateService.setOnFailed((event) -> {
            LOG.info("Couldn't update MyFreeCams model table", event.getSource().getException());
        });
    }

    private void onSuccess(WorkerStateEvent evt) {
        Collection<SessionState> sessionStates = updateService.getValue();
        if (sessionStates == null) {
            return;
        }

        lock.lock();
        try {
            for (SessionState updatedModel : sessionStates) {
                int index = observableModels.indexOf(updatedModel);
                if (index == -1) {
                    observableModels.add(updatedModel);
                } else {
                    observableModels.set(index, updatedModel);
                }
            }

            for (Iterator<SessionState> iterator = observableModels.iterator(); iterator.hasNext();) {
                SessionState model = iterator.next();
                if (!sessionStates.contains(model)) {
                    iterator.remove();
                }
            }
        } finally {
            lock.unlock();
        }

        filteredModels.clear();
        filter(filterInput.getText());
        table.sort();
    }

    private void createGui() {
        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(5, 10, 10, 10));

        filterInput = new SearchBox(false);
        filterInput.setPromptText("Filter");
        filterInput.textProperty().addListener( (observableValue, oldValue, newValue) -> {
            String filter = filterInput.getText();
            Config.getInstance().getSettings().mfcModelsTableFilter = filter;
            lock.lock();
            try {
                filter(filter);
            } finally {
                lock.unlock();
            }
        });
        filterInput.getStyleClass().remove("search-box-icon");
        HBox.setHgrow(filterInput, Priority.ALWAYS);
        Button columnSelection = new Button("⚙");
        //Button columnSelection = new Button("⩩");
        columnSelection.setOnAction(this::showColumnSelection);
        HBox topBar = new HBox(5);
        topBar.getChildren().addAll(filterInput, count, columnSelection);
        count.prefHeightProperty().bind(filterInput.heightProperty());
        count.setAlignment(Pos.CENTER);
        layout.setTop(topBar);
        BorderPane.setMargin(topBar, new Insets(0, 0, 5, 0));

        table.setItems(observableModels);
        table.getSortOrder().addListener(createSortOrderChangedListener());

        TableColumn<SessionState, String> name = createTableColumn("Name", 200, 0);
        name.setCellValueFactory(cdf -> {
            return new SimpleStringProperty(Optional.ofNullable(cdf.getValue().getNm()).orElse("n/a"));
        });
        addTableColumnIfEnabled(name);

        TableColumn<SessionState, String> state = createTableColumn("State", 130, 1);
        state.setCellValueFactory(cdf -> {
            String st = Optional.ofNullable(cdf.getValue().getVs()).map(vs -> ctbrec.sites.mfc.State.of(vs).toString()).orElse("n/a");
            return new SimpleStringProperty(st);
        });
        addTableColumnIfEnabled(state);

        TableColumn<SessionState, Number> camscore = createTableColumn("Score", 75, 2);
        camscore.setCellValueFactory(cdf -> {
            Double camScore = Optional.ofNullable(cdf.getValue().getM()).map(m -> m.getCamscore()).orElse(0d);
            return new SimpleDoubleProperty(camScore);
        });
        addTableColumnIfEnabled(camscore);

        TableColumn<SessionState, String> ethnic = createTableColumn("Ethnicity", 130, 3);
        ethnic.setCellValueFactory(cdf -> {
            String eth = Optional.ofNullable(cdf.getValue().getU()).map(u -> u.getEthnic()).orElse("n/a");
            return new SimpleStringProperty(eth);
        });
        addTableColumnIfEnabled(ethnic);

        TableColumn<SessionState, String> country = createTableColumn("Country", 160, 4);
        country.setCellValueFactory(cdf -> {
            String c = Optional.ofNullable(cdf.getValue().getU()).map(u -> u.getCountry()).orElse("n/a");
            return new SimpleStringProperty(c);
        });
        addTableColumnIfEnabled(country);

        TableColumn<SessionState, String> continent = createTableColumn("Continent", 100, 5);
        continent.setCellValueFactory(cdf -> {
            String c = Optional.ofNullable(cdf.getValue().getM()).map(m -> m.getContinent()).orElse("n/a");
            return new SimpleStringProperty(c);
        });
        addTableColumnIfEnabled(continent);

        TableColumn<SessionState, String> occupation = createTableColumn("Occupation", 160, 6);
        occupation.setCellValueFactory(cdf -> {
            String occ = Optional.ofNullable(cdf.getValue().getU()).map(u -> u.getOccupation()).orElse("n/a");
            return new SimpleStringProperty(occ);
        });
        addTableColumnIfEnabled(occupation);

        TableColumn<SessionState, String> tags = createTableColumn("Tags", 300, 7);
        tags.setCellValueFactory(cdf -> {
            Set<String> tagSet = Optional.ofNullable(cdf.getValue().getM()).map(m -> m.getTags()).orElse(Collections.emptySet());
            if(tagSet.isEmpty()) {
                return new SimpleStringProperty("");
            } else {
                StringBuilder sb = new StringBuilder();
                for (String t : tagSet) {
                    sb.append(t).append(',').append(' ');
                }
                return new SimpleStringProperty(sb.substring(0, sb.length()-2));
            }
        });
        addTableColumnIfEnabled(tags);

        TableColumn<SessionState, String> blurp = createTableColumn("Blurp", 300, 8);
        blurp.setCellValueFactory(cdf -> {
            String blrp = Optional.ofNullable(cdf.getValue().getU()).map(u -> u.getBlurb()).orElse("n/a");
            return new SimpleStringProperty(blrp);
        });
        addTableColumnIfEnabled(blurp);

        TableColumn<SessionState, String> topic = createTableColumn("Topic", 600, 9);
        topic.setCellValueFactory(cdf -> {
            String tpc = Optional.ofNullable(cdf.getValue().getM()).map(m -> m.getTopic()).orElse("n/a");
            try {
                tpc = URLDecoder.decode(tpc, "utf-8");
            } catch (UnsupportedEncodingException e) {
                LOG.warn("Couldn't url decode topic", e);
            }
            return new SimpleStringProperty(tpc);
        });
        addTableColumnIfEnabled(topic);


        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setContent(table);
        scrollPane.setStyle("-fx-background-color: -fx-background");
        layout.setCenter(scrollPane);
        setContent(layout);
    }

    private void addTableColumnIfEnabled(TableColumn<SessionState, ?> tc) {
        if(isColumnEnabled(tc)) {
            table.getColumns().add(tc);
        }
    }

    private void filter(String filter) {
        lock.lock();
        try {
            if (StringUtil.isBlank(filter)) {
                observableModels.addAll(filteredModels);
                filteredModels.clear();
                return;
            }

            String[] tokens = filter.split(" ");
            observableModels.addAll(filteredModels);
            filteredModels.clear();
            for (int i = 0; i < table.getItems().size(); i++) {
                StringBuilder sb = new StringBuilder();
                for (TableColumn<SessionState, ?> tc : table.getColumns()) {
                    String cellData = tc.getCellData(i).toString();
                    sb.append(cellData).append(' ');
                }
                String searchText = sb.toString();

                boolean tokensMissing = false;
                for (String token : tokens) {
                    if(!searchText.toLowerCase().contains(token.toLowerCase())) {
                        tokensMissing = true;
                        break;
                    }
                }
                if(tokensMissing) {
                    SessionState sessionState = table.getItems().get(i);
                    filteredModels.add(sessionState);
                }
            }
            observableModels.removeAll(filteredModels);
        } finally {
            lock.unlock();
            int filtered = filteredModels.size();
            int showing = observableModels.size();
            int total = showing + filtered;
            count.setText(showing + "/" + total);
        }
    }

    private void showColumnSelection(ActionEvent evt) {
        ContextMenu menu = new ContextMenu();
        for (TableColumn<SessionState, ?> tc : columns) {
            CheckMenuItem item = new CheckMenuItem(tc.getText());
            item.setSelected(isColumnEnabled(tc));
            menu.getItems().add(item);
            item.setOnAction(e -> {
                if(item.isSelected()) {
                    Config.getInstance().getSettings().mfcDisabledModelsTableColumns.remove(tc.getText());
                    for (int i = table.getColumns().size()-1; i>=0; i--) {
                        TableColumn<SessionState, ?> other = table.getColumns().get(i);
                        int idx = (int) tc.getUserData();
                        int otherIdx = (int) other.getUserData();
                        if(otherIdx < idx) {
                            table.getColumns().add(i+1, tc);
                            break;
                        }
                    }
                } else {
                    Config.getInstance().getSettings().mfcDisabledModelsTableColumns.add(tc.getText());
                    table.getColumns().remove(tc);
                }
            });
        }
        Button src = (Button) evt.getSource();
        Point2D location = src.localToScreen(src.getTranslateX(), src.getTranslateY());
        menu.show(getTabPane().getScene().getWindow(), location.getX(), location.getY() + src.getHeight() + 5);
    }

    private boolean isColumnEnabled(TableColumn<SessionState, ?> tc) {
        return !Config.getInstance().getSettings().mfcDisabledModelsTableColumns.contains(tc.getText());
    }

    private <T> TableColumn<SessionState, T> createTableColumn(String text, int width, int idx) {
        TableColumn<SessionState, T> tc = new TableColumn<>(text);
        tc.setPrefWidth(width);
        tc.sortTypeProperty().addListener((obs, o, n) -> saveState());
        tc.widthProperty().addListener((obs, o, n) -> saveState());
        tc.setUserData(idx);
        columns.add(tc);
        return tc;
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

    private void saveState() {
        if (!table.getSortOrder().isEmpty()) {
            TableColumn<SessionState, ?> col = table.getSortOrder().get(0);
            Config.getInstance().getSettings().mfcModelsTableSortColumn = col.getText();
            Config.getInstance().getSettings().mfcModelsTableSortType = col.getSortType().toString();
        }
        double[] columnWidths = new double[table.getColumns().size()];
        for (int i = 0; i < columnWidths.length; i++) {
            columnWidths[i] = table.getColumns().get(i).getWidth();
        }
        Config.getInstance().getSettings().mfcModelsTableColumnWidths = columnWidths;
    };

    private void restoreState() {
        String sortCol = Config.getInstance().getSettings().mfcModelsTableSortColumn;
        if (StringUtil.isNotBlank(sortCol)) {
            for (TableColumn<SessionState, ?> col : table.getColumns()) {
                if (Objects.equals(sortCol, col.getText())) {
                    col.setSortType(SortType.valueOf(Config.getInstance().getSettings().mfcModelsTableSortType));
                    table.getSortOrder().clear();
                    table.getSortOrder().add(col);
                    break;
                }
            }
        }

        double[] columnWidths = Config.getInstance().getSettings().mfcModelsTableColumnWidths;
        if (columnWidths != null && columnWidths.length == table.getColumns().size()) {
            for (int i = 0; i < columnWidths.length; i++) {
                table.getColumns().get(i).setPrefWidth(columnWidths[i]);
            }
        }

        filterInput.setText(Config.getInstance().getSettings().mfcModelsTableFilter);
    }

    private ListChangeListener<TableColumn<SessionState, ?>> createSortOrderChangedListener() {
        return new ListChangeListener<TableColumn<SessionState, ?>>() {
            @Override
            public void onChanged(Change<? extends TableColumn<SessionState, ?>> c) {
                saveState();
            }
        };
    }
}
