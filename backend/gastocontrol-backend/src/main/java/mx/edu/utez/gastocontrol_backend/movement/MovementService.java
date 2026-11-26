package mx.edu.utez.gastocontrol_backend.movement;

import mx.edu.utez.gastocontrol_backend.auth.User;
import mx.edu.utez.gastocontrol_backend.auth.UserRepository;
import mx.edu.utez.gastocontrol_backend.category.Category;
import mx.edu.utez.gastocontrol_backend.category.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Service
public class MovementService {

    private final MovementRepository movements;
    private final UserRepository users;
    private final CategoryRepository categories;

    public MovementService(
            MovementRepository movements,
            UserRepository users,
            CategoryRepository categories
    ) {
        this.movements = movements;
        this.users = users;
        this.categories = categories;
    }

    private User currentUser(String email) {
        return users.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private Movement findOwned(Long id, Long userId) {
        return movements.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Movimiento no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<MovementDto> findAll(
            String email, String type, String from, String to
    ) {
        Long userId = currentUser(email).getId();

        LocalDate start = parseOptionalDate(from);
        LocalDate end = parseOptionalDate(to);

        if (start != null && end != null && start.isAfter(end)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Rango de fechas invalido");
        }

        String filterType = type == null || type.isBlank()
                ? null : normalizeType(type);

        return movements.findByUserIdOrderByDateDescIdDesc(userId)
                .stream()
                .filter(m -> filterType == null
                        || m.getType().equals(filterType))
                .filter(m -> start == null
                        || !m.getDate().isBefore(start))
                .filter(m -> end == null
                        || !m.getDate().isAfter(end))
                .map(MovementDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public MovementDto findById(String email, Long id) {
        Long userId = currentUser(email).getId();
        return MovementDto.fromEntity(findOwned(id, userId));
    }

    @Transactional
    public MovementDto create(String email, MovementRequest request) {
        User user = currentUser(email);
        Movement movement = new Movement();
        movement.setUser(user);
        applyChanges(movement, request);
        return MovementDto.fromEntity(movements.save(movement));
    }

    @Transactional
    public MovementDto update(
            String email, Long id, MovementRequest request
    ) {
        Long userId = currentUser(email).getId();
        Movement movement = findOwned(id, userId);
        applyChanges(movement, request);
        return MovementDto.fromEntity(movements.save(movement));
    }

    @Transactional
    public void delete(String email, Long id) {
        Long userId = currentUser(email).getId();
        movements.delete(findOwned(id, userId));
    }

    private void applyChanges(
            Movement movement, MovementRequest request
    ) {
        String type = normalizeType(request.type());

        BigDecimal amount;
        try {
            amount = new BigDecimal(request.amount());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Importe invalido");
        }

        if (amount.signum() <= 0 || amount.scale() > 2
                || amount.precision() - amount.scale() > 13) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Importe fuera de rango");
        }

        LocalDate date = parseDate(request.date());

        Category category = categories.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Categoria no encontrada"));

        if (!category.getType().equalsIgnoreCase(type)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La categoria no corresponde al tipo");
        }

        movement.setType(type);
        movement.setAmount(amount);
        movement.setDescription(request.description());
        movement.setDate(date);
        movement.setCategory(category);
    }

    private String normalizeType(String value) {
        if (value == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tipo obligatorio");
        }

        String type = value.trim().toUpperCase(Locale.ROOT);

        if (!type.equals("INCOME") && !type.equals("EXPENSE")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tipo invalido");
        }

        return type;
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Fecha invalida. Usa yyyy-MM-dd");
        }
    }

    private LocalDate parseOptionalDate(String value) {
        return value == null || value.isBlank()
                ? null : parseDate(value);
    }
}
