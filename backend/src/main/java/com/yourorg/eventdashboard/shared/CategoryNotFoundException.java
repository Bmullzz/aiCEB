package com.yourorg.eventdashboard.shared;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(UUID id) {
        super("Category not found: " + id);
    }
}
