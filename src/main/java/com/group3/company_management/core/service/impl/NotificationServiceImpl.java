package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.NotificationMessage;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Notification;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.CustomerRepository;
import com.group3.company_management.core.repository.NotificationRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final String USER_NOTIFICATION_QUEUE = "/queue/notifications";

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private Optional<User> findActiveUserByUsername(String username) {
        return userRepository.findByUsernameAndNotDeleted(username)
                .or(() -> userRepository.findByUsername(username));
    }

    @Override
    public List<Notification> getNotificationsByUsername(String username) {
        return findActiveUserByUsername(username)
                .map(user -> notificationRepository.findByAccountIdOrderByCreatedAtDesc(user.getId()))
                .orElse(List.of());
    }

    @Override
    public long getUnreadCount(String username) {
        return findActiveUserByUsername(username)
                .map(user -> notificationRepository.countByAccountIdAndIsReadFalse(user.getId()))
                .orElse(0L);
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
    public boolean markAsReadForUsername(Long notificationId, String username) {
        Optional<User> userOpt = findActiveUserByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        Optional<Notification> notificationOpt = notificationRepository.findByIdAndAccountId(notificationId, user.getId());
        if (notificationOpt.isEmpty()) {
            return false;
        }

        Notification notification = notificationOpt.get();
        notification.setIsRead(true);
        notificationRepository.save(notification);
        publishAccountCount(user);
        return true;
    }

    @Override
    @Transactional
    public boolean markAsReadForCustomer(Long notificationId, Long customerId) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isEmpty()) {
            return false;
        }

        Optional<Notification> notificationOpt = notificationRepository.findByIdAndCustomerId(notificationId, customerId);
        if (notificationOpt.isEmpty()) {
            return false;
        }

        Notification notification = notificationOpt.get();
        notification.setIsRead(true);
        notificationRepository.save(notification);
        publishCustomerCount(customerOpt.get());
        return true;
    }

    @Override
    @Transactional
    public void createNotification(Long accountId, String title, String message) {
        Notification notification = Notification.builder()
                .accountId(accountId)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        Notification saved = notificationRepository.save(notification);
        userRepository.findById(accountId).ifPresent(user -> publishAccountNotification(user, saved));
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
        Notification notification = Notification.builder()
                .customerId(customerId)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        Notification saved = notificationRepository.save(notification);
        customerRepository.findById(customerId).ifPresent(customer -> publishCustomerNotification(customer, saved));
    }

    private void publishAccountNotification(User user, Notification notification) {
        long unreadCount = notificationRepository.countByAccountIdAndIsReadFalse(user.getId());
        messagingTemplate.convertAndSendToUser(user.getUsername(), USER_NOTIFICATION_QUEUE, NotificationMessage.created(notification, unreadCount));
    }

    private void publishCustomerNotification(Customer customer, Notification notification) {
        long unreadCount = notificationRepository.countByCustomerIdAndIsReadFalse(customer.getId());
        messagingTemplate.convertAndSendToUser(customer.getEmail(), USER_NOTIFICATION_QUEUE, NotificationMessage.created(notification, unreadCount));
    }

    private void publishAccountCount(User user) {
        long unreadCount = notificationRepository.countByAccountIdAndIsReadFalse(user.getId());
        messagingTemplate.convertAndSendToUser(user.getUsername(), USER_NOTIFICATION_QUEUE, NotificationMessage.countChanged(unreadCount));
    }

    private void publishCustomerCount(Customer customer) {
        long unreadCount = notificationRepository.countByCustomerIdAndIsReadFalse(customer.getId());
        messagingTemplate.convertAndSendToUser(customer.getEmail(), USER_NOTIFICATION_QUEUE, NotificationMessage.countChanged(unreadCount));
    }
}
