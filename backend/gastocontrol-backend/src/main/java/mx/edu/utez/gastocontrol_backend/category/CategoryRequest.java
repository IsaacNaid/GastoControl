package mx.edu.utez.gastocontrol_backend.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Pattern(regexp = "INCOME|EXPENSE")
        String type,

        String color,
        String icon
) {}
