package com.ioes.photo.global.auth.oauth;

import com.ioes.photo.global.persistence.enumeration.CodedEnumConverter;
import jakarta.persistence.Converter;

/**
 * @author 김성민
 */
@Converter(autoApply = true)
public class OAuthProviderConverter extends CodedEnumConverter<OAuthProvider> {
    public OAuthProviderConverter() {
        super(OAuthProvider.class);
    }
}
