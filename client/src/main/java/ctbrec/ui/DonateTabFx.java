package ctbrec.ui;



import ctbrec.sites.chaturbate.Chaturbate;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class DonateTabFx extends Tab {

    public DonateTabFx() {
        setClosable(false);
        setText("Donate");
        BorderPane container = new BorderPane();
        container.setPadding(new Insets(10));
        setContent(container);

        VBox headerVbox = new VBox(10);
        headerVbox.setAlignment(Pos.CENTER);
        Label beer = new Label("Buy me some beer?!");
        beer.setFont(new Font(36));
        Label desc = new Label("If you like this software and want to buy me some beer or pizza, here are some possibilities!");
        desc.setFont(new Font(24));
        headerVbox.getChildren().addAll(beer, desc);
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER);
        header.getChildren().add(headerVbox);
        header.setPadding(new Insets(20, 0, 0, 0));
        container.setTop(header);

        ImageView tokenImage = new ImageView(getClass().getResource("/html/token.png").toString());
        Button tokenButton = new Button("Buy tokens");
        tokenButton.setOnAction((e) -> { DesktopIntegration.open(Chaturbate.AFFILIATE_LINK); });
        VBox tokenBox = new VBox(5);
        tokenBox.setAlignment(Pos.TOP_CENTER);
        Label tokenDesc = new Label("If you buy tokens by using this button,\n"
                + "Chaturbate will award me 20% of the tokens' value for sending you over.\n"
                + "You get the full tokens and it doesn't cost you any more!");
        tokenDesc.setTextAlignment(TextAlignment.CENTER);
        tokenBox.getChildren().addAll(tokenImage, tokenButton, tokenDesc);

        ImageView coffeeImage = new ImageView(getClass().getResource("/buymeacoffee-round.png").toString());
        Button coffeeButton = new Button("Buy me a coffee");
        coffeeButton.setOnMouseClicked((e) -> { DesktopIntegration.open("https://www.buymeacoffee.com/0xboobface"); });
        VBox buyCoffeeBox = new VBox(5);
        buyCoffeeBox.setAlignment(Pos.TOP_CENTER);
        buyCoffeeBox.getChildren().addAll(coffeeImage, coffeeButton);

        ImageView paypalImage = new ImageView(getClass().getResource("/paypal-round.png").toString());
        Button paypalButton = new Button("PayPal");
        paypalButton.setOnMouseClicked((e) -> { DesktopIntegration.open("https://www.paypal.me/0xb00bface"); });
        VBox paypalBox = new VBox(5);
        paypalBox.setAlignment(Pos.TOP_CENTER);
        paypalBox.getChildren().addAll(paypalImage, paypalButton);

        ImageView patreonImage = new ImageView(getClass().getResource("/patreon-round.png").toString());
        Button patreonButton = new Button("Become a Patron");
        patreonButton.setOnMouseClicked((e) -> { DesktopIntegration.open("https://www.patreon.com/0xb00bface"); });
        VBox patreonBox = new VBox(5);
        patreonBox.setAlignment(Pos.TOP_CENTER);
        patreonBox.getChildren().addAll(patreonImage, patreonButton);

        HBox topBox = new HBox(5);
        topBox.setAlignment(Pos.CENTER);
        topBox.setSpacing(50);
        topBox.getChildren().addAll(tokenBox);

        HBox bottomBox = new HBox(5);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setSpacing(50);
        bottomBox.getChildren().addAll(buyCoffeeBox, paypalBox, patreonBox);

        VBox centerBox = new VBox(50);
        centerBox.getChildren().addAll(topBox, bottomBox);
        container.setCenter(centerBox);
    }
}
