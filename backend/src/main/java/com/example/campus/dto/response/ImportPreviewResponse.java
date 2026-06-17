package com.example.campus.dto.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ImportPreviewResponse {
    private String filename;
    private String sheetName;
    private List<String> columns;
    private List<Map<String, String>> rows;
    private int rowCount;
    private int successCount;
    private int skippedCount;
}
