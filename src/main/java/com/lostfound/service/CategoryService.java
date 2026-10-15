
package com.lostfound.service;

import com.lostfound.dto.category.CategoryRequest;
import com.lostfound.dto.category.CategoryResponse;
import com.lostfound.entity.Category;
import com.lostfound.repository.CategoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse getCategoryById(Long id) {
        return toResponse(getCategory(id));
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Category already exists");
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(request.getDescription());
        category.setActive(true);

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(
            Long id, CategoryRequest request) {

        Category category = getCategory(id);
        String name = request.getName().trim();

        boolean nameUsed = categoryRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .isPresent();

        if (nameUsed) {
            throw new IllegalArgumentException("Category name already exists");
        }

        category.setName(name);
        category.setDescription(request.getDescription());

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deactivateCategory(Long id) {
        Category category = getCategory(id);
        category.setActive(false);
        categoryRepository.save(category);
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Category not found"));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive()
        );
    }
}
