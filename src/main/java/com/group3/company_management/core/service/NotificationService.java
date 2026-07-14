package com.group3.company_management.core.service;

import com.group3.company_management.core.entity.Notification;
import java.util.List;

public interface NotificationService {
    List<Notification> getNotificationsByUsername(String username);
    long getUnreadCount(String username);
    void markAsRead(Long notificationId);
    void createNotification(Long accountId, String title, String message); // Hàm để các tính năng khác gọi tự động bắn noti
    
    List<Notification> getNotificationsByCustomerId(Long customerId);
    long getUnreadCountByCustomerId(Long customerId);
    void createCustomerNotification(Long customerId, String title, String message);
}