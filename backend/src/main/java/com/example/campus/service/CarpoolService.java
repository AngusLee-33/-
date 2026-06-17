package com.example.campus.service;

import com.example.campus.dto.request.CarpoolRequest;
import com.example.campus.dto.response.CarpoolApplyResponse;
import com.example.campus.dto.response.CarpoolResponse;
import com.example.campus.dto.response.UserResponse;
import com.example.campus.entity.Carpool;
import com.example.campus.entity.CarpoolApply;
import com.example.campus.entity.User;
import com.example.campus.exception.BusinessException;
import com.example.campus.exception.ResourceNotFoundException;
import com.example.campus.repository.CarpoolApplyRepository;
import com.example.campus.repository.CarpoolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;

@Service
public class CarpoolService {

    private final CarpoolRepository carpoolRepository;
    private final CarpoolApplyRepository carpoolApplyRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ImageJsonService imageJsonService;

    public CarpoolService(
            CarpoolRepository carpoolRepository,
            CarpoolApplyRepository carpoolApplyRepository,
            UserService userService,
            NotificationService notificationService,
            ImageJsonService imageJsonService) {
        this.carpoolRepository = carpoolRepository;
        this.carpoolApplyRepository = carpoolApplyRepository;
        this.userService = userService;
        this.notificationService = notificationService;
        this.imageJsonService = imageJsonService;
    }

    @Transactional
    public CarpoolResponse createCarpool(Long userId, CarpoolRequest request) {
        User user = userService.getCurrentUser(userId);
        validateCarpoolRequest(request);

        Carpool carpool = new Carpool();
        carpool.setUser(user);
        carpool.setDeparture(request.getDeparture().trim());
        carpool.setDestination(request.getDestination().trim());
        carpool.setDepartureTime(request.getDepartureTime());
        carpool.setSeats(request.getSeats());
        carpool.setPrice(request.getPrice());
        carpool.setDescription(normalizeBlank(request.getDescription()));
        carpool.setImages(imageJsonService.stringify(request.getImages()));
        carpool.setStatus("pending");

        Carpool savedCarpool = carpoolRepository.save(carpool);
        return convertToResponse(savedCarpool);
    }

    public List<CarpoolResponse> getAllCarpools() {
        return carpoolRepository.findByStatus("active").stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<CarpoolResponse> searchCarpools(String departure, String destination, LocalDateTime startTime, LocalDateTime endTime) {
        return carpoolRepository.findByStatus("active").stream()
                .filter(item -> contains(item.getDeparture(), departure))
                .filter(item -> contains(item.getDestination(), destination))
                .filter(item -> startTime == null || !item.getDepartureTime().isBefore(startTime))
                .filter(item -> endTime == null || !item.getDepartureTime().isAfter(endTime))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public CarpoolResponse getCarpoolById(@NonNull Long id) {
        Carpool carpool = carpoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("拼车信息不存在"));
        return convertToResponse(carpool);
    }

    public List<CarpoolResponse> getCarpoolsByUser(Long userId) {
        return carpoolRepository.findByUser_UserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<CarpoolApplyResponse> getReceivedApplications(Long userId) {
        return carpoolRepository.findByUser_UserId(userId).stream()
                .flatMap(carpool -> carpoolApplyRepository.findByCarpool_CarpoolId(carpool.getCarpoolId()).stream())
                .map(this::convertToApplyResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void applyCarpool(Long userId, @NonNull Long carpoolId) {
        Carpool carpool = carpoolRepository.findById(carpoolId)
                .orElseThrow(() -> new ResourceNotFoundException("拼车信息不存在"));

        if (carpool.getUser().getUserId().equals(userId)) {
            throw new BusinessException("不能申请自己发布的拼车");
        }

        if (!"active".equals(carpool.getStatus())) {
            throw new BusinessException("该拼车已关闭");
        }

        if (carpool.getSeats() == null || carpool.getSeats() <= 0) {
            throw new BusinessException("座位已满");
        }

        User user = userService.getCurrentUser(userId);

        CarpoolApply apply = new CarpoolApply();
        apply.setCarpool(carpool);
        apply.setUser(user);
        apply.setStatus("pending");

        carpoolApplyRepository.save(apply);
        notificationService.create(
                carpool.getUser().getUserId(),
                "收到新的拼车申请",
                user.getUsername() + " 申请加入你的拼车：" + carpool.getDeparture() + " → " + carpool.getDestination(),
                "carpool",
                "carpool",
                carpool.getCarpoolId());
    }

    @Transactional
    public void handleApply(Long userId, @NonNull Long carpoolId, @NonNull Long applyId, String status) {
        Carpool carpool = carpoolRepository.findById(carpoolId)
                .orElseThrow(() -> new ResourceNotFoundException("拼车信息不存在"));

        if (!carpool.getUser().getUserId().equals(userId)) {
            throw new BusinessException("无权操作此拼车");
        }

        CarpoolApply apply = carpoolApplyRepository.findById(applyId)
                .orElseThrow(() -> new ResourceNotFoundException("申请不存在"));

        if (!"pending".equals(apply.getStatus())) {
            throw new BusinessException("申请已处理");
        }

        if (!"accepted".equals(status) && !"rejected".equals(status)) {
            throw new BusinessException("申请状态不合法");
        }

        if ("accepted".equals(status)) {
            if (carpool.getSeats() == null || carpool.getSeats() <= 0) {
                throw new BusinessException("座位已满");
            }
            carpool.setSeats(carpool.getSeats() - 1);
            if (carpool.getSeats() <= 0) {
                carpool.setStatus("confirmed");
            }
            carpoolRepository.save(carpool);
        }

        apply.setStatus(status);
        carpoolApplyRepository.save(apply);
        notificationService.create(
                apply.getUser().getUserId(),
                "拼车申请已处理",
                "你的拼车申请已" + ("accepted".equals(status) ? "同意" : "拒绝"),
                "carpool",
                "carpool",
                carpool.getCarpoolId());
    }

    @Transactional
    public void cancelCarpool(Long userId, Long carpoolId) {
        Carpool carpool = carpoolRepository.findById(carpoolId)
                .orElseThrow(() -> new ResourceNotFoundException("拼车信息不存在"));

        if (!carpool.getUser().getUserId().equals(userId)) {
            throw new BusinessException("无权取消此拼车");
        }
        if ("completed".equals(carpool.getStatus())) {
            throw new BusinessException("已完成的拼车不能取消");
        }
        if ("canceled".equals(carpool.getStatus())) {
            throw new BusinessException("该拼车已取消");
        }

        carpool.setStatus("canceled");
        carpoolRepository.save(carpool);
        carpoolApplyRepository.findByCarpool_CarpoolId(carpoolId).forEach(apply ->
                notificationService.create(
                        apply.getUser().getUserId(),
                        "拼车已取消",
                        "你申请的拼车已被发布者取消",
                        "carpool",
                        "carpool",
                        carpoolId));
    }

    private CarpoolResponse convertToResponse(Carpool carpool) {
        CarpoolResponse response = new CarpoolResponse();
        response.setCarpoolId(carpool.getCarpoolId());
        response.setUser(convertToUserResponse(carpool.getUser()));
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

    private CarpoolApplyResponse convertToApplyResponse(CarpoolApply apply) {
        Carpool carpool = apply.getCarpool();
        CarpoolApplyResponse response = new CarpoolApplyResponse();
        response.setApplyId(apply.getApplyId());
        response.setCarpoolId(carpool.getCarpoolId());
        response.setDeparture(carpool.getDeparture());
        response.setDestination(carpool.getDestination());
        response.setApplicant(convertToUserResponse(apply.getUser()));
        response.setStatus(apply.getStatus());
        response.setCreateTime(apply.getCreateTime());
        return response;
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

    private void validateCarpoolRequest(CarpoolRequest request) {
        String departure = request.getDeparture().trim();
        String destination = request.getDestination().trim();
        LocalDateTime departureTime = request.getDepartureTime();
        Integer seats = request.getSeats();
        BigDecimal price = request.getPrice();

        if (departure.equals(destination)) {
            throw new BusinessException("出发地和目的地不能相同");
        }
        if (departureTime.isBefore(LocalDateTime.now().plusMinutes(5))) {
            throw new BusinessException("出发时间必须晚于当前时间");
        }
        if (seats == null || seats <= 0) {
            throw new BusinessException("座位数必须大于 0");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("价格必须大于 0");
        }
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean contains(String value, String keyword) {
        return keyword == null || keyword.isBlank() || (value != null && value.contains(keyword.trim()));
    }
}
