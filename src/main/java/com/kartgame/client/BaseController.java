package com.kartgame.client;

import com.kartgame.common.protocol.packets.S2C_GameEnding;
import com.kartgame.common.protocol.packets.S2C_RecentGamesResponsePacket;
import com.kartgame.common.protocol.packets.S2C_WorldState;
import com.kartgame.server.game.Collectible;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.util.ArrayList;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BaseController implements Initializable {
    private static final float WORLD_LANE_WIDTH = 320.0f;
    private static final float WORLD_LANE_MARGIN = 40.0f;
    private static final float WORLD_Y_MIN = 40.0f;
    private static final float WORLD_Y_MAX = 680.0f;
    private static final float VIEW_LANE_WIDTH = 160.0f;
    private static final float VIEW_Y_MIN = 20.0f;
    private static final float VIEW_Y_MAX = 340.0f;

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
    @FXML private Button startLobbyBtn;

    @FXML private Pane logInForm;
    @FXML private Pane signInForm;
    @FXML private Pane joinLobby;
    @FXML private Pane logsPane;

    @FXML private VBox mainMenuBox;
    @FXML private VBox gameOver;

    @FXML private StackPane menuPane;
    @FXML private StackPane lobbyPane;
    @FXML private StackPane game;
    @FXML private Pane lane1;
    @FXML private Pane lane2;
    @FXML private Pane lane3;
    @FXML private Pane lane4;

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
    @FXML private Label hp1Label;
    @FXML private Label hp2Label;
    @FXML private Label hp3Label;
    @FXML private Label hp4Label;
    @FXML private Label winnerLabel;
    @FXML private Label logsStatusLabel;

    @FXML private TableView<RecentGameRow> recentGamesTable;
    @FXML private TableColumn<RecentGameRow, String> logsDateColumn;
    @FXML private TableColumn<RecentGameRow, String> logsWinnerColumn;
    @FXML private TableColumn<RecentGameRow, String> logsDurationColumn;
    @FXML private TableColumn<RecentGameRow, String> logsPlayersColumn;

    @FXML private Group car1;
    @FXML private Group car2;
    @FXML private Group car3;
    @FXML private Group car4;

    private TCPClient client;
    private UDPClient udpClient;
    private String pendingAuthUsername;
    private AuthAction pendingAuthAction = AuthAction.LOGIN;
    private boolean authorized;

    private final ScheduledExecutorService inputSender = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> inputSenderFuture;
    private volatile boolean accelerating;
    private volatile boolean slowing;
    private volatile boolean turningLeft;
    private volatile boolean turningRight;
    private volatile boolean inputLoopStarted;

    private final Map<Integer, Integer> tokenToSlot = new HashMap<>();
    private final Group[] carGroups = new Group[4];
    private final Label[] hpLabels = new Label[4];
    private final Pane[] lanePanes = new Pane[4];
    private final List<Node> pickupNodes = new ArrayList<>();

    public void setClient(TCPClient client) {
        this.client = client;
        this.client.setLoginResponseListener(this::handleLoginResponse);
        this.client.setLobbyInfoListener(this::handleLobbyInfo);
        this.client.setGameStartedListener(packet -> handleGameStarted());
        this.client.setGameEndingListener(this::handleGameEnding);
        this.client.setRecentGamesListener(this::handleRecentGames);
        this.authorized = this.client.getLoginTag() > 0;
        updateAuthButtonsVisibility();
    }

    public void attachScene(Scene scene) {
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case W, UP -> accelerating = true;
                case S, DOWN -> slowing = true;
                case A, LEFT -> turningLeft = true;
                case D, RIGHT -> turningRight = true;
                default -> {
                }
            }
        });

        scene.setOnKeyReleased(e -> {
            switch (e.getCode()) {
                case W, UP -> accelerating = false;
                case S, DOWN -> slowing = false;
                case A, LEFT -> turningLeft = false;
                case D, RIGHT -> turningRight = false;
                default -> {
                }
            }
        });
    }

    public void showConnectionError(String message) {
        Platform.runLater(() -> {
            loginStatusLabel.setText(message);
            signInStatusLabel.setText(message);
            joinLobbyStatusLabel.setText(message);
        });
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
        game.setVisible(false);
        game.setDisable(true);
        gameOver.setVisible(false);
        gameOver.setDisable(true);
        loginStatusLabel.setText("");
        signInStatusLabel.setText("");
        joinLobbyStatusLabel.setText("");
        logsStatusLabel.setText("");
        updateAuthButtonsVisibility();

        logsDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBattleDate()));
        logsWinnerColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getWinnerUsername()));
        logsDurationColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDuration()));
        logsPlayersColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPlayersCount()));

        carGroups[0] = car1;
        carGroups[1] = car2;
        carGroups[2] = car3;
        carGroups[3] = car4;

        hpLabels[0] = hp1Label;
        hpLabels[1] = hp2Label;
        hpLabels[2] = hp3Label;
        hpLabels[3] = hp4Label;

        lanePanes[0] = lane1;
        lanePanes[1] = lane2;
        lanePanes[2] = lane3;
        lanePanes[3] = lane4;
    }

    @FXML
    private void onLgInBtnClick(ActionEvent e) {
        logsPane.setVisible(false);
        logsPane.setDisable(true);
        signInForm.setVisible(false);
        signInForm.setDisable(true);
        logInForm.setVisible(true);
        logInForm.setDisable(false);
        mainMenuBox.setDisable(true);
        mainMenuBox.setVisible(false);
    }

    @FXML
    private void onSignInBtnClick(ActionEvent e) {
        logsPane.setVisible(false);
        logsPane.setDisable(true);
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
                authorized = true;
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
                updateAuthButtonsVisibility();
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
    private void onStartLobbyBtnClick(ActionEvent e) {
        if (client == null) {
            return;
        }

        try {
            client.sendStartLobby();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onJoinLobbyBtnClick() {
        logsPane.setVisible(false);
        logsPane.setDisable(true);
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

    @FXML
    private void onLogBtnClick(ActionEvent e) {
        logInForm.setVisible(false);
        logInForm.setDisable(true);
        signInForm.setVisible(false);
        signInForm.setDisable(true);
        joinLobby.setVisible(false);
        joinLobby.setDisable(true);
        mainMenuBox.setVisible(false);
        mainMenuBox.setDisable(true);
        logsPane.setVisible(true);
        logsPane.setDisable(false);

        requestRecentGames();
    }

    @FXML
    private void onLogInBackBtnClick(ActionEvent e) {
        showMainMenu();
    }

    @FXML
    private void onSignInBackBtnClick(ActionEvent e) {
        showMainMenu();
    }

    @FXML
    private void onJoinBackBtnClick(ActionEvent e) {
        showMainMenu();
    }

    @FXML
    private void onLogsBackBtnClick(ActionEvent e) {
        showMainMenu();
    }

    @FXML
    private void onLeaveLobbyBtnClick(ActionEvent e) {
        if (client != null) {
            try {
                client.sendLeaveLobby();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        showMainMenu();
        stopGameplay();
    }

    @FXML
    private void onBackToLobbyBtnClick(ActionEvent e) {
        gameOver.setVisible(false);
        gameOver.setDisable(true);
        lobbyPane.setVisible(true);
        lobbyPane.setDisable(false);
        game.setVisible(false);
        game.setDisable(true);
        stopGameplay();
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

            if (game.isVisible() || gameOver.isVisible()) {
                return;
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

    private void handleGameStarted() {
        Platform.runLater(() -> {
            ensureUdpClient();

            menuPane.setVisible(false);
            menuPane.setDisable(true);
            lobbyPane.setVisible(false);
            lobbyPane.setDisable(true);
            gameOver.setVisible(false);
            gameOver.setDisable(true);
            game.setVisible(true);
            game.setDisable(false);
            game.requestFocus();
            startInputLoop();
        });
    }

    private void handleGameEnding(S2C_GameEnding packet) {
        Platform.runLater(() -> {
            winnerLabel.setText("Winner: " + packet.getWinnerUsername());
            game.setVisible(true);
            game.setDisable(false);
            gameOver.setVisible(true);
            gameOver.setDisable(false);
        });
    }

    private void requestRecentGames() {
        if (client == null) {
            logsStatusLabel.setText("Client connection is not ready.");
            return;
        }

        logsStatusLabel.setText("Loading recent games...");
        try {
            client.sendRecentGamesRequest();
        } catch (IOException ex) {
            logsStatusLabel.setText("Failed to load logs: " + ex.getMessage());
        }
    }

    private void handleRecentGames(S2C_RecentGamesResponsePacket packet) {
        Platform.runLater(() -> {
            recentGamesTable.getItems().clear();
            for (S2C_RecentGamesResponsePacket.GameLogEntry entry : packet.getEntries()) {
                recentGamesTable.getItems().add(new RecentGameRow(
                        entry.getBattleDate(),
                        entry.getWinnerUsername(),
                        formatDuration(entry.getDurationMillis()),
                        Integer.toString(entry.getPlayersCount())
                ));
            }

            logsStatusLabel.setText(packet.getEntries().isEmpty() ? "No games found." : "Loaded " + packet.getEntries().size() + " games.");
        });
    }

    private String formatDuration(long durationMillis) {
        long totalSeconds = durationMillis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void handleWorldState(S2C_WorldState state) {
        Platform.runLater(() -> {
            for (S2C_WorldState.KartData kart : state.getKarts()) {
                int slot = tokenToSlot.computeIfAbsent(kart.getPlayerToken(), token -> inferSlotFromWorldX(kart.getX()));
                if (slot < 0 || slot >= carGroups.length) {
                    continue;
                }

                Group group = carGroups[slot];
                Label hpLabel = hpLabels[slot];

                if (group != null) {
                    group.setLayoutX(worldToLaneX(kart.getX(), slot));
                    group.setLayoutY(worldToViewY(kart.getY()));
                    group.setRotate(Math.toDegrees(kart.getAngle()));
                }

                if (hpLabel != null) {
                    hpLabel.setText("HP" + (slot + 1) + ": " + kart.getHp());
                }
            }

            renderCollectibles(state.getCollectibles());
        });
    }

    private void renderCollectibles(List<Collectible> collectibles) {
        for (int i = 0; i < lanePanes.length; i++) {
            Pane lane = lanePanes[i];
            if (lane != null) {
                lane.getChildren().removeAll(pickupNodes);
            }
        }
        pickupNodes.clear();

        double viewPickupRadius = (Collectible.RADIUS / (WORLD_Y_MAX - WORLD_Y_MIN)) * (VIEW_Y_MAX - VIEW_Y_MIN);

        for (Collectible col : collectibles) {
            int slot = inferSlotFromWorldX(col.getX());
            if (slot < 0 || slot >= lanePanes.length) {
                continue;
            }

            Pane lane = lanePanes[slot];
            if (lane == null) {
                continue;
            }

            Circle pickup = new Circle(viewPickupRadius);
            pickup.setManaged(false);
            pickup.setMouseTransparent(true);
            pickup.setFill(Color.web("#ffd400"));
            pickup.setStroke(Color.web("#8a6a00"));
            pickup.setStrokeWidth(2.0);
            pickup.setLayoutX(worldToLaneX(col.getX(), slot));
            pickup.setLayoutY(worldToViewY(col.getY()));

            lane.getChildren().add(pickup);
            pickupNodes.add(pickup);
        }
    }

    private int inferSlotFromWorldX(float worldX) {
        int slot = (int) Math.floor(worldX / WORLD_LANE_WIDTH);
        if (slot < 0) {
            return 0;
        }
        if (slot >= carGroups.length) {
            return carGroups.length - 1;
        }
        return slot;
    }

    private double worldToLaneX(float worldX, int slot) {
        float laneLeft = slot * WORLD_LANE_WIDTH;
        float laneLocal = worldX - laneLeft;

        float worldPlayable = WORLD_LANE_WIDTH - 2 * WORLD_LANE_MARGIN;
        float viewPlayable = VIEW_LANE_WIDTH - 2 * VIEW_Y_MIN;
        float normalized = (laneLocal - WORLD_LANE_MARGIN) / worldPlayable;
        float clamped = Math.max(0.0f, Math.min(1.0f, normalized));

        return VIEW_Y_MIN + clamped * viewPlayable;
    }

    private double worldToViewY(float worldY) {
        float normalized = (worldY - WORLD_Y_MIN) / (WORLD_Y_MAX - WORLD_Y_MIN);
        float clamped = Math.max(0.0f, Math.min(1.0f, normalized));
        return VIEW_Y_MIN + clamped * (VIEW_Y_MAX - VIEW_Y_MIN);
    }

    private void ensureUdpClient() {
        if (udpClient != null) {
            return;
        }

        try {
            udpClient = new UDPClient(client.getAesEngine(), client::getLoginTag);
            udpClient.setWorldStateListener(this::handleWorldState);
            udpClient.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void startInputLoop() {
        if (inputLoopStarted) {
            return;
        }

        inputLoopStarted = true;
        inputSenderFuture = inputSender.scheduleAtFixedRate(() -> {
            UDPClient localUdp = udpClient;
            if (localUdp == null) {
                return;
            }

            try {
                localUdp.sendInput(accelerating, slowing, turningLeft, turningRight);
            } catch (IOException ignored) {
            }
        }, 0, 20, TimeUnit.MILLISECONDS);
    }

    private void stopGameplay() {
        accelerating = false;
        slowing = false;
        turningLeft = false;
        turningRight = false;
        inputLoopStarted = false;
        tokenToSlot.clear();

        if (udpClient != null) {
            udpClient.close();
            udpClient = null;
        }

        if (!inputSenderFuture.isCancelled()) {
            inputSenderFuture.cancel(false);
        }

        for (Pane lane : lanePanes) {
            if (lane != null) {
                lane.getChildren().removeAll(pickupNodes);
            }
        }
        pickupNodes.clear();

        game.setVisible(false);
        game.setDisable(true);
        gameOver.setVisible(false);
        gameOver.setDisable(true);
    }

    private void showMainMenu() {
        logInForm.setVisible(false);
        logInForm.setDisable(true);
        signInForm.setVisible(false);
        signInForm.setDisable(true);
        joinLobby.setVisible(false);
        joinLobby.setDisable(true);
        logsPane.setVisible(false);
        logsPane.setDisable(true);
        lobbyPane.setVisible(false);
        lobbyPane.setDisable(true);
        game.setVisible(false);
        game.setDisable(true);
        menuPane.setVisible(true);
        menuPane.setDisable(false);
        mainMenuBox.setVisible(true);
        mainMenuBox.setDisable(false);
        updateAuthButtonsVisibility();
    }

    private void updateAuthButtonsVisibility() {
        if (lgInBtn != null) {
            lgInBtn.setVisible(!authorized);
            lgInBtn.setManaged(!authorized);
            lgInBtn.setDisable(authorized);
        }

        if (signInBtn != null) {
            signInBtn.setVisible(!authorized);
            signInBtn.setManaged(!authorized);
            signInBtn.setDisable(authorized);
        }
    }

    public static class RecentGameRow {
        private final String battleDate;
        private final String winnerUsername;
        private final String duration;
        private final String playersCount;

        public RecentGameRow(String battleDate, String winnerUsername, String duration, String playersCount) {
            this.battleDate = battleDate;
            this.winnerUsername = winnerUsername;
            this.duration = duration;
            this.playersCount = playersCount;
        }

        public String getBattleDate() {
            return battleDate;
        }

        public String getWinnerUsername() {
            return winnerUsername;
        }

        public String getDuration() {
            return duration;
        }

        public String getPlayersCount() {
            return playersCount;
        }
    }

    public void close() {
        if (!inputSender.isShutdown()) {
            inputSender.shutdownNow();
        }

        if (udpClient != null) udpClient.close();
    }
}
