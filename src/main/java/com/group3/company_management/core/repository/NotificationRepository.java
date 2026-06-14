package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Lấy toàn bộ danh sách thông báo của một tài khoản, xếp cái mới nhất lên đầu
    List<Notification> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    
    // Đếm số lượng thông báo chưa đọc để hiển thị số đỏ trên icon chuông
    long countByAccountIdAndIsReadFalse(Long accountId);
}