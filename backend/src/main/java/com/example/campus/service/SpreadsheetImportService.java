package com.example.campus.service;

import com.example.campus.dto.response.ImportPreviewResponse;
import com.example.campus.entity.User;
import com.example.campus.exception.BusinessException;
import com.example.campus.repository.UserRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SpreadsheetImportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String RESULT_COLUMN = "导入结果";

    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public SpreadsheetImportService(
            UserRepository userRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public TemplateFile createUserImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("用户导入模板");
            String[] headers = {"用户名", "密码", "姓名", "手机号", "邮箱", "角色"};
            String[] examples = {"zhangsan", "123456", "张三", "13800000000", "zhangsan@example.com", "学生"};

            CellStyle headerStyle = createHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            Row exampleRow = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell headerCell = headerRow.createCell(i);
                headerCell.setCellValue(headers[i]);
                headerCell.setCellStyle(headerStyle);
                exampleRow.createCell(i).setCellValue(examples[i]);
                sheet.setColumnWidth(i, 18 * 256);
            }

            workbook.write(outputStream);
            return new TemplateFile("用户信息导入模板.xlsx", outputStream.toByteArray());
        } catch (IOException ex) {
            throw new BusinessException("生成用户导入模板失败：" + ex.getMessage());
        }
    }

    public TemplateFile exportUsers() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("用户信息");
            String[] headers = {"用户ID", "用户名", "姓名", "手机号", "邮箱", "角色", "状态"};
            CellStyle headerStyle = createHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 18 * 256);
            }

            List<User> users = userRepository.findAll();
            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue("U" + String.format("%06d", user.getUserId()));
                row.createCell(1).setCellValue(defaultString(user.getUsername()));
                row.createCell(2).setCellValue(defaultString(user.getRealName()));
                row.createCell(3).setCellValue(defaultString(user.getPhone()));
                row.createCell(4).setCellValue(defaultString(user.getEmail()));
                row.createCell(5).setCellValue(roleText(user.getRole()));
                row.createCell(6).setCellValue(statusText(user.getStatus()));
            }

            workbook.write(outputStream);
            return new TemplateFile("用户信息导出.xlsx", outputStream.toByteArray());
        } catch (IOException ex) {
            throw new BusinessException("导出用户信息失败：" + ex.getMessage());
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        return headerStyle;
    }

    @Transactional
    public ImportPreviewResponse importSpreadsheet(MultipartFile file) {
        validateFile(file);

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = findHeaderRow(sheet);
            List<String> columns = readColumns(headerRow);
            if (columns.isEmpty()) {
                throw new BusinessException("电子表格第一行未读取到有效列名");
            }

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<Map<String, String>> rows = readRows(sheet, headerRow.getRowNum() + 1, columns, formatter, evaluator);
            ImportStats stats = importUsers(rows);
            List<String> displayColumns = new ArrayList<>(columns);
            if (!displayColumns.contains(RESULT_COLUMN)) {
                displayColumns.add(RESULT_COLUMN);
            }

            ImportPreviewResponse response = new ImportPreviewResponse();
            response.setFilename(file.getOriginalFilename());
            response.setSheetName(sheet.getSheetName());
            response.setColumns(displayColumns);
            response.setRows(rows);
            response.setRowCount(rows.size());
            response.setSuccessCount(stats.successCount());
            response.setSkippedCount(stats.skippedCount());
            return response;
        } catch (IOException ex) {
            throw new BusinessException("读取电子表格失败：" + ex.getMessage());
        } catch (Exception ex) {
            if (ex instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException("导入电子表格失败：" + ex.getMessage());
        }
    }

    private ImportStats importUsers(List<Map<String, String>> rows) {
        int successCount = 0;
        int skippedCount = 0;
        for (Map<String, String> row : rows) {
            try {
                String username = getValue(row, "用户名", "账号", "用户账号", "username", "user_name");
                if (isBlank(username)) {
                    skippedCount++;
                    row.put(RESULT_COLUMN, "跳过：用户名为空");
                    continue;
                }
                username = username.trim();
                if (userRepository.existsByUsername(username)) {
                    skippedCount++;
                    row.put(RESULT_COLUMN, "跳过：用户名已存在");
                    continue;
                }

                String phone = normalizeBlank(getValue(row, "手机号", "手机", "电话", "phone"));
                if (phone != null && userRepository.existsByPhone(phone)) {
                    skippedCount++;
                    row.put(RESULT_COLUMN, "跳过：手机号已存在");
                    continue;
                }

                String email = normalizeBlank(getValue(row, "邮箱", "电子邮箱", "email"));
                if (email != null && userRepository.existsByEmail(email)) {
                    skippedCount++;
                    row.put(RESULT_COLUMN, "跳过：邮箱已存在");
                    continue;
                }

                String password = normalizeBlank(getValue(row, "密码", "password"));
                if (password == null) {
                    password = "123456";
                }
                if (password.length() < 6 || password.length() > 50) {
                    skippedCount++;
                    row.put(RESULT_COLUMN, "跳过：密码长度需为 6 到 50 位");
                    continue;
                }

                User user = new User();
                user.setUsername(username);
                user.setPassword(passwordEncoder.encode(password));
                user.setRealName(normalizeBlank(getValue(row, "姓名", "真实姓名", "realName", "real_name")));
                user.setPhone(phone);
                user.setEmail(email);
                user.setRole(normalizeRole(getValue(row, "角色", "role")));
                user.setStatus("active");
                user.setIdCard(normalizeBlank(getValue(row, "身份证", "身份证号", "idCard", "id_card")));
                userRepository.save(user);
                successCount++;
                row.put(RESULT_COLUMN, "成功");
            } catch (Exception ex) {
                skippedCount++;
                row.put(RESULT_COLUMN, "跳过：" + ex.getMessage());
            }
        }
        return new ImportStats(successCount, skippedCount);
    }

    private String getValue(Map<String, String> row, String... names) {
        for (String name : names) {
            for (Map.Entry<String, String> entry : row.entrySet()) {
                if (normalizeKey(entry.getKey()).equals(normalizeKey(name))) {
                    return entry.getValue();
                }
            }
        }
        return "";
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.replace(" ", "").replace("_", "").toLowerCase();
    }

    private String normalizeBlank(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeRole(String value) {
        String role = normalizeBlank(value);
        if (role == null || "学生".equals(role)) {
            return "student";
        }
        if ("商家".equals(role)) {
            return "merchant";
        }
        if ("管理员".equals(role)) {
            return "admin";
        }
        return switch (role.toLowerCase()) {
            case "merchant", "admin" -> role.toLowerCase();
            default -> "student";
        };
    }

    private String roleText(String role) {
        return switch (role == null ? "" : role) {
            case "admin" -> "管理员";
            case "merchant" -> "商家";
            default -> "学生";
        };
    }

    private String statusText(String status) {
        return "disabled".equals(status) ? "禁用" : "正常";
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的电子表格文件");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".xls") && !filename.endsWith(".xlsx")) {
            throw new BusinessException("只支持导入 .xls 或 .xlsx 文件");
        }
    }

    private Row findHeaderRow(Sheet sheet) {
        for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && !isRowEmpty(row)) {
                return row;
            }
        }
        throw new BusinessException("电子表格为空，无法导入");
    }

    private List<String> readColumns(Row headerRow) {
        List<String> columns = new ArrayList<>();
        short lastCellNum = headerRow.getLastCellNum();
        for (int i = 0; i < lastCellNum; i++) {
            Cell cell = headerRow.getCell(i);
            String column = normalizeColumnName(cell == null ? "" : cell.toString(), i);
            columns.add(column);
        }
        return columns;
    }

    private List<Map<String, String>> readRows(
            Sheet sheet,
            int startRow,
            List<String> columns,
            DataFormatter formatter,
            FormulaEvaluator evaluator) {
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) {
                continue;
            }

            Map<String, String> values = new LinkedHashMap<>();
            boolean hasValue = false;
            for (int j = 0; j < columns.size(); j++) {
                String value = readCellValue(row.getCell(j), formatter, evaluator);
                if (!value.isBlank()) {
                    hasValue = true;
                }
                values.put(columns.get(j), value);
            }
            if (hasValue) {
                rows.add(values);
            }
        }
        return rows;
    }

    private String normalizeColumnName(String value, int index) {
        String columnName = value == null ? "" : value.trim();
        return columnName.isBlank() ? "未命名列" + (index + 1) : columnName;
    }

    private String readCellValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .format(DATE_TIME_FORMATTER);
        }
        return formatter.formatCellValue(cell, evaluator).trim();
    }

    private boolean isRowEmpty(Row row) {
        short lastCellNum = row.getLastCellNum();
        if (lastCellNum < 0) {
            return true;
        }
        for (int i = 0; i < lastCellNum; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK && !cell.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private record ImportStats(int successCount, int skippedCount) {
    }

    public record TemplateFile(String filename, byte[] content) {
    }
}
