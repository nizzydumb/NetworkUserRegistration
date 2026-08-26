package security;

public record UserCredentials(
        String username,
        String passwordHash,
        String passwordSalt,
        int iterations,
        String algorithm
) {
}
