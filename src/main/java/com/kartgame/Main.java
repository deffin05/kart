package com.kartgame;

import com.kartgame.client.BaseController;
import com.kartgame.client.TCPClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private TCPClient client;
    private BaseController controller;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/kartgame/client/FXML/Base.fxml"));
        Parent root = loader.load();

        controller = loader.getController();
        this.client = new TCPClient();
        controller.setClient(client);

        stage.setTitle("Kart Game");
        Scene scene = new Scene(root);
        controller.attachScene(scene);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        this.client.close();
        this.controller.close();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
