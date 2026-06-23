package com.kartgame.server.lobby;

import com.kartgame.common.protocol.packets.S2C_LobbyInfoPacket;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyManager {
    private final ConcurrentHashMap<Integer, Player> playerMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Lobby> lobbyMap =  new ConcurrentHashMap<>();

    private final SecureRandom random = new SecureRandom();

    public void registerPlayer(Player player) {
        playerMap.put(player.getToken(), player);
    }

    public Player getPlayer(int token) {
        return playerMap.get(token);
    }

    public void disconnectPlayer(int token) {
        Player player = playerMap.remove(token);
        if (player != null) {
            if (player.getCurrentLobbyId() != null) {
                leaveLobby(player);
            }
            System.out.println("Closed session for player: " + player.getUsername());
        }
    }

    public Lobby createLobby(Player creator) {
        if (creator.getCurrentLobbyId() != null) {
            leaveLobby(creator);
        }

        int lobbyId = generateLobbyCode();
        Lobby newLobby = new Lobby(lobbyId);

        lobbyMap.put(lobbyId, newLobby);
        newLobby.addPlayer(creator);

        System.out.println("Lobby " + lobbyId + " created by " + creator.getUsername());
        return newLobby;
    }

    public boolean joinLobby(Player player, int lobbyId) {
        Lobby lobby = lobbyMap.get(lobbyId);

        if (lobby == null) return false;

        if (player.getCurrentLobbyId() != null) {
            leaveLobby(player);
        }

        return lobby.addPlayer(player);
    }

    public void leaveLobby(Player player) {
        Integer lobbyId = player.getCurrentLobbyId();
        if (lobbyId == null) return;

        Lobby lobby = lobbyMap.get(lobbyId);
        if (lobby != null) {
            lobby.removePlayer(player);

            if (lobby.getPlayerCount() == 0) {
                lobbyMap.remove(lobbyId);
                System.out.println("Lobby " + lobbyId + " destroyed.");
            } else {
                lobby.broadcast(new S2C_LobbyInfoPacket(lobbyId, lobby.getLobbyUsernames()));
            }
        }
    }

    private int generateLobbyCode() {
        int code;
        do {
            code = random.nextInt();
        } while (lobbyMap.containsKey(code));

        return code;
    }

    public ConcurrentHashMap<Integer, Player> getPlayerMap() {
        return playerMap;
    }

    public ConcurrentHashMap<Integer, Lobby> getLobbyMap() {
        return lobbyMap;
    }
}
