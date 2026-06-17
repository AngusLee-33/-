package com.example.campus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatMessageRequest {

    @NotNull(message = "接收人不能为空")
    private Long receiverId;

    @NotBlank(message = "关联类型不能为空")
    private String targetType;

    @NotNull(message = "关联内容不能为空")
    private Long targetId;

    @NotBlank(message = "消息内容不能为空")
    private String content;
}
