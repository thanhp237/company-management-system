package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.Notification;
import com.group3.company_management.core.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationRestController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.group3.company_management.core.repository.CustomerRepository customerRepository;

    // Lấy nhanh số lượng chưa đọc + 5 thông báo mới nhất cho chiếc chuông trên Navbar
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getNotificationSummary(Principal principal) {
        String username = principal.getName();
        List<Notification> allNoti = notificationService.getNotificationsByUsername(username);
        long unreadCount = notificationService.getUnreadCount(username);
        
        // Chỉ lấy tối đa 10 cái mới nhất để hiển thị nhanh ở dropdown chuông
        List<Notification> top10 = allNoti.stream().limit(10).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("unreadCount", unreadCount);
        response.put("notifications", top10);
        return ResponseEntity.ok(response);
    }

    // Customer summary endpoint
    @GetMapping("/customer/summary")
    public ResponseEntity<Map<String, Object>> getCustomerNotificationSummary(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String email = principal.getName();
        return customerRepository.findByEmailAndNotDeleted(email)
                .map(customer -> {
                    List<Notification> allNoti = notificationService.getNotificationsByCustomerId(customer.getId());
                    long unreadCount = notificationService.getUnreadCountByCustomerId(customer.getId());
                    List<Notification> top10 = allNoti.stream().limit(10).toList();

                    Map<String, Object> response = new HashMap<>();
                    response.put("unreadCount", unreadCount);
                    response.put("notifications", top10);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.ok(Map.of("unreadCount", 0, "notifications", List.of())));
    }

    // Đánh dấu thông báo là đã đọc khi click vào
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable("id") Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}