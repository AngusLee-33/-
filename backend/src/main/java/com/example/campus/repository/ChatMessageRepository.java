package com.example.campus.repository;

import com.example.campus.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            select m from ChatMessage m
            where m.targetType = :targetType
              and m.targetId = :targetId
              and ((m.sender.userId = :userId and m.receiver.userId = :peerId)
                or (m.sender.userId = :peerId and m.receiver.userId = :userId))
            order by m.createTime asc
            """)
    List<ChatMessage> findConversation(
            @Param("userId") Long userId,
            @Param("peerId") Long peerId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId);

    @Query("""
            select m from ChatMessage m
            where m.sender.userId = :userId or m.receiver.userId = :userId
            order by m.createTime desc
            """)
    List<ChatMessage> findRelatedMessages(@Param("userId") Long userId);

    long countByReceiver_UserIdAndReadFlagFalse(Long userId);

    void deleteBySender_UserIdOrReceiver_UserId(Long senderId, Long receiverId);

    void deleteByTargetTypeAndTargetId(String targetType, Long targetId);
}
