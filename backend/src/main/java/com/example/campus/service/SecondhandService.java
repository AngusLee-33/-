package com.example.campus.service;

import com.example.campus.dto.request.SecondhandRequest;
import com.example.campus.dto.response.SecondhandResponse;
import com.example.campus.dto.response.UserResponse;
import com.example.campus.entity.Secondhand;
import com.example.campus.entity.User;
import com.example.campus.exception.BusinessException;
import com.example.campus.exception.ResourceNotFoundException;
import com.example.campus.repository.SecondhandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SecondhandService {

    private final SecondhandRepository secondhandRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ImageJsonService imageJsonService;

    public SecondhandService(
            SecondhandRepository secondhandRepository,
            UserService userService,
            NotificationService notificationService,
            ImageJsonService imageJsonService) {
        this.secondhandRepository = secondhandRepository;
        this.userService = userService;
        this.notificationService = notificationService;
        this.imageJsonService = imageJsonService;
    }

    @Transactional
    public SecondhandResponse createSecondhand(Long userId, SecondhandRequest request) {
        User user = userService.getCurrentUser(userId);
        validateSecondhandRequest(request);

        Secondhand secondhand = new Secondhand();
        secondhand.setUser(user);
        secondhand.setTitle(request.getTitle().trim());
        secondhand.setDescription(normalizeBlank(request.getDescription()));
        secondhand.setPrice(request.getPrice());
        secondhand.setCategory(request.getCategory().trim());
        secondhand.setImages(imageJsonService.stringify(request.getImages()));
        secondhand.setStatus("pending");

        Secondhand savedSecondhand = secondhandRepository.save(secondhand);
        return convertToResponse(savedSecondhand);
    }

    public List<SecondhandResponse> getAllSecondhands() {
        return secondhandRepository.findByStatusNot("offline").stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<SecondhandResponse> searchSecondhands(String category, String keyword, BigDecimal minPrice, BigDecimal maxPrice) {
        return secondhandRepository.findByStatusNot("offline").stream()
                .filter(item -> category == null || category.isBlank() || category.equals(item.getCategory()))
                .filter(item -> matchesKeyword(item, keyword))
                .filter(item -> minPrice == null || item.getPrice().compareTo(minPrice) >= 0)
                .filter(item -> maxPrice == null || item.getPrice().compareTo(maxPrice) <= 0)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public SecondhandResponse getSecondhandById(Long id) {
        Secondhand secondhand = secondhandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("闲置物品不存在"));
        return convertToResponse(secondhand);
    }

    public List<SecondhandResponse> getSecondhandsByUser(Long userId) {
        return secondhandRepository.findByUser_UserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<SecondhandResponse> getSecondhandsByCategory(String category) {
        return secondhandRepository.findByCategory(category).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsSold(Long userId, Long secondhandId) {
        Secondhand secondhand = secondhandRepository.findById(secondhandId)
                .orElseThrow(() -> new ResourceNotFoundException("闲置物品不存在"));

        if (!secondhand.getUser().getUserId().equals(userId)) {
            throw new BusinessException("无权操作此物品");
        }

        if ("sold".equals(secondhand.getStatus())) {
            throw new BusinessException("该物品已标记为售出");
        }

        secondhand.setStatus("sold");
        secondhandRepository.save(secondhand);
        notificationService.create(
                secondhand.getUser().getUserId(),
                "闲置物品已标记售出",
                "你的闲置物品“" + secondhand.getTitle() + "”已标记售出",
                "secondhand",
                "secondhand",
                secondhandId);
    }

    @Transactional
    public void offlineSecondhand(Long userId, Long secondhandId) {
        Secondhand secondhand = secondhandRepository.findById(secondhandId)
                .orElseThrow(() -> new ResourceNotFoundException("闲置物品不存在"));

        if (!secondhand.getUser().getUserId().equals(userId)) {
            throw new BusinessException("无权下架此物品");
        }
        if ("offline".equals(secondhand.getStatus())) {
            throw new BusinessException("该物品已下架");
        }

        secondhand.setStatus("offline");
        secondhandRepository.save(secondhand);
        notificationService.create(
                secondhand.getUser().getUserId(),
                "闲置物品已下架",
                "你的闲置物品“" + secondhand.getTitle() + "”已下架",
                "secondhand",
                "secondhand",
                secondhandId);
    }

    private boolean matchesKeyword(Secondhand item, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String text = keyword.trim();
        return contains(item.getTitle(), text)
                || contains(item.getDescription(), text)
                || contains(item.getCategory(), text);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private SecondhandResponse convertToResponse(Secondhand secondhand) {
        SecondhandResponse response = new SecondhandResponse();
        response.setSecondhandId(secondhand.getSecondhandId());
        response.setUser(convertToUserResponse(secondhand.getUser()));
        response.setTitle(secondhand.getTitle());
        response.setDescription(secondhand.getDescription());
        response.setPrice(secondhand.getPrice());
        response.setCategory(secondhand.getCategory());
        
        response.setImages(imageJsonService.parse(secondhand.getImages()));
        
        response.setStatus(secondhand.getStatus());
        response.setCreateTime(secondhand.getCreateTime());
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

    private void validateSecondhandRequest(SecondhandRequest request) {
        BigDecimal price = request.getPrice();

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("价格必须大于 0");
        }
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
