package mx.edu.utez.gastocontrol_backend.movement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MovementRequest(
        @NotBlank String type,
        @NotBlank String amount,
        String description,
        @NotBlank String date,
        @NotNull Long categoryId
) {}
