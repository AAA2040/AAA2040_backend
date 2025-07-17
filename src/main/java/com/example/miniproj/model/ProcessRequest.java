package com.example.miniproj.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRequest {

    @NotBlank(message = "YouTube URL은 필수입니다.")
    @Pattern(
            regexp = "^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be)/.+",
            message = "유효한 YouTube URL을 입력해주세요."
    )
    private String Url;
}
