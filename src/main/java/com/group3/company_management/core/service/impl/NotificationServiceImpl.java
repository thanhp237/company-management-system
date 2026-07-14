package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.entity.Notification;
import com.group3.company_management.core.repository.NotificationRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate; // Dùng tạm để lấy nhanh account_id từ username đăng nhập
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.group3.company_management.core.entity.User; // <-- ĐẢM BẢO CÓ DÒNG NÀY

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

   // Sửa lại hàm helper để tìm chuẩn xác theo thực thể User đang đăng nhập
    private Long getAccountIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElse(null);
    }

    @Override
    public List<Notification> getNotificationsByUsername(String username) {
        Long accountId = getAccountIdByUsername(username);
        if (accountId == null) return List.of();
        return notificationRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    @Override
    public long getUnreadCount(String username) {
        Long accountId = getAccountIdByUsername(username);
        if (accountId == null) return 0;
        return notificationRepository.countByAccountIdAndIsReadFalse(accountId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(noti -> {
            noti.setIsRead(true);
            notificationRepository.save(noti);
        });
    }

    @Override
    @Transactional
    public void createNotification(Long accountId, String title, String message) {
        Notification noti = Notification.builder()
                .accountId(accountId)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(noti);
    }

    @Override
    public List<Notification> getNotificationsByCustomerId(Long customerId) {
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Override
    public long getUnreadCountByCustomerId(Long customerId) {
        return notificationRepository.countByCustomerIdAndIsReadFalse(customerId);
    }

    @Override
    @Transactional
    public void createCustomerNotification(Long customerId, String title, String message) {
        Notification noti = Notification.builder()
                .customerId(customerId)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(noti);
    }
}