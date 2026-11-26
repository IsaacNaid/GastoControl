package mx.edu.utez.gastocontrol_backend.category;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public List<Category> findAll() {
        return repository.findAll();
    }

    public Category findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Categoría no encontrada"
                        ));
    }

    public Category create(CategoryRequest request) {
        Category category = new Category();
        applyChanges(category, request);
        return repository.save(category);
    }

    public Category update(Long id, CategoryRequest request) {
        Category category = findById(id);
        applyChanges(category, request);
        return repository.save(category);
    }

    public void delete(Long id) {
        Category category = findById(id);
        repository.delete(category);
    }

    private void applyChanges(
            Category category,
            CategoryRequest request
    ) {
        category.setName(request.name().trim());
        category.setType(request.type());
        category.setColor(
                request.color() == null || request.color().isBlank()
                        ? "#2563EB"
                        : request.color()
        );
        category.setIcon(
                request.icon() == null || request.icon().isBlank()
                        ? "category"
                        : request.icon()
        );
    }
}
