package com.group3.company_management.core.dto;

import com.group3.company_management.core.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private Long id;
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private long unreadCount;
    private String eventType;

    public static NotificationMessage created(Notification notification, long unreadCount) {
        return NotificationMessage.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .unreadCount(unreadCount)
                .eventType("CREATED")
                .build();
    }

    public static NotificationMessage countChanged(long unreadCount) {
        return NotificationMessage.builder()
                .unreadCount(unreadCount)
                .eventType("COUNT_CHANGED")
                .build();
    }
}
