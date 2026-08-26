package repository;

import model.Network;
import model.NetworkUser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InMemoryRegistrationRepository implements RegistrationRepository {
    private final List<Network> networks = new ArrayList<>();
    private final Map<UUID, List<NetworkUser>> usersByNetworkId = new LinkedHashMap<>();
    private final Map<UUID, Long> nextUserIdByNetworkId = new LinkedHashMap<>();

    public InMemoryRegistrationRepository() {
        seedDemoData();
    }

    @Override
    public List<Network> findNetworks() {
        return List.copyOf(networks);
    }

    @Override
    public List<NetworkUser> findUsersByNetworkId(UUID networkId) {
        return List.copyOf(usersByNetworkId.getOrDefault(networkId, List.of()));
    }

    @Override
    public long nextUserId(UUID networkId) {
        long nextUserId = nextUserIdByNetworkId.getOrDefault(networkId, 0L);
        if (nextUserId > 0xFFFFFFFFL) {
            throw new IllegalStateException("No more 32-bit user IDs are available.");
        }
        nextUserIdByNetworkId.put(networkId, nextUserId + 1);
        return nextUserId++;
    }

    @Override
    public void addNetwork(Network network) {
        networks.add(network);
        usersByNetworkId.putIfAbsent(network.id(), new ArrayList<>());
        nextUserIdByNetworkId.putIfAbsent(network.id(), 0L);
    }

    @Override
    public void addUser(NetworkUser user) {
        usersByNetworkId.computeIfAbsent(user.networkId(), id -> new ArrayList<>()).add(user);
    }

    @Override
    public void updateNetwork(Network network) {
        for (int i = 0; i < networks.size(); i++) {
            if (networks.get(i).id().equals(network.id())) {
                networks.set(i, network);
                return;
            }
        }
    }

    @Override
    public void updateUser(NetworkUser user) {
        List<NetworkUser> users = usersByNetworkId.get(user.networkId());
        if (users == null) {
            return;
        }

        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).id() == user.id()) {
                users.set(i, user);
                return;
            }
        }
    }

    @Override
    public void deleteNetwork(Network network) {
        networks.removeIf(savedNetwork -> savedNetwork.id().equals(network.id()));
        usersByNetworkId.remove(network.id());
        nextUserIdByNetworkId.remove(network.id());
    }

    @Override
    public void deleteUser(NetworkUser user) {
        List<NetworkUser> users = usersByNetworkId.get(user.networkId());
        if (users != null) {
            users.removeIf(savedUser -> savedUser.id() == user.id());
        }
    }

    private void seedDemoData() {
        Network office = Network.create("Office Network", "Corporate office devices", "192.168.1.0/24", "Main office");
        Network lab = Network.create("Lab Network", "Testing and staging devices", "10.0.5.0/24", "Testing room");

        addNetwork(office);
        addNetwork(lab);

        addUser(NetworkUser.create(nextUserId(office.id()), office.id(), "Primary administrator", "Miras Admin", "madmin", "192.168.1.10", "Administrator"));
        addUser(NetworkUser.create(nextUserId(office.id()), office.id(), "Help desk workstation", "Support Desk", "support", "192.168.1.11", "Operator"));
        addUser(NetworkUser.create(nextUserId(lab.id()), lab.id(), "Validation account", "Test User", "tester", "10.0.5.20", "Tester"));
    }
}
