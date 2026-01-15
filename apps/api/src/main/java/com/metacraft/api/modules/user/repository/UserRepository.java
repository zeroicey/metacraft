package cc.serenique.api.modules.user.repository;

import cc.serenique.api.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Short> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
