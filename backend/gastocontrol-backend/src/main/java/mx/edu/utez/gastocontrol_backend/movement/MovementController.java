package mx.edu.utez.gastocontrol_backend.movement;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movements")
public class MovementController {

    private final MovementService service;

    public MovementController(MovementService service) {
        this.service = service;
    }

    @GetMapping
    public List<MovementDto> findAll(
            Authentication authentication,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return service.findAll(
                authentication.getName(), type, from, to
        );
    }

    @GetMapping("/{id}")
    public MovementDto findById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return service.findById(authentication.getName(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovementDto create(
            Authentication authentication,
            @Valid @RequestBody MovementRequest request
    ) {
        return service.create(authentication.getName(), request);
    }

    @PutMapping("/{id}")
    public MovementDto update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody MovementRequest request
    ) {
        return service.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            Authentication authentication,
            @PathVariable Long id
    ) {
        service.delete(authentication.getName(), id);
    }
}
