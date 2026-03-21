package com.mgh.backend.tree.repository;

import com.mgh.backend.tree.domain.entity.RegisterForm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegisterFormRepository extends JpaRepository<RegisterForm, Long> {
}

