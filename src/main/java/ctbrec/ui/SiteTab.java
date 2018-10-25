package ctbrec.ui;

import ctbrec.sites.Site;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class SiteTab extends Tab implements TabSelectionListener {

    private BorderPane rootPane = new BorderPane();
    private HBox tokenPanel;
    private SiteTabPane siteTabPane;

    public SiteTab(Site site, Scene scene) {
        super(site.getName());

        setClosable(false);
        setContent(rootPane);
        siteTabPane = new SiteTabPane(site, scene);
        rootPane.setCenter(siteTabPane);

        if (site.supportsTips() && site.credentialsAvailable()) {
            Button buyTokens = new Button("Buy Tokens");
            buyTokens.setOnAction((e) -> DesktopIntergation.open(site.getBuyTokensLink()));
            TokenLabel tokenBalance = new TokenLabel(site);
            tokenPanel = new HBox(5, tokenBalance, buyTokens);
            tokenPanel.setAlignment(Pos.BASELINE_RIGHT);
            rootPane.setTop(tokenPanel);
            // HBox.setMargin(tokenBalance, new Insets(0, 5, 0, 0));
            // HBox.setMargin(buyTokens, new Insets(0, 5, 0, 0));
            tokenBalance.loadBalance();
            BorderPane.setMargin(tokenPanel, new Insets(5, 10, 0, 10));
        }
    }

    @Override
    public void selected() {
        siteTabPane.selected();
    }

    @Override
    public void deselected() {
        siteTabPane.deselected();
    }
}
