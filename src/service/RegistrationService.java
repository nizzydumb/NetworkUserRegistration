package service;

import logging.AppLogger;
import model.Network;
import model.NetworkUser;
import repository.RegistrationRepository;

import java.util.List;

public class RegistrationService {
    private final RegistrationRepository repository;

    public RegistrationService(RegistrationRepository repository) {
        this.repository = repository;
    }

    public List<Network> getNetworks() {
        return repository.findNetworks();
    }

    public List<NetworkUser> getUsers(Network network) {
        if (network == null) {
            return List.of();
        }
        return repository.findUsersByNetworkId(network.id());
    }

    public Network addNetwork(String name, String description, String addressRange, String location) {
        Network network = Network.create(required(name), required(description), required(addressRange), optional(location));
        repository.addNetwork(network);
        AppLogger.info("RECORD_ADDED table=networks id=" + network.id() + " name=\"" + safe(network.name()) + "\"");
        return network;
    }

    public Network updateNetwork(Network network, String name, String description, String addressRange, String location) {
        if (network == null) {
            throw new IllegalArgumentException("A network must be selected before editing.");
        }

        Network updatedNetwork = network.update(required(name), required(description), required(addressRange), optional(location));
        repository.updateNetwork(updatedNetwork);
        AppLogger.info("RECORD_UPDATED table=networks id=" + updatedNetwork.id());
        return updatedNetwork;
    }

    public NetworkUser addUser(Network network, String description, String fullName, String login, String ipAddress, String role) {
        if (network == null) {
            throw new IllegalArgumentException("A network must be selected before adding a user.");
        }

        NetworkUser user = NetworkUser.create(
                repository.nextUserId(network.id()),
                network.id(),
                required(description),
                required(fullName),
                required(login),
                optional(ipAddress),
                optional(role)
        );
        repository.addUser(user);
        AppLogger.info("RECORD_ADDED table=users id=" + user.hexId() + " networkId=" + user.networkId()
                + " login=\"" + safe(user.login()) + "\"");
        return user;
    }

    public NetworkUser updateUser(NetworkUser user, String description, String fullName, String login, String ipAddress, String role) {
        if (user == null) {
            throw new IllegalArgumentException("A user must be selected before editing.");
        }

        NetworkUser updatedUser = user.update(
                required(description),
                required(fullName),
                required(login),
                optional(ipAddress),
                optional(role)
        );
        repository.updateUser(updatedUser);
        AppLogger.info("RECORD_UPDATED table=users id=" + updatedUser.hexId()
                + " networkId=" + updatedUser.networkId());
        return updatedUser;
    }

    public void deleteNetwork(Network network) {
        if (network == null) {
            throw new IllegalArgumentException("A network must be selected before deleting.");
        }
        int removedUsers = repository.findUsersByNetworkId(network.id()).size();
        repository.deleteNetwork(network);
        AppLogger.info("RECORD_REMOVED table=networks id=" + network.id() + " name=\"" + safe(network.name())
                + "\" cascadedUsers=" + removedUsers);
    }

    public void deleteUser(NetworkUser user) {
        if (user == null) {
            throw new IllegalArgumentException("A user must be selected before deleting.");
        }
        repository.deleteUser(user);
        AppLogger.info("RECORD_REMOVED table=users id=" + user.hexId() + " networkId=" + user.networkId()
                + " login=\"" + safe(user.login()) + "\"");
    }

    private String required(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Required fields cannot be empty.");
        }
        return normalized;
    }

    private String optional(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? "-" : normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').replace('"', '\'');
    }
}
