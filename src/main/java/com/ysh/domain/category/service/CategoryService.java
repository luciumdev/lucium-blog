package com.ysh.domain.category.service;

import com.ysh.domain.category.dto.CategoryResponse;
import com.ysh.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
