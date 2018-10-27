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

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Model;
import ctbrec.recorder.Recorder;
import ctbrec.ui.AutosizeAlert;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import okhttp3.Request;
import okhttp3.Response;

public class CamsodaShowsTab extends Tab implements TabSelectionListener {

    private static final transient Logger LOG = LoggerFactory.getLogger(CamsodaShowsTab.class);

    private Camsoda camsoda;
    private Recorder recorder;
    private VBox showList;
    private ProgressIndicator progressIndicator;

    public CamsodaShowsTab(Camsoda camsoda, Recorder recorder) {
        this.camsoda = camsoda;
        this.recorder = recorder;
        createGui();
    }

    private void createGui() {
        showList = new VBox(10);
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
                Response response = camsoda.getHttpClient().execute(req);
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
                    response.close();
                    showErrorDialog("Oh no!", "Couldn't load upcoming CamSoda shows", "Got an unexpected response from server");
                    LOG.error("Couldn't load upcoming camsoda shows: {} {}", response.code(), response.message());
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
                        for (ShowBox showBox : boxes) {
                            showList.getChildren().add(showBox);
                            VBox.setMargin(showBox, new Insets(20));
                        }
                    } catch (Exception e) {
                        LOG.error("Couldn't load upcoming camsoda shows", e);
                    }
                    setContent(showList);
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
            grid.add(record, 1, 2);
            GridPane.setMargin(record, new Insets(10));
            root.setCenter(grid);

            record.setOnAction((evt) -> {
                setCursor(Cursor.WAIT);
                new Thread(() -> {
                    try {
                        recorder.startRecording(model);
                    } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException | IOException e) {
                        showErrorDialog("Oh no!", "Couldn't add model to the recorder", "Recorder error: " + e.getMessage());
                    } finally {
                        Platform.runLater(() ->{
                            setCursor(Cursor.DEFAULT);
                        });
                    }
                }).start();
            });

            loadImage(model, thumb);
        }

        private void loadImage(Model model, ImageView thumb) {
            new Thread(() -> {
                try {
                    String url = camsoda.getBaseUrl() + "/api/v1/user/" + model.getName();
                    Request detailRequest = new Request.Builder().url(url).build();
                    Response resp = camsoda.getHttpClient().execute(detailRequest);
                    if (resp.isSuccessful()) {
                        JSONObject json = new JSONObject(resp.body().string());
                        if(json.optBoolean("status") && json.has("user")) {
                            JSONObject user = json.getJSONObject("user");
                            if(user.has("settings")) {
                                JSONObject settings = user.getJSONObject("settings");
                                if(settings.has("offline_picture")) {
                                    Platform.runLater(() -> {
                                        String imageUrl = settings.getString("offline_picture");
                                        Image img = new Image(imageUrl, 1000, thumbSize, true, true, true);
                                        img.progressProperty().addListener(new ChangeListener<Number>() {
                                            @Override
                                            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                                                if(newValue.doubleValue() == 1.0) {
                                                    thumb.setImage(img);
                                                    root.setLeft(thumb);
                                                }
                                            }
                                        });

                                    });
                                }
                            }
                        }
                    }
                    resp.close();
                } catch(Exception e) {
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
