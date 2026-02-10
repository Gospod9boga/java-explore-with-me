package ru.practicum.mainservice.service;

import ru.practicum.mainservice.dto.request.NewCategoryDto;
import ru.practicum.mainservice.dto.response.CategoryDto;

import java.util.List;

public interface CategoryService  {

    CategoryDto createCategory(NewCategoryDto newCategoryDto);

    CategoryDto updateCategory(Long catId, NewCategoryDto newCategoryDto);

    void deleteCategory(Long catId);

    List<CategoryDto> getCategories(int from, int size);

    CategoryDto getCategoryById(Long catId);
}
