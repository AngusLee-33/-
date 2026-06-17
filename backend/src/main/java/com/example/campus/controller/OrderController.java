package com.example.campus.controller;

import com.example.campus.dto.request.OrderCreateRequest;
import com.example.campus.dto.response.ApiResponse;
import com.example.campus.dto.response.OrderResponse;
import com.example.campus.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        OrderResponse order = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("订单创建成功", order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        OrderResponse order = orderService.getOrderResponseById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<OrderResponse> orders = orderService.getOrdersByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<OrderResponse>> payOrder(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        OrderResponse order = orderService.payOrder(userId, id);
        return ResponseEntity.ok(ApiResponse.success("支付成功", order));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> completeOrder(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        OrderResponse order = orderService.completeOrder(userId, id);
        return ResponseEntity.ok(ApiResponse.success("订单完成", order));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        OrderResponse order = orderService.cancelOrder(userId, id);
        return ResponseEntity.ok(ApiResponse.success("订单已取消", order));
    }
}
