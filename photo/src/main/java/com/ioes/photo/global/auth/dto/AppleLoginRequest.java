package com.ioes.photo.global.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Apple 네이티브 SDK 로그인 요청 DTO.
 *
 * @author 황제연
 */
@Schema(description = "Apple 네이티브 SDK 로그인 요청")
public record AppleLoginRequest(

    @Schema(description = "Apple SDK가 발급한 identity token (RS256 JWT)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Apple identity token은 필수입니다.")
    String identityToken,

    @Schema(description = "최초 로그인 시 Apple SDK가 전달하는 사용자 정보 (이후 로그인 시 null)")
    AppleUser user

) {

    @Schema(description = "Apple 사용자 정보")
    public record AppleUser(
        @Schema(description = "이름 (최초 로그인 시만 제공됨)") AppleName name,
        @Schema(description = "이메일") String email
    ) {}

    @Schema(description = "Apple 사용자 이름")
    public record AppleName(
        @Schema(description = "이름") String firstName,
        @Schema(description = "성") String lastName
    ) {}
}
