package ru.practicum.mainservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mainservice.dto.request.NewCategoryDto;
import ru.practicum.mainservice.dto.response.CategoryDto;
import ru.practicum.mainservice.exception.CategoryNameAlreadyExistsException;
import ru.practicum.mainservice.exception.CategoryNotEmptyException;
import ru.practicum.mainservice.exception.CategoryNotFoundException;
import ru.practicum.mainservice.mapper.CategoryMapper;
import ru.practicum.mainservice.model.entity.Category;
import ru.practicum.mainservice.repository.CategoryRepository;
import ru.practicum.mainservice.repository.EventRepository;
import ru.practicum.mainservice.service.CategoryService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        log.info("Создание категории: {}", newCategoryDto.getName());

        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new CategoryNameAlreadyExistsException(
                    "Категория с именем '" + newCategoryDto.getName() + "' уже существует"
            );
        }

        Category category = categoryMapper.toCategory(newCategoryDto);

        Category savedCategory = categoryRepository.save(category);
        log.info("Категория создана с ID: {}", savedCategory.getId());


        return categoryMapper.toCategoryDto(savedCategory);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, NewCategoryDto newCategoryDto) {
        log.info("Обновление категории ID: {}", catId);

        Category existingCategory = categoryRepository.findById(catId)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Категория с ID " + catId + " не найдена"
                ));

        String newName = newCategoryDto.getName();
        if (!existingCategory.getName().equals(newName)) {

            if (categoryRepository.existsByName(newName)) {
                throw new CategoryNameAlreadyExistsException(
                        "Категория с именем '" + newName + "' уже существует"
                );
            }
            existingCategory.setName(newName);
        }

        Category updatedCategory = categoryRepository.save(existingCategory);
        log.info("Категория ID: {} обновлена", catId);

        return categoryMapper.toCategoryDto(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        log.info("Удаление категории ID: {}", catId);

        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Категория с ID " + catId + " не найдена"
                ));

        if (eventRepository.existsByCategoryId(catId)) {
            throw new CategoryNotEmptyException(
                    String.format("Нельзя удалить категорию '%s' (ID: %d), так как с ней связаны события",
                            category.getName(), catId)
            );
        }

        categoryRepository.delete(category);
        log.info("Категория '{}' (ID: {}) удалена", category.getName(), catId);
    }

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        log.info("Получение списка категорий, from: {}, size: {}", from, size);


        List<Category> allCategories = categoryRepository.findAll();

        int start = from;
        int end = Math.min(from + size, allCategories.size());

        if (start >= allCategories.size()) {
            return List.of();
        }

        List<Category> paginatedCategories = allCategories.subList(start, end);


        return paginatedCategories.stream()
                .map(categoryMapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategoryById(Long catId) {
        log.info("Получение категории по ID: {}", catId);


        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Категория с ID " + catId + " не найдена"
                ));

        return categoryMapper.toCategoryDto(category);
    }
}