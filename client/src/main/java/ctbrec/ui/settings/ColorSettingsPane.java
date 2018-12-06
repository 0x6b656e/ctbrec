package ctbrec.ui.settings;

import ctbrec.Config;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class ColorSettingsPane extends Pane {

    Label labelBaseColor = new Label("Base");
    ColorPicker baseColor = new ColorPicker();
    Label labelAccentColor = new Label("Accent");
    ColorPicker accentColor = new ColorPicker();
    Button reset = new Button("Reset");
    Pane foobar = new Pane();

    public ColorSettingsPane(SettingsTab settingsTab) {
        getChildren().add(labelBaseColor);
        getChildren().add(baseColor);
        getChildren().add(labelAccentColor);
        getChildren().add(accentColor);
        getChildren().add(reset);

        baseColor.setValue(Color.web(Config.getInstance().getSettings().colorBase));
        accentColor.setValue(Color.web(Config.getInstance().getSettings().colorAccent));

        baseColor.setOnAction(evt -> {
            Config.getInstance().getSettings().colorBase = toWeb(baseColor.getValue());
            settingsTab.showRestartRequired();
            settingsTab.saveConfig();
        });
        accentColor.setOnAction(evt -> {
            Config.getInstance().getSettings().colorAccent = toWeb(accentColor.getValue());
            settingsTab.showRestartRequired();
            settingsTab.saveConfig();
        });
        reset.setOnAction(evt -> {
            baseColor.setValue(Color.WHITE);
            Config.getInstance().getSettings().colorBase = toWeb(Color.WHITE);
            accentColor.setValue(Color.WHITE);
            Config.getInstance().getSettings().colorAccent = toWeb(Color.WHITE);
            settingsTab.showRestartRequired();
            settingsTab.saveConfig();
        });
    }

    private String toWeb(Color value) {
        StringBuilder sb = new StringBuilder("#");
        sb.append(toHex((int) (value.getRed() * 255)));
        sb.append(toHex((int) (value.getGreen() * 255)));
        sb.append(toHex((int) (value.getBlue() * 255)));
        if(!value.isOpaque()) {
            sb.append(toHex((int) (value.getOpacity() * 255)));
        }
        return sb.toString();
    }

    private CharSequence toHex(int v) {
        StringBuilder sb = new StringBuilder();
        if(v < 16) {
            sb.append('0');
        }
        sb.append(Integer.toHexString(v));
        return sb;
    }

    @Override
    protected void layoutChildren() {
        labelBaseColor.resize(32, 25);
        baseColor.resize(44, 25);
        labelAccentColor.resize(46, 25);
        accentColor.resize(44, 25);
        reset.resize(60, 25);

        labelBaseColor.setTranslateX(0);
        baseColor.setTranslateX(labelBaseColor.getWidth() + 10);
        labelAccentColor.setTranslateX(baseColor.getTranslateX() + baseColor.getWidth() + 15);
        accentColor.setTranslateX(labelAccentColor.getTranslateX() + labelAccentColor.getWidth() + 10);
        reset.setTranslateX(accentColor.getTranslateX() + accentColor.getWidth() + 50);
    }
}
