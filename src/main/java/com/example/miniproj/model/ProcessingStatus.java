package com.example.miniproj.model;

import com.example.miniproj.ProcessingStatusEnum;
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
    private ProcessingStatusEnum status;
    private int progress;
    private String error;
    private String videoTitle;
    private String mrPath;
    private String subtitlePath;
}
