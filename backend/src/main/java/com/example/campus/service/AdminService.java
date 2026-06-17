package com.example.campus.service;

import com.example.campus.dto.response.CarpoolResponse;
import com.example.campus.dto.response.LostFoundResponse;
import com.example.campus.dto.response.OrderResponse;
import com.example.campus.dto.response.PartTimeResponse;
import com.example.campus.dto.response.SecondhandResponse;
import com.example.campus.dto.response.UserResponse;
import com.example.campus.entity.Carpool;
import com.example.campus.entity.LostFound;
import com.example.campus.entity.Order;
import com.example.campus.entity.PartTime;
import com.example.campus.entity.Secondhand;
import com.example.campus.entity.User;
import com.example.campus.exception.BusinessException;
import com.example.campus.exception.ResourceNotFoundException;
import com.example.campus.repository.CarpoolRepository;
import com.example.campus.repository.CarpoolApplyRepository;
import com.example.campus.repository.ChatMessageRepository;
import com.example.campus.repository.LostFoundRepository;
import com.example.campus.repository.OrderRepository;
import com.example.campus.repository.PartTimeApplyRepository;
import com.example.campus.repository.PartTimeRepository;
import com.example.campus.repository.ReviewRepository;
import com.example.campus.repository.SecondhandRepository;
import com.example.campus.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final Set<String> USER_STATUSES = Set.of("active", "disabled");
    private static final Set<String> USER_ROLES = Set.of("student", "merchant", "admin");
    private static final Set<String> CARPOOL_STATUSES = Set.of("pending", "active", "confirmed", "completed", "canceled");
    private static final Set<String> PART_TIME_STATUSES = Set.of("pending", "active", "closed");
    private static final Set<String> SECONDHAND_STATUSES = Set.of("pending", "active", "sold", "offline");
    private static final Set<String> LOST_FOUND_STATUSES = Set.of("open", "resolved", "closed");

    private final UserRepository userRepository;
    private final CarpoolRepository carpoolRepository;
    private final CarpoolApplyRepository carpoolApplyRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PartTimeRepository partTimeRepository;
    private final PartTimeApplyRepository partTimeApplyRepository;
    private final SecondhandRepository secondhandRepository;
    private final LostFoundRepository lostFoundRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageJsonService imageJsonService;

    public AdminService(
            UserRepository userRepository,
            CarpoolRepository carpoolRepository,
            CarpoolApplyRepository carpoolApplyRepository,
            ChatMessageRepository chatMessageRepository,
            PartTimeRepository partTimeRepository,
            PartTimeApplyRepository partTimeApplyRepository,
            SecondhandRepository secondhandRepository,
            LostFoundRepository lostFoundRepository,
            OrderRepository orderRepository,
            ReviewRepository reviewRepository,
            PasswordEncoder passwordEncoder,
            ImageJsonService imageJsonService) {
        this.userRepository = userRepository;
        this.carpoolRepository = carpoolRepository;
        this.carpoolApplyRepository = carpoolApplyRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.partTimeRepository = partTimeRepository;
        this.partTimeApplyRepository = partTimeApplyRepository;
        this.secondhandRepository = secondhandRepository;
        this.lostFoundRepository = lostFoundRepository;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.passwordEncoder = passwordEncoder;
        this.imageJsonService = imageJsonService;
    }

    public Map<String, Long> getSummary() {
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("users", userRepository.count());
        summary.put("pendingCarpools", carpoolRepository.findByStatus("pending").stream().count());
        summary.put("pendingPartTimes", partTimeRepository.findByStatus("pending").stream().count());
        summary.put("pendingSecondhands", secondhandRepository.findByStatus("pending").stream().count());
        return summary;
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    public List<CarpoolResponse> getCarpools() {
        return carpoolRepository.findAll().stream()
                .map(this::toCarpoolResponse)
                .collect(Collectors.toList());
    }

    public List<PartTimeResponse> getPartTimes() {
        return partTimeRepository.findAll().stream()
                .map(this::toPartTimeResponse)
                .collect(Collectors.toList());
    }

    public List<SecondhandResponse> getSecondhands() {
        return secondhandRepository.findAll().stream()
                .map(this::toSecondhandResponse)
                .collect(Collectors.toList());
    }

    public List<LostFoundResponse> getLostFounds() {
        return lostFoundRepository.findAll().stream()
                .map(this::toLostFoundResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getOrders() {
        return orderRepository.findAll().stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateUserStatus(Long id, String status) {
        validateStatus(status, USER_STATUSES);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        user.setStatus(status);
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUserRole(Long id, String role) {
        validateStatus(role, USER_ROLES);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        user.setRole(role);
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUserPassword(Long id, String password) {
        String newPassword = password == null ? "" : password.trim();
        if (newPassword.length() < 6 || newPassword.length() > 50) {
            throw new BusinessException("密码长度必须在 6 到 50 位之间");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException("不能删除当前登录的管理员账号");
        }
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        reviewRepository.deleteByOrder_User_UserId(targetUserId);
        reviewRepository.deleteByReviewer_UserId(targetUserId);
        reviewRepository.deleteByReviewee_UserId(targetUserId);
        chatMessageRepository.deleteBySender_UserIdOrReceiver_UserId(targetUserId, targetUserId);
        orderRepository.deleteByUser_UserId(targetUserId);

        carpoolApplyRepository.deleteByUser_UserId(targetUserId);
        for (Carpool carpool : carpoolRepository.findByUser_UserId(targetUserId)) {
            carpoolApplyRepository.deleteByCarpool_CarpoolId(carpool.getCarpoolId());
            carpoolRepository.delete(carpool);
        }

        partTimeApplyRepository.deleteByUser_UserId(targetUserId);
        for (PartTime partTime : partTimeRepository.findByMerchant_UserId(targetUserId)) {
            partTimeApplyRepository.deleteByPartTime_PartTimeId(partTime.getPartTimeId());
            partTimeRepository.delete(partTime);
        }

        secondhandRepository.findByUser_UserId(targetUserId)
                .forEach(secondhand -> deleteSecondhandWithRelations(secondhand.getSecondhandId(), secondhand));
        lostFoundRepository.findByUser_UserId(targetUserId)
                .forEach(lostFoundRepository::delete);
        userRepository.delete(user);
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        reviewRepository.deleteByOrder_OrderId(orderId);
        orderRepository.delete(order);
    }

    @Transactional
    public void deleteCarpool(Long carpoolId) {
        Carpool carpool = carpoolRepository.findById(carpoolId)
                .orElseThrow(() -> new ResourceNotFoundException("拼车信息不存在"));
        carpoolApplyRepository.deleteByCarpool_CarpoolId(carpoolId);
        chatMessageRepository.deleteByTargetTypeAndTargetId("carpool", carpoolId);
        carpoolRepository.delete(carpool);
    }

    @Transactional
    public void deletePartTime(Long partTimeId) {
        PartTime partTime = partTimeRepository.findById(partTimeId)
                .orElseThrow(() -> new ResourceNotFoundException("兼职信息不存在"));
        partTimeApplyRepository.deleteByPartTime_PartTimeId(partTimeId);
        chatMessageRepository.deleteByTargetTypeAndTargetId("part-time", partTimeId);
        partTimeRepository.delete(partTime);
    }

    @Transactional
    public void deleteSecondhand(Long secondhandId) {
        Secondhand secondhand = secondhandRepository.findById(secondhandId)
                .orElseThrow(() -> new ResourceNotFoundException("闲置物品不存在"));
        deleteSecondhandWithRelations(secondhandId, secondhand);
    }

    @Transactional
    public void deleteLostFound(Long lostFoundId) {
        LostFound lostFound = lostFoundRepository.findById(lostFoundId)
                .orElseThrow(() -> new ResourceNotFoundException("失物招领信息不存在"));
        chatMessageRepository.deleteByTargetTypeAndTargetId("lost-found", lostFoundId);
        lostFoundRepository.delete(lostFound);
    }

    private void deleteSecondhandWithRelations(Long secondhandId, Secondhand secondhand) {
        for (Order order : orderRepository.findByTypeAndTargetId("secondhand", secondhandId)) {
            reviewRepository.deleteByOrder_OrderId(order.getOrderId());
            orderRepository.delete(order);
        }
        chatMessageRepository.deleteByTargetTypeAndTargetId("secondhand", secondhandId);
        secondhandRepository.delete(secondhand);
    }

    @Transactional
    public CarpoolResponse updateCarpoolStatus(Long id, String status) {
        validateStatus(status, CARPOOL_STATUSES);
        Carpool carpool = carpoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("拼车信息不存在"));
        carpool.setStatus(status);
        return toCarpoolResponse(carpoolRepository.save(carpool));
    }

    @Transactional
    public PartTimeResponse updatePartTimeStatus(Long id, String status) {
        validateStatus(status, PART_TIME_STATUSES);
        PartTime partTime = partTimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("兼职信息不存在"));
        partTime.setStatus(status);
        return toPartTimeResponse(partTimeRepository.save(partTime));
    }

    @Transactional
    public SecondhandResponse updateSecondhandStatus(Long id, String status) {
        validateStatus(status, SECONDHAND_STATUSES);
        Secondhand secondhand = secondhandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("闲置物品不存在"));
        secondhand.setStatus(status);
        return toSecondhandResponse(secondhandRepository.save(secondhand));
    }

    @Transactional
    public LostFoundResponse updateLostFoundStatus(Long id, String status) {
        validateStatus(status, LOST_FOUND_STATUSES);
        LostFound lostFound = lostFoundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("失物招领信息不存在"));
        lostFound.setStatus(status);
        return toLostFoundResponse(lostFoundRepository.save(lostFound));
    }

    private void validateStatus(String status, Set<String> allowedStatuses) {
        if (status == null || !allowedStatuses.contains(status)) {
            throw new BusinessException("状态值不合法");
        }
    }

    private UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setIdCard(user.getIdCard());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setAvatar(user.getAvatar());
        response.setCreateTime(user.getCreateTime());
        return response;
    }

    private CarpoolResponse toCarpoolResponse(Carpool carpool) {
        CarpoolResponse response = new CarpoolResponse();
        response.setCarpoolId(carpool.getCarpoolId());
        response.setUser(toUserResponse(carpool.getUser()));
        response.setDeparture(carpool.getDeparture());
        response.setDestination(carpool.getDestination());
        response.setDepartureTime(carpool.getDepartureTime());
        response.setSeats(carpool.getSeats());
        response.setAcceptedCount(carpoolApplyRepository.countByCarpool_CarpoolIdAndStatus(carpool.getCarpoolId(), "accepted"));
        response.setPrice(carpool.getPrice());
        response.setDescription(carpool.getDescription());
        response.setImages(imageJsonService.parse(carpool.getImages()));
        response.setStatus(carpool.getStatus());
        response.setCreateTime(carpool.getCreateTime());
        return response;
    }

    private PartTimeResponse toPartTimeResponse(PartTime partTime) {
        PartTimeResponse response = new PartTimeResponse();
        response.setPartTimeId(partTime.getPartTimeId());
        response.setMerchant(toUserResponse(partTime.getMerchant()));
        response.setTitle(partTime.getTitle());
        response.setDescription(partTime.getDescription());
        response.setSalaryMin(partTime.getSalaryMin());
        response.setSalaryMax(partTime.getSalaryMax());
        response.setRecruitCount(partTime.getRecruitCount() == null || partTime.getRecruitCount() <= 0 ? 1 : partTime.getRecruitCount());
        response.setAcceptedCount(partTimeApplyRepository.countByPartTime_PartTimeIdAndStatus(partTime.getPartTimeId(), "accepted"));
        response.setWorkTime(partTime.getWorkTime());
        response.setRequirements(partTime.getRequirements());
        response.setLocation(partTime.getLocation());
        response.setImages(imageJsonService.parse(partTime.getImages()));
        response.setStatus(partTime.getStatus());
        response.setCreateTime(partTime.getCreateTime());
        return response;
    }

    private SecondhandResponse toSecondhandResponse(Secondhand secondhand) {
        SecondhandResponse response = new SecondhandResponse();
        response.setSecondhandId(secondhand.getSecondhandId());
        response.setUser(toUserResponse(secondhand.getUser()));
        response.setTitle(secondhand.getTitle());
        response.setDescription(secondhand.getDescription());
        response.setPrice(secondhand.getPrice());
        response.setCategory(secondhand.getCategory());
        response.setImages(imageJsonService.parse(secondhand.getImages()));
        response.setStatus(secondhand.getStatus());
        response.setCreateTime(secondhand.getCreateTime());
        return response;
    }

    private LostFoundResponse toLostFoundResponse(LostFound item) {
        LostFoundResponse response = new LostFoundResponse();
        response.setLostFoundId(item.getLostFoundId());
        response.setUser(toUserResponse(item.getUser()));
        response.setType(item.getType());
        response.setTitle(item.getTitle());
        response.setDescription(item.getDescription());
        response.setLocation(item.getLocation());
        response.setEventTime(item.getEventTime());
        response.setContact(item.getContact());
        response.setImages(imageJsonService.parse(item.getImages()));
        response.setStatus(item.getStatus());
        response.setCreateTime(item.getCreateTime());
        return response;
    }

    private OrderResponse toOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setType(order.getType());
        response.setTargetId(order.getTargetId());
        response.setAmount(order.getAmount());
        response.setStatus(order.getStatus());
        response.setUser(toUserResponse(order.getUser()));
        if ("secondhand".equals(order.getType())) {
            secondhandRepository.findById(order.getTargetId()).ifPresent(secondhand -> {
                response.setTargetTitle(secondhand.getTitle());
                response.setSeller(toUserResponse(secondhand.getUser()));
                List<String> images = imageJsonService.parse(secondhand.getImages());
                response.setTargetImage(images.isEmpty() ? "" : images.get(0));
            });
        }
        response.setPayTime(order.getPayTime());
        response.setCompleteTime(order.getCompleteTime());
        response.setCreateTime(order.getCreateTime());
        return response;
    }
}
