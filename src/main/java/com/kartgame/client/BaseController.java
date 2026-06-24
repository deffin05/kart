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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class BaseController implements Initializable {
    private enum AuthAction {
        LOGIN,
        REGISTER
    }

    @FXML private Button lgInBtn;
    @FXML private Button lgInFormBtn;
    @FXML private Button signInBtn;
    @FXML private Button signInFormBtn;
    @FXML private Button joinLobbyBtn;
    @FXML private Button createLobbyBtn;
    @FXML private Button joinLobbyFormBtn;

    @FXML private Pane logInForm;
    @FXML private Pane signInForm;
    @FXML private Pane joinLobby;

    @FXML private VBox mainMenuBox;

    @FXML private StackPane menuPane;
    @FXML private StackPane lobbyPane;

    @FXML private TextField lgnForm;
    @FXML private TextField signInLgnForm;
    @FXML private TextField lobbyId;

    @FXML private PasswordField pswrdForm;
    @FXML private PasswordField signInPswrdForm;

    @FXML private Label loginStatusLabel;
    @FXML private Label signInStatusLabel;
    @FXML private Label joinLobbyStatusLabel;
    @FXML private Label nicknameLabel;
    @FXML private Label lobbyIdLabel;
    @FXML private VBox lobbyPlayersList;
    @FXML private Label lobbyStatusLabel;

    private TCPClient client;
    private String pendingAuthUsername;
    private AuthAction pendingAuthAction = AuthAction.LOGIN;

    public void setClient(TCPClient client) {
        this.client = client;
        this.client.setLoginResponseListener(this::handleLoginResponse);
        this.client.setLobbyInfoListener(this::handleLobbyInfo);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logInForm.setVisible(false);
        logInForm.setDisable(true);
        signInForm.setVisible(false);
        signInForm.setDisable(true);
        joinLobby.setVisible(false);
        joinLobby.setDisable(true);
        lobbyPane.setVisible(false);
        lobbyPane.setDisable(true);
        loginStatusLabel.setText("");
        signInStatusLabel.setText("");
        joinLobbyStatusLabel.setText("");
    }

    @FXML
    private void onLgInBtnClick(ActionEvent e) {
        signInForm.setVisible(false);
        signInForm.setDisable(true);
        logInForm.setVisible(true);
        logInForm.setDisable(false);
        mainMenuBox.setDisable(true);
        mainMenuBox.setVisible(false);
    }

    @FXML
    private void onSignInBtnClick(ActionEvent e) {
        logInForm.setVisible(false);
        logInForm.setDisable(true);
        signInForm.setVisible(true);
        signInForm.setDisable(false);
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

        pendingAuthUsername = username;
        pendingAuthAction = AuthAction.LOGIN;
        try {
            client.sendLogin(username, password);
            loginStatusLabel.setText("Login packet sent...");
        } catch (IOException ex) {
            loginStatusLabel.setText("Failed to send login: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void onSignInFormBtnClick(ActionEvent e) {
        String username = signInLgnForm.getText().trim();
        String password = signInPswrdForm.getText();

        if (username.isEmpty()) {
            signInStatusLabel.setText("Username is required.");
            return;
        }

        if (client == null) {
            signInStatusLabel.setText("Client connection is not ready.");
            return;
        }

        pendingAuthUsername = username;
        pendingAuthAction = AuthAction.REGISTER;
        try {
            client.sendRegister(username, password);
            signInStatusLabel.setText("Register packet sent...");
        } catch (IOException ex) {
            signInStatusLabel.setText("Failed to send register: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void handleLoginResponse(com.kartgame.common.protocol.packets.S2C_LoginResponse response) {
        Platform.runLater(() -> {
            Label activeStatusLabel = pendingAuthAction == AuthAction.REGISTER ? signInStatusLabel : loginStatusLabel;
            if (response.getToken() > 0) {
                if (pendingAuthUsername != null && !pendingAuthUsername.isEmpty()) {
                    nicknameLabel.setText(pendingAuthUsername);
                }
                activeStatusLabel.setText("Success: " + response.getMessage());
                logInForm.setVisible(false);
                logInForm.setDisable(true);
                signInForm.setVisible(false);
                signInForm.setDisable(true);
                joinLobby.setVisible(false);
                joinLobby.setDisable(true);
                menuPane.setVisible(true);
                menuPane.setDisable(false);
                lobbyPane.setVisible(false);
                lobbyPane.setDisable(true);
                mainMenuBox.setVisible(true);
                mainMenuBox.setDisable(false);
                joinLobbyStatusLabel.setText("");
            } else {
                activeStatusLabel.setText(response.getMessage());
            }
        });
    }

    @FXML
    private void onCreateLobbyBtnClick() {
        if (client == null) {
            return;
        }

        try {
            client.sendCreateLobby();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onJoinLobbyBtnClick() {
        joinLobbyStatusLabel.setText("");
        joinLobby.setVisible(true);
        joinLobby.setDisable(false);
        mainMenuBox.setDisable(true);
        mainMenuBox.setVisible(false);
    }

    @FXML
    private void onJoinLobbyFormBtnClick() {
        if (client == null) {
            joinLobbyStatusLabel.setText("Client connection is not ready.");
            return;
        }

        int targetLobbyId;
        try {
            targetLobbyId = Integer.parseInt(lobbyId.getText().trim());
        } catch (NumberFormatException ex) {
            joinLobbyStatusLabel.setText("Lobby ID must be a number.");
            return;
        }

        try {
            client.sendJoinLobby(targetLobbyId);
            joinLobbyStatusLabel.setText("Joining lobby...");
        } catch (IOException ex) {
            joinLobbyStatusLabel.setText("Failed to join lobby: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void handleLobbyInfo(com.kartgame.common.protocol.packets.S2C_LobbyInfoPacket response) {
        Platform.runLater(() -> {
            if (lobbyIdLabel != null) {
                lobbyIdLabel.setText("Lobby ID: " + response.getLobbyId());
            }
            if (lobbyStatusLabel != null) {
                lobbyStatusLabel.setText("Lobby loaded with " + response.getPlayerUsernames().size() + " players.");
            }
            if (lobbyPlayersList != null) {
                lobbyPlayersList.getChildren().clear();
                for (String username : response.getPlayerUsernames()) {
                    Label playerLabel = new Label(username);
                    lobbyPlayersList.getChildren().add(playerLabel);
                }
            }
            menuPane.setVisible(false);
            menuPane.setDisable(true);
            joinLobby.setVisible(false);
            joinLobby.setDisable(true);
            mainMenuBox.setVisible(false);
            mainMenuBox.setDisable(true);
            lobbyPane.setVisible(true);
            lobbyPane.setDisable(false);
        });
    }
}
