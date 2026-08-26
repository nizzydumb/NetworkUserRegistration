package model;

import java.util.UUID;

public record Network(UUID id, String name, String description, String addressRange, String location) {
    public static Network create(String name, String description, String addressRange, String location) {
        return new Network(UUID.randomUUID(), name, description, addressRange, location);
    }

    public Network update(String name, String description, String addressRange, String location) {
        return new Network(id, name, description, addressRange, location);
    }
}
