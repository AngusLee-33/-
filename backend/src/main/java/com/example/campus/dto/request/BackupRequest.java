package com.example.campus.dto.request;

import lombok.Data;

@Data
public class BackupRequest {
    private boolean includeStructure = true;
    private boolean includeData = true;
}
