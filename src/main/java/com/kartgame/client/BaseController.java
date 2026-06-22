package com.kartgame.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class BaseController implements Initializable {
    @FXML private Button lgInBtn;
    @FXML private Button lgInFormBtn;
    @FXML private Pane logInForm;
    @FXML private VBox mainMenuBox;
    @FXML private TextField lgnForm;
    @FXML private PasswordField pswrdForm;

    private TCPClient client;

    public void setClient(TCPClient client) {
        this.client = client;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logInForm.setVisible(false);
        logInForm.setDisable(true);
    }

    @FXML
    private void onLgInBtnClick(ActionEvent e) {
        logInForm.setVisible(true);
        logInForm.setDisable(false);
        mainMenuBox.setDisable(true);
        mainMenuBox.setVisible(false);
    }

    @FXML
    private void onLgInFormBtnClick(ActionEvent e) {
        String username = lgnForm.getText().trim();
        String password = pswrdForm.getText();

        if (username.isEmpty()) {
            System.err.println("Username is required.");
            return;
        }

        if (client == null) {
            System.err.println("TCP client is not initialized.");
            return;
        }

        try {
            client.sendLogin(username, password);
            System.out.println("Login packet sent for user: " + username);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        logInForm.setVisible(false);
        logInForm.setDisable(true);
        mainMenuBox.setDisable(false);
        mainMenuBox.setVisible(true);
    }
}
