package com.ioes.photo.domain.user.enums;

import com.ioes.photo.global.persistence.enumeration.CodedEnumConverter;
import jakarta.persistence.Converter;

/**
 * 유저 ROLE 컨버터, UserRole Enum을 컨버팅하는 역할
 *
 * @author 황제연
 */
@Converter(autoApply = true)
public class UserRoleConverter extends CodedEnumConverter<UserRole> {
    public UserRoleConverter() {
        super(UserRole.class);
    }
}
