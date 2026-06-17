package com.example.campus.service;

import com.example.campus.dto.request.ChatMessageRequest;
import com.example.campus.dto.response.ChatConversationResponse;
import com.example.campus.dto.response.ChatMessageResponse;
import com.example.campus.dto.response.UserResponse;
import com.example.campus.entity.Carpool;
import com.example.campus.entity.ChatMessage;
import com.example.campus.entity.LostFound;
import com.example.campus.entity.PartTime;
import com.example.campus.entity.Secondhand;
import com.example.campus.entity.User;
import com.example.campus.exception.BusinessException;
import com.example.campus.exception.ResourceNotFoundException;
import com.example.campus.repository.ChatMessageRepository;
import com.example.campus.repository.CarpoolRepository;
import com.example.campus.repository.LostFoundRepository;
import com.example.campus.repository.PartTimeRepository;
import com.example.campus.repository.SecondhandRepository;
import com.example.campus.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SecondhandRepository secondhandRepository;
    private final CarpoolRepository carpoolRepository;
    private final PartTimeRepository partTimeRepository;
    private final LostFoundRepository lostFoundRepository;
    private final NotificationService notificationService;

    public ChatService(
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository,
            SecondhandRepository secondhandRepository,
            CarpoolRepository carpoolRepository,
            PartTimeRepository partTimeRepository,
            LostFoundRepository lostFoundRepository,
            NotificationService notificationService) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.secondhandRepository = secondhandRepository;
        this.carpoolRepository = carpoolRepository;
        this.partTimeRepository = partTimeRepository;
        this.lostFoundRepository = lostFoundRepository;
        this.notificationService = notificationService;
    }

    public List<ChatConversationResponse> getConversations(Long userId) {
        Map<String, ChatConversationResponse> conversations = new LinkedHashMap<>();
        for (ChatMessage message : chatMessageRepository.findRelatedMessages(userId)) {
            User peer = message.getSender().getUserId().equals(userId) ? message.getReceiver() : message.getSender();
            String key = peer.getUserId() + "|" + message.getTargetType() + "|" + message.getTargetId();
            conversations.computeIfAbsent(key, ignored -> toConversation(userId, peer, message));
        }
        return conversations.values().stream()
                .sorted(Comparator.comparing(ChatConversationResponse::getLastTime).reversed())
                .toList();
    }

    @Transactional
    public List<ChatMessageResponse> getConversation(Long userId, Long peerId, String targetType, Long targetId) {
        List<ChatMessage> messages = chatMessageRepository.findConversation(userId, peerId, targetType, targetId);
        messages.stream()
                .filter(message -> message.getReceiver().getUserId().equals(userId))
                .filter(message -> !message.getReadFlag())
                .forEach(message -> message.setReadFlag(true));
        chatMessageRepository.saveAll(messages);
        return messages.stream()
                .map(message -> toMessageResponse(userId, message))
                .toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long senderId, ChatMessageRequest request) {
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isBlank()) {
            throw new BusinessException("消息内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException("消息内容不能超过 500 字");
        }
        if (senderId.equals(request.getReceiverId())) {
            throw new BusinessException("不能给自己发送消息");
        }
        validateTarget(request.getTargetType(), request.getTargetId());

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("发送人不存在"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("接收人不存在"));

        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setTargetType(request.getTargetType().trim());
        message.setTargetId(request.getTargetId());
        message.setContent(content);
        message.setReadFlag(false);

        ChatMessage saved = chatMessageRepository.save(message);
        notificationService.create(
                receiver.getUserId(),
                "收到新的交流消息",
                sender.getUsername() + " 给你发送了一条消息",
                "chat",
                saved.getTargetType(),
                saved.getTargetId());
        return toMessageResponse(senderId, saved);
    }

    public long getUnreadCount(Long userId) {
        return chatMessageRepository.countByReceiver_UserIdAndReadFlagFalse(userId);
    }

    private void validateTarget(String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            throw new BusinessException("关联内容不能为空");
        }
        switch (targetType) {
            case "secondhand" -> secondhandRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("闲置物品不存在"));
            case "carpool" -> carpoolRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("拼车信息不存在"));
            case "part-time" -> partTimeRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("兼职信息不存在"));
            case "lost-found" -> lostFoundRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("失物招领信息不存在"));
            default -> throw new BusinessException("不支持的交流类型");
        }
    }

    private ChatConversationResponse toConversation(Long userId, User peer, ChatMessage latest) {
        ChatConversationResponse response = new ChatConversationResponse();
        response.setPeer(toUserResponse(peer));
        response.setTargetType(latest.getTargetType());
        response.setTargetId(latest.getTargetId());
        response.setTargetTitle(resolveTargetTitle(latest.getTargetType(), latest.getTargetId()));
        response.setLastContent(latest.getContent());
        response.setLastTime(latest.getCreateTime());
        response.setUnreadCount(chatMessageRepository.findConversation(
                        userId,
                        peer.getUserId(),
                        latest.getTargetType(),
                        latest.getTargetId()).stream()
                .filter(message -> message.getReceiver().getUserId().equals(userId))
                .filter(message -> !message.getReadFlag())
                .count());
        return response;
    }

    private String resolveTargetTitle(String targetType, Long targetId) {
        if ("secondhand".equals(targetType)) {
            return secondhandRepository.findById(targetId)
                    .map(Secondhand::getTitle)
                    .orElse("闲置物品");
        }
        if ("carpool".equals(targetType)) {
            return carpoolRepository.findById(targetId)
                    .map(item -> item.getDeparture() + " → " + item.getDestination())
                    .orElse("拼车服务");
        }
        if ("part-time".equals(targetType)) {
            return partTimeRepository.findById(targetId)
                    .map(PartTime::getTitle)
                    .orElse("兼职招聘");
        }
        if ("lost-found".equals(targetType)) {
            return lostFoundRepository.findById(targetId)
                    .map(LostFound::getTitle)
                    .orElse("失物招领");
        }
        return "交流内容";
    }

    private ChatMessageResponse toMessageResponse(Long currentUserId, ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setMessageId(message.getMessageId());
        response.setSender(toUserResponse(message.getSender()));
        response.setReceiver(toUserResponse(message.getReceiver()));
        response.setTargetType(message.getTargetType());
        response.setTargetId(message.getTargetId());
        response.setContent(message.getContent());
        response.setReadFlag(message.getReadFlag());
        response.setMine(message.getSender().getUserId().equals(currentUserId));
        response.setCreateTime(message.getCreateTime());
        return response;
    }

    private UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        return response;
    }
}
