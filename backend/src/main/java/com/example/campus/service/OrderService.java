package com.example.campus.service;

import com.example.campus.dto.request.OrderCreateRequest;
import com.example.campus.dto.response.OrderResponse;
import com.example.campus.dto.response.UserResponse;
import com.example.campus.entity.Order;
import com.example.campus.entity.Secondhand;
import com.example.campus.entity.User;
import com.example.campus.exception.BusinessException;
import com.example.campus.exception.ResourceNotFoundException;
import com.example.campus.repository.OrderRepository;
import com.example.campus.repository.SecondhandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserService userService;
    private final SecondhandRepository secondhandRepository;
    private final NotificationService notificationService;
    private final ImageJsonService imageJsonService;

    public OrderService(
            OrderRepository orderRepository,
            UserService userService,
            SecondhandRepository secondhandRepository,
            NotificationService notificationService,
            ImageJsonService imageJsonService) {
        this.orderRepository = orderRepository;
        this.userService = userService;
        this.secondhandRepository = secondhandRepository;
        this.notificationService = notificationService;
        this.imageJsonService = imageJsonService;
    }

    @Transactional
    public OrderResponse createOrder(Long userId, OrderCreateRequest request) {
        User user = userService.getCurrentUser(userId);
        validateOrderRequest(userId, request);

        Order order = new Order();
        order.setType(request.getType());
        order.setUser(user);
        order.setTargetId(request.getTargetId());
        order.setAmount(resolveOrderAmount(request));
        order.setStatus("created");

        Order savedOrder = orderRepository.save(order);
        notifyOrderCreated(savedOrder);
        return convertToResponse(savedOrder);
    }

    public Order getOrderById(@NonNull Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
    }

    public OrderResponse getOrderResponseById(Long userId, @NonNull Long id) {
        Order order = getOrderById(id);
        ensureOwner(userId, order);
        return convertToResponse(order);
    }

    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUser_UserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse payOrder(Long userId, Long orderId) {
        Order order = getOrderById(orderId);
        ensureOwner(userId, order);

        if (!"created".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许支付");
        }

        order.setStatus("payed");
        order.setPayTime(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        markSecondhandAsSold(savedOrder);
        notifyOrderChanged(savedOrder, "订单已支付", "你的交易订单已完成模拟支付");
        return convertToResponse(savedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = getOrderById(orderId);
        ensureOwner(userId, order);

        if (!"created".equals(order.getStatus())) {
            throw new BusinessException("只有待支付订单可以取消");
        }

        order.setStatus("canceled");
        Order savedOrder = orderRepository.save(order);
        notifyOrderChanged(savedOrder, "订单已取消", "买家已取消该交易订单");
        return convertToResponse(savedOrder);
    }

    @Transactional
    public OrderResponse completeOrder(Long userId, Long orderId) {
        Order order = getOrderById(orderId);
        ensureOwner(userId, order);

        if (!"payed".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许完成");
        }

        order.setStatus("completed");
        order.setCompleteTime(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        notifyOrderChanged(savedOrder, "订单已完成", "你的交易订单已完成，可以提醒对方评价");
        return convertToResponse(savedOrder);
    }

    private void validateOrderRequest(Long userId, OrderCreateRequest request) {
        if (!"secondhand".equals(request.getType())) {
            throw new BusinessException("当前仅支持闲置交易订单");
        }
        Secondhand secondhand = secondhandRepository.findById(request.getTargetId())
                .orElseThrow(() -> new ResourceNotFoundException("闲置物品不存在"));
        if (secondhand.getUser().getUserId().equals(userId)) {
            throw new BusinessException("不能购买自己发布的闲置物品");
        }
        if (!"active".equals(secondhand.getStatus()) && !"pending".equals(secondhand.getStatus())) {
            throw new BusinessException("该物品当前不可购买");
        }
        BigDecimal requestAmount = request.getAmount();
        if (requestAmount == null || requestAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("订单金额不合法");
        }
    }

    private BigDecimal resolveOrderAmount(OrderCreateRequest request) {
        if ("secondhand".equals(request.getType())) {
            return secondhandRepository.findById(request.getTargetId())
                    .map(Secondhand::getPrice)
                    .orElse(request.getAmount());
        }
        return request.getAmount();
    }

    private void ensureOwner(Long userId, Order order) {
        if (!order.getUser().getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
    }

    private void notifyOrderCreated(Order order) {
        if ("secondhand".equals(order.getType())) {
            secondhandRepository.findById(order.getTargetId()).ifPresent(secondhand ->
                    notificationService.create(
                            secondhand.getUser().getUserId(),
                            "收到新的购买订单",
                            order.getUser().getUsername() + " 对你的闲置物品发起了购买订单",
                            "order",
                            "secondhand",
                            order.getTargetId()));
        }
    }

    private void notifyOrderChanged(Order order, String title, String content) {
        if ("secondhand".equals(order.getType())) {
            secondhandRepository.findById(order.getTargetId()).ifPresent(secondhand ->
                    notificationService.create(
                            secondhand.getUser().getUserId(),
                            title,
                            content,
                            "order",
                            "secondhand",
                            order.getTargetId()));
        }
    }

    private void markSecondhandAsSold(Order order) {
        if ("secondhand".equals(order.getType())) {
            secondhandRepository.findById(order.getTargetId()).ifPresent(secondhand -> {
                secondhand.setStatus("sold");
                secondhandRepository.save(secondhand);
            });
        }
    }

    private OrderResponse convertToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setType(order.getType());
        response.setTargetId(order.getTargetId());
        response.setAmount(order.getAmount());
        response.setStatus(order.getStatus());
        response.setUser(convertToUserResponse(order.getUser()));
        fillTargetInfo(response, order);
        response.setPayTime(order.getPayTime());
        response.setCompleteTime(order.getCompleteTime());
        response.setCreateTime(order.getCreateTime());
        return response;
    }

    private void fillTargetInfo(OrderResponse response, Order order) {
        if ("secondhand".equals(order.getType())) {
            secondhandRepository.findById(order.getTargetId()).ifPresent(secondhand -> {
                response.setTargetTitle(secondhand.getTitle());
                response.setSeller(convertToUserResponse(secondhand.getUser()));
                List<String> images = imageJsonService.parse(secondhand.getImages());
                response.setTargetImage(images.isEmpty() ? "" : images.get(0));
            });
        }
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        return response;
    }
}
