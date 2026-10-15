package com.lostfound.config;

import com.lostfound.entity.Category;
import com.lostfound.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CategorySeedConfig {
    @Bean
    CommandLineRunner seedDefaultCategories(CategoryRepository repository) {
        return args -> {
            for (String name : List.of("Electronics", "Bags", "Documents", "Keys", "Clothing", "Accessories", "Other")) {
                if (!repository.existsByNameIgnoreCase(name)) {
                    Category category = new Category();
                    category.setName(name);
                    category.setDescription("Items categorized as " + name.toLowerCase());
                    category.setActive(true);
                    repository.save(category);
                }
            }
        };
    }
}
