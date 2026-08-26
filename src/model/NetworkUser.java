package model;

import java.util.UUID;

public record NetworkUser(long id, UUID networkId, String description, String fullName, String login, String ipAddress, String role) {
    public static NetworkUser create(long id, UUID networkId, String description, String fullName, String login, String ipAddress, String role) {
        if (id < 0 || id > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("User ID must fit into 32 bits.");
        }
        return new NetworkUser(id, networkId, description, fullName, login, ipAddress, role);
    }

    public NetworkUser update(String description, String fullName, String login, String ipAddress, String role) {
        return new NetworkUser(id, networkId, description, fullName, login, ipAddress, role);
    }

    public String hexId() {
        return String.format("%08X", id);
    }
}
