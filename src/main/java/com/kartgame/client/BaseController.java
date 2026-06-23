package com.kartgame.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    @FXML private Label loginStatusLabel;
    @FXML private Label nicknameLabel;

    private TCPClient client;
    private String lastLoginUsername;

    public void setClient(TCPClient client) {
        this.client = client;
        this.client.setLoginResponseListener(this::handleLoginResponse);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logInForm.setVisible(false);
        logInForm.setDisable(true);
        loginStatusLabel.setText("");
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
            loginStatusLabel.setText("Username is required.");
            return;
        }

        if (client == null) {
            loginStatusLabel.setText("Client connection is not ready.");
            return;
        }

        lastLoginUsername = username;
        try {
            client.sendLogin(username, password);
            loginStatusLabel.setText("Login packet sent...");
        } catch (IOException ex) {
            loginStatusLabel.setText("Failed to send login: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void handleLoginResponse(com.kartgame.common.protocol.packets.S2C_LoginResponse response) {
        Platform.runLater(() -> {
            if (response.getToken() > 0) {
                if (lastLoginUsername != null && !lastLoginUsername.isEmpty()) {
                    nicknameLabel.setText(lastLoginUsername);
                }
                loginStatusLabel.setText("Login successful: " + response.getMessage());
                logInForm.setVisible(false);
                logInForm.setDisable(true);
                mainMenuBox.setVisible(true);
                mainMenuBox.setDisable(false);

                try {
                    client.sendPlayerInfoReq();
                    client.sendLobbyInfoReq();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                loginStatusLabel.setText(response.getMessage());
            }
        });
    }
}
