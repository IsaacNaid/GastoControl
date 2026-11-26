package mx.edu.utez.gastocontrol_backend.movement;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovementRepository
        extends JpaRepository<Movement, Long> {

    List<Movement> findByUserIdOrderByDateDescIdDesc(Long userId);

    Optional<Movement> findByIdAndUserId(Long id, Long userId);

    List<Movement> findByUserIdAndTypeOrderByDateDescIdDesc(
            Long userId,
            String type
    );

    List<Movement> findByUserIdAndDateBetweenOrderByDateDescIdDesc(
            Long userId,
            LocalDate from,
            LocalDate to
    );
}
