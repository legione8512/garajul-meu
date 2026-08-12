package ro.garajulmeu.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Both lookups expect an already-normalised address. Callers must lower-case and
 * trim before querying, or the unique index will be consulted with a value that
 * cannot match what was stored.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);
}