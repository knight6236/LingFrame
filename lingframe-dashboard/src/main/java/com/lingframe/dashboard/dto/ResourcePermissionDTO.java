package com.lingframe.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourcePermissionDTO {
    private boolean dbRead;
    private boolean dbWrite;
    private boolean cacheRead;
    private boolean cacheWrite;
    private List<String> ipcServices;
}
