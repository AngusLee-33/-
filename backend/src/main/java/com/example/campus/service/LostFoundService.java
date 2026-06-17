package com.example.campus.service;

import com.example.campus.dto.request.LostFoundRequest;
import com.example.campus.dto.response.LostFoundResponse;
import com.example.campus.dto.response.UserResponse;
import com.example.campus.entity.LostFound;
import com.example.campus.entity.User;
import com.example.campus.exception.BusinessException;
import com.example.campus.exception.ResourceNotFoundException;
import com.example.campus.repository.LostFoundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LostFoundService {

    private final LostFoundRepository lostFoundRepository;
    private final UserService userService;
    private final ImageJsonService imageJsonService;

    public LostFoundService(
            LostFoundRepository lostFoundRepository,
            UserService userService,
            ImageJsonService imageJsonService) {
        this.lostFoundRepository = lostFoundRepository;
        this.userService = userService;
        this.imageJsonService = imageJsonService;
    }

    @Transactional
    public LostFoundResponse createLostFound(Long userId, LostFoundRequest request) {
        validateRequest(request);
        User user = userService.getCurrentUser(userId);

        LostFound item = new LostFound();
        item.setUser(user);
        item.setType(request.getType().trim());
        item.setTitle(request.getTitle().trim());
        item.setDescription(request.getDescription().trim());
        item.setLocation(request.getLocation().trim());
        item.setEventTime(request.getEventTime());
        item.setContact(request.getContact().trim());
        item.setImages(imageJsonService.stringify(request.getImages()));
        item.setStatus("open");

        return convertToResponse(lostFoundRepository.save(item));
    }

    public List<LostFoundResponse> getOpenItems() {
        return lostFoundRepository.findByStatus("open").stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<LostFoundResponse> getOpenItemsByType(String type) {
        if (!"lost".equals(type) && !"found".equals(type)) {
            throw new BusinessException("类型不合法");
        }
        return lostFoundRepository.findByTypeAndStatus(type, "open").stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<LostFoundResponse> getMyItems(Long userId) {
        return lostFoundRepository.findByUser_UserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void resolveItem(Long userId, Long id) {
        updateStatus(userId, id, "resolved", "该信息已解决");
    }

    @Transactional
    public void closeItem(Long userId, Long id) {
        updateStatus(userId, id, "closed", "该信息已关闭");
    }

    private void updateStatus(Long userId, Long id, String status, String duplicateMessage) {
        LostFound item = lostFoundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("失物招领信息不存在"));
        if (!item.getUser().getUserId().equals(userId)) {
            throw new BusinessException("无权操作此信息");
        }
        if (status.equals(item.getStatus())) {
            throw new BusinessException(duplicateMessage);
        }
        item.setStatus(status);
        lostFoundRepository.save(item);
    }

    private void validateRequest(LostFoundRequest request) {
        String type = request.getType().trim();
        if (!"lost".equals(type) && !"found".equals(type)) {
            throw new BusinessException("类型只能是寻物或招领");
        }
        if (request.getTitle().trim().length() > 100) {
            throw new BusinessException("标题不能超过 100 字");
        }
        if (request.getLocation().trim().length() > 120) {
            throw new BusinessException("地点不能超过 120 字");
        }
        if (request.getContact().trim().length() > 100) {
            throw new BusinessException("联系方式不能超过 100 字");
        }
    }

    private LostFoundResponse convertToResponse(LostFound item) {
        LostFoundResponse response = new LostFoundResponse();
        response.setLostFoundId(item.getLostFoundId());
        response.setUser(convertToUserResponse(item.getUser()));
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
