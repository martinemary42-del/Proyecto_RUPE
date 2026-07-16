package mx.edu.unadm.rupe.auth.repository;

import java.util.Optional;
import mx.edu.unadm.rupe.auth.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
    Optional<PasswordResetToken> findByToken(String token);
}
