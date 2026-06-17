package com.example.campus.controller;

import com.example.campus.dto.request.BackupRequest;
import com.example.campus.dto.request.PasswordUpdateRequest;
import com.example.campus.dto.request.StatusUpdateRequest;
import com.example.campus.dto.response.ApiResponse;
import com.example.campus.dto.response.CarpoolResponse;
import com.example.campus.dto.response.ImportPreviewResponse;
import com.example.campus.dto.response.LostFoundResponse;
import com.example.campus.dto.response.OrderResponse;
import com.example.campus.dto.response.PartTimeResponse;
import com.example.campus.dto.response.SecondhandResponse;
import com.example.campus.dto.response.UserResponse;
import com.example.campus.service.AdminService;
import com.example.campus.service.DatabaseBackupService;
import com.example.campus.service.SpreadsheetImportService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final DatabaseBackupService databaseBackupService;
    private final SpreadsheetImportService spreadsheetImportService;

    public AdminController(
            AdminService adminService,
            DatabaseBackupService databaseBackupService,
            SpreadsheetImportService spreadsheetImportService) {
        this.adminService = adminService;
        this.databaseBackupService = databaseBackupService;
        this.spreadsheetImportService = spreadsheetImportService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getSummary()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUsers()));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("用户状态已更新", adminService.updateUserStatus(id, request.getStatus())));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("用户角色已更新", adminService.updateUserRole(id, request.getStatus())));
    }

    @PutMapping("/users/{id}/password")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserPassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("用户密码已更新", adminService.updateUserPassword(id, request.getPassword())));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id, Authentication authentication) {
        Long currentUserId = (Long) authentication.getPrincipal();
        adminService.deleteUser(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success("用户已删除", null));
    }

    @GetMapping("/carpools")
    public ResponseEntity<ApiResponse<List<CarpoolResponse>>> getCarpools() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getCarpools()));
    }

    @PutMapping("/carpools/{id}/status")
    public ResponseEntity<ApiResponse<CarpoolResponse>> updateCarpoolStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("拼车状态已更新", adminService.updateCarpoolStatus(id, request.getStatus())));
    }

    @DeleteMapping("/carpools/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCarpool(@PathVariable Long id) {
        adminService.deleteCarpool(id);
        return ResponseEntity.ok(ApiResponse.success("拼车信息已删除", null));
    }

    @GetMapping("/part-times")
    public ResponseEntity<ApiResponse<List<PartTimeResponse>>> getPartTimes() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getPartTimes()));
    }

    @PutMapping("/part-times/{id}/status")
    public ResponseEntity<ApiResponse<PartTimeResponse>> updatePartTimeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("兼职状态已更新", adminService.updatePartTimeStatus(id, request.getStatus())));
    }

    @DeleteMapping("/part-times/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePartTime(@PathVariable Long id) {
        adminService.deletePartTime(id);
        return ResponseEntity.ok(ApiResponse.success("兼职信息已删除", null));
    }

    @GetMapping("/secondhands")
    public ResponseEntity<ApiResponse<List<SecondhandResponse>>> getSecondhands() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getSecondhands()));
    }

    @GetMapping("/lost-founds")
    public ResponseEntity<ApiResponse<List<LostFoundResponse>>> getLostFounds() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getLostFounds()));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getOrders()));
    }

    @PutMapping("/secondhands/{id}/status")
    public ResponseEntity<ApiResponse<SecondhandResponse>> updateSecondhandStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("闲置状态已更新", adminService.updateSecondhandStatus(id, request.getStatus())));
    }

    @DeleteMapping("/secondhands/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSecondhand(@PathVariable Long id) {
        adminService.deleteSecondhand(id);
        return ResponseEntity.ok(ApiResponse.success("闲置信息已删除", null));
    }

    @PutMapping("/lost-founds/{id}/status")
    public ResponseEntity<ApiResponse<LostFoundResponse>> updateLostFoundStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("失物招领状态已更新", adminService.updateLostFoundStatus(id, request.getStatus())));
    }

    @DeleteMapping("/lost-founds/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLostFound(@PathVariable Long id) {
        adminService.deleteLostFound(id);
        return ResponseEntity.ok(ApiResponse.success("失物招领信息已删除", null));
    }

    @DeleteMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        adminService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success("订单已删除", null));
    }

    @PostMapping("/backup")
    public ResponseEntity<byte[]> backupDatabase(@RequestBody BackupRequest request) {
        DatabaseBackupService.BackupFile backupFile = databaseBackupService.createBackup(request);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(backupFile.filename())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType("application/sql;charset=UTF-8"))
                .body(backupFile.content());
    }

    @PostMapping(value = "/import/spreadsheet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportPreviewResponse>> importSpreadsheet(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("导入成功", spreadsheetImportService.importSpreadsheet(file)));
    }

    @GetMapping("/import/user-template")
    public ResponseEntity<byte[]> downloadUserImportTemplate() {
        SpreadsheetImportService.TemplateFile templateFile = spreadsheetImportService.createUserImportTemplate();
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(templateFile.filename(), java.nio.charset.StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(templateFile.content());
    }

    @GetMapping("/users/export")
    public ResponseEntity<byte[]> exportUsers() {
        SpreadsheetImportService.TemplateFile exportFile = spreadsheetImportService.exportUsers();
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(exportFile.filename(), java.nio.charset.StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(exportFile.content());
    }
}
