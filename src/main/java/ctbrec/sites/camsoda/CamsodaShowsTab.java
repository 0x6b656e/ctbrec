package ctbrec.sites.camsoda;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Model;
import ctbrec.recorder.Recorder;
import ctbrec.ui.AutosizeAlert;
import ctbrec.ui.DesktopIntegration;
import ctbrec.ui.TabSelectionListener;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import okhttp3.Request;
import okhttp3.Response;

public class CamsodaShowsTab extends Tab implements TabSelectionListener {

    private static final transient Logger LOG = LoggerFactory.getLogger(CamsodaShowsTab.class);

    private Camsoda camsoda;
    private Recorder recorder;
    private GridPane showList;
    private ProgressIndicator progressIndicator;

    public CamsodaShowsTab(Camsoda camsoda, Recorder recorder) {
        this.camsoda = camsoda;
        this.recorder = recorder;
        createGui();
    }

    private void createGui() {
        showList = new GridPane();
        showList.setPadding(new Insets(5));
        showList.setHgap(5);
        showList.setVgap(5);
        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(100, 100);
        setContent(progressIndicator);
        setClosable(false);
        setText("Shows");
    }

    @Override
    public void selected() {
        Task<List<ShowBox>> task = new Task<List<ShowBox>>() {
            @Override
            protected List<ShowBox> call() throws Exception {
                String url = camsoda.getBaseUrl() + "/api/v1/user/model_shows";
                Request req = new Request.Builder().url(url).build();
                try(Response response = camsoda.getHttpClient().execute(req)) {
                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(response.body().string());
                        if (json.optInt("success") == 1) {
                            List<ShowBox> boxes = new ArrayList<>();
                            JSONArray results = json.getJSONArray("results");
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject result = results.getJSONObject(i);
                                String modelUrl = camsoda.getBaseUrl() + result.getString("url");
                                String name = modelUrl.substring(modelUrl.lastIndexOf('/') + 1);
                                Model model = camsoda.createModel(name);
                                ZonedDateTime startTime = parseUtcTime(result.getString("start"));
                                ZonedDateTime endTime = parseUtcTime(result.getString("end"));
                                boxes.add(new ShowBox(model, startTime, endTime));
                            }
                            return boxes;
                        } else {
                            LOG.error("Couldn't load upcoming camsoda shows. Unexpected response: {}", json.toString());
                            showErrorDialog("Oh no!", "Couldn't load upcoming CamSoda shows", "Got an unexpected response from server");
                        }
                    } else {
                        showErrorDialog("Oh no!", "Couldn't load upcoming CamSoda shows", "Got an unexpected response from server");
                        LOG.error("Couldn't load upcoming camsoda shows: {} {}", response.code(), response.message());
                    }
                }
                return Collections.emptyList();
            }

            private ZonedDateTime parseUtcTime(String string) {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
                TemporalAccessor ta = formatter.parse(string.replace(" UTC", ""));
                Instant instant = Instant.from(ta);
                return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
            }

            @Override
            protected void done() {
                super.done();
                Platform.runLater(() -> {
                    try {
                        List<ShowBox> boxes = get();
                        showList.getChildren().clear();
                        int index = 0;
                        for (ShowBox showBox : boxes) {
                            showList.add(showBox, index % 2, index++ / 2);
                            GridPane.setMargin(showBox, new Insets(20, 20, 0, 20));
                        }
                    } catch (Exception e) {
                        LOG.error("Couldn't load upcoming camsoda shows", e);
                    }
                    setContent(new ScrollPane(showList));
                });
            }
        };
        new Thread(task).start();
    }

    @Override
    public void deselected() {
    }

    private void showErrorDialog(String title, String head, String msg) {
        Platform.runLater(() -> {
            Alert alert = new AutosizeAlert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(head);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    private class ShowBox extends TitledPane {

        BorderPane root = new BorderPane();
        int thumbSize = 200;

        public ShowBox(Model model, ZonedDateTime startTime, ZonedDateTime endTime) {
            setText(model.getName());
            setPrefHeight(268);
            setContent(root);

            ImageView thumb = new ImageView();
            thumb.setPreserveRatio(true);
            thumb.setFitHeight(thumbSize);
            loadImage(model, thumb);
            root.setLeft(new ProgressIndicator());
            BorderPane.setMargin(thumb, new Insets(10, 30, 10, 10));

            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM);
            GridPane grid = new GridPane();

            grid.add(createLabel("Start", true), 0, 0);
            grid.add(createLabel(formatter.format(startTime), false), 1, 0);
            grid.add(createLabel("End", true), 0, 1);
            grid.add(createLabel(formatter.format(endTime), false), 1, 1);
            Button record = new Button("Record Model");
            record.setTooltip(new Tooltip(record.getText()));
            record.setOnAction((evt) -> record(model));
            grid.add(record, 1, 2);
            GridPane.setMargin(record, new Insets(10));
            Button follow = new Button("Follow");
            follow.setTooltip(new Tooltip(follow.getText()));
            follow.setOnAction((evt) -> follow(model));
            grid.add(follow, 1, 3);
            GridPane.setMargin(follow, new Insets(10));
            Button openInBrowser = new Button("Open in Browser");
            openInBrowser.setTooltip(new Tooltip(openInBrowser.getText()));
            openInBrowser.setOnAction((evt) -> DesktopIntegration.open(model.getUrl()));
            grid.add(openInBrowser, 1, 4);
            GridPane.setMargin(openInBrowser, new Insets(10));
            root.setCenter(grid);
            loadImage(model, thumb);

            record.minWidthProperty().bind(openInBrowser.widthProperty());
            follow.minWidthProperty().bind(openInBrowser.widthProperty());
        }

        private void follow(Model model) {
            setCursor(Cursor.WAIT);
            new Thread(() -> {
                try {
                    model.follow();
                } catch (Exception e) {
                    LOG.error("Couldn't follow model {}", model, e);
                    showErrorDialog("Oh no!", "Couldn't follow model", e.getMessage());
                } finally {
                    Platform.runLater(() -> {
                        setCursor(Cursor.DEFAULT);
                    });
                }
            }).start();
        }

        private void record(Model model) {
            setCursor(Cursor.WAIT);
            new Thread(() -> {
                try {
                    recorder.startRecording(model);
                } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException | IOException e) {
                    showErrorDialog("Oh no!", "Couldn't add model to the recorder", "Recorder error: " + e.getMessage());
                } finally {
                    Platform.runLater(() -> {
                        setCursor(Cursor.DEFAULT);
                    });
                }
            }).start();
        }

        private void loadImage(Model model, ImageView thumb) {
            new Thread(() -> {
                try {
                    String url = camsoda.getBaseUrl() + "/api/v1/user/" + model.getName();
                    Request detailRequest = new Request.Builder().url(url).build();
                    Response resp = camsoda.getHttpClient().execute(detailRequest);
                    if (resp.isSuccessful()) {
                        JSONObject json = new JSONObject(resp.body().string());
                        if (json.optBoolean("status") && json.has("user")) {
                            JSONObject user = json.getJSONObject("user");
                            if (user.has("settings")) {
                                JSONObject settings = user.getJSONObject("settings");
                                String imageUrl;
                                if(Objects.equals(System.getenv("CTBREC_DEV"), "1"))  {
                                    imageUrl = getClass().getResource("/image_not_found.png").toString();
                                } else {
                                    if (settings.has("offline_picture")) {
                                        imageUrl = settings.getString("offline_picture");
                                    } else {
                                        imageUrl = "https:" + user.getString("thumb");
                                    }
                                }
                                Platform.runLater(() -> {
                                    Image img = new Image(imageUrl, 1000, thumbSize, true, true, true);
                                    img.progressProperty().addListener(new ChangeListener<Number>() {
                                        @Override
                                        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                                            if (newValue.doubleValue() == 1.0) {
                                                thumb.setImage(img);
                                                root.setLeft(thumb);
                                            }
                                        }
                                    });

                                });
                            }
                        }
                    }
                    resp.close();
                } catch (Exception e) {
                    LOG.error("Couldn't load model details", e);
                }
            }).start();
        }

        private Node createLabel(String string, boolean bold) {
            Label label = new Label(string);
            label.setPadding(new Insets(10));
            Font def = Font.getDefault();
            label.setFont(Font.font(def.getFamily(), bold ? FontWeight.BOLD : FontWeight.NORMAL, 16));
            return label;
        }
    }
}
