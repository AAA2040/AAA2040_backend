package com.example.miniproj.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallbackResponse {
    private String result;
    private String uriId;

    @JsonProperty("no_vocals_url")
    private String noVocalsUrl;

    @JsonProperty("vocals_url")
    private String vocalsUrl;

    private String lyrics;
}
