package com.mgh.backend.tree.mapper;

import com.mgh.backend.tree.domain.dto.RegisterFormDto;
import com.mgh.backend.tree.domain.entity.RegisterForm;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RegisterFormMapper {

    RegisterFormDto toDto(RegisterForm registerForm);
}

