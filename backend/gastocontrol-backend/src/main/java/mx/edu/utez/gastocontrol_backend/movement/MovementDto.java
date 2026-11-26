package mx.edu.utez.gastocontrol_backend.movement;

public record MovementDto(
        Long id,
        String type,
        String amount,
        String description,
        String date,
        Long categoryId,
        String categoryName
) {
    public static MovementDto fromEntity(Movement movement) {
        return new MovementDto(
                movement.getId(),
                movement.getType(),
                movement.getAmount().toPlainString(),
                movement.getDescription(),
                movement.getDate().toString(),
                movement.getCategory().getId(),
                movement.getCategory().getName()
        );
    }
}
