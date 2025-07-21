package com.example.miniproj.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingStatus {
    private String taskId;
    private String mrPath;
    private String vocalsPath;
    private String lyrics;
}