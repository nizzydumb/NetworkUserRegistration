package repository;

import model.Network;
import model.NetworkUser;

import java.util.List;
import java.util.UUID;

public interface RegistrationRepository {
    List<Network> findNetworks();

    List<NetworkUser> findUsersByNetworkId(UUID networkId);

    long nextUserId(UUID networkId);

    void addNetwork(Network network);

    void addUser(NetworkUser user);

    void updateNetwork(Network network);

    void updateUser(NetworkUser user);

    void deleteNetwork(Network network);

    void deleteUser(NetworkUser user);
}
