package ctbrec.ui.settings;

import ctbrec.Config;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class ColorSettingsPane extends Pane {

    ColorPicker baseColor = new ColorPicker();
    ColorPicker accentColor = new ColorPicker();
    Button reset = new Button("Reset");
    Pane foobar = new Pane();

    public ColorSettingsPane(SettingsTab settingsTab) {
        getChildren().add(baseColor);
        getChildren().add(accentColor);
        getChildren().add(reset);

        baseColor.setValue(Color.web(Config.getInstance().getSettings().colorBase));
        baseColor.setTooltip(new Tooltip("Base Color"));
        accentColor.setValue(Color.web(Config.getInstance().getSettings().colorAccent));
        accentColor.setTooltip(new Tooltip("Accent Color"));

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
        baseColor.resize(44, 25);
        accentColor.resize(44, 25);
        reset.resize(60, 25);

        baseColor.setTranslateX(0);
        accentColor.setTranslateX(baseColor.getTranslateX() + baseColor.getWidth() + 10);
        reset.setTranslateX(accentColor.getTranslateX() + accentColor.getWidth() + 10);
    }
}
