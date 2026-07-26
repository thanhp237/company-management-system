package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.Notification;
import com.group3.company_management.core.repository.CustomerRepository;
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
    private CustomerRepository customerRepository;

    // Lấy nhanh số lượng chưa đọc + 10 thông báo mới nhất cho chiếc chuông trên Navbar
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getNotificationSummary(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        String username = principal.getName();
        List<Notification> allNoti = notificationService.getNotificationsByUsername(username);
        long unreadCount = notificationService.getUnreadCount(username);

        Map<String, Object> response = new HashMap<>();
        response.put("unreadCount", unreadCount);
        response.put("notifications", allNoti.stream().limit(10).toList());
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

                    Map<String, Object> response = new HashMap<>();
                    response.put("unreadCount", unreadCount);
                    response.put("notifications", allNoti.stream().limit(10).toList());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.ok(Map.of("unreadCount", 0, "notifications", List.of())));
    }

    // Đánh dấu thông báo hệ thống là đã đọc, chỉ cho đúng account nhận thông báo
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable("id") Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        boolean updated = notificationService.markAsReadForUsername(id, principal.getName());
        return updated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // Đánh dấu thông báo khách hàng là đã đọc, chỉ cho đúng customer nhận thông báo
    @PostMapping("/customer/{id}/read")
    public ResponseEntity<Void> markCustomerAsRead(@PathVariable("id") Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        return customerRepository.findByEmailAndNotDeleted(principal.getName())
                .map(customer -> notificationService.markAsReadForCustomer(id, customer.getId())
                        ? ResponseEntity.ok().<Void>build()
                        : ResponseEntity.notFound().<Void>build())
                .orElse(ResponseEntity.status(403).build());
    }
}
