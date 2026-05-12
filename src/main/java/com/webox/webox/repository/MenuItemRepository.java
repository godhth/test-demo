package com.webox.webox.repository;

import com.webox.webox.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    Optional<MenuItem> findByCode(String code);

    List<MenuItem> findByCategory(String category);
}
