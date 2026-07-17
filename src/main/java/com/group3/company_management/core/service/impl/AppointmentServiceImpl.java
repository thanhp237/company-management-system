package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.AppointmentRequest;
import com.group3.company_management.core.dto.AppointmentResponse;
import com.group3.company_management.core.entity.Appointment;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.AppointmentRepository;
import com.group3.company_management.core.repository.CustomerRepository; // Giả định bạn có repo này
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.service.AppointmentService;
import com.group3.company_management.core.service.NotificationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository; // Cần dùng để liên kết khách hàng

    // THÊM DÒNG NÀY ĐỂ KẾT NỐI VỚI HỆ THỐNG THÔNG BÁO
    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByEmployee(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        
        if (user.isManager() || "SALES_MANAGER".equalsIgnoreCase(user.getRole().getRoleCode())) {
            Long deptId = user.getDepartmentId();
            if (deptId != null) {
                List<User> deptUsers = userRepository.findByDepartmentIdAndIsDeletedFalseOrderByFullNameAsc(deptId);
                List<Long> deptUserIds = new java.util.ArrayList<>();
                for (User u : deptUsers) {
                    if (!"MANAGER".equalsIgnoreCase(u.getRole().getRoleCode()) && 
                        !"SALES_MANAGER".equalsIgnoreCase(u.getRole().getRoleCode()) &&
                        !"ADMIN".equalsIgnoreCase(u.getRole().getRoleCode())) {
                        deptUserIds.add(u.getId());
                    }
                }
                if (deptUserIds.isEmpty()) {
                    return List.of();
                }
                return appointmentRepository.findByEmployeeIdInOrderByAppointmentTimeAsc(deptUserIds)
                        .stream()
                        .map(AppointmentResponse::fromEntity)
                        .toList();
            }
            return List.of();
        }

        return appointmentRepository.findByEmployeeIdOrderByAppointmentTimeAsc(user.getId())
                .stream()
                .map(AppointmentResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public void createAppointment(AppointmentRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        Appointment appointment = new Appointment();
        appointment.setTitle(request.getTitle());
        appointment.setDescription(request.getDescription());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setStatus("PENDING"); // Mặc định khi tạo mới là chờ diễn ra
        appointment.setReminderSent(false); // Chưa gửi nhắc nhở
        appointment.setEmployee(user);
        appointment.setCustomer(customer);

        appointmentRepository.save(appointment);

        // THÊM ĐOẠN NÀY: Kích hoạt chuông thông báo ngay sau khi tạo lịch thành công
        notificationService.createNotification(
                user.getId(), 
                "Tạo lịch hẹn thành công", 
                "Bạn đã lên lịch: '" + request.getTitle() + "' với KH " + customer.getFullName() + " vào lúc " + request.getAppointmentTime()
        );
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));
        
        appointment.setStatus(status);
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public List<AppointmentResponse> checkAndGetUpcomingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1); // Thời điểm hiện tại + đúng 1 ngày (24 giờ tới)

        // Tìm các lịch hẹn PENDING, chưa gửi thông báo, và nằm trong khoảng từ NOW đến TOMORROW
        List<Appointment> upcomingAppointments = appointmentRepository.findAppointmentsNeedReminder(now, tomorrow);

        // Đánh dấu sang `true` để hệ thống biết các lịch này đã được quét trúng, tránh thông báo lặp lại phiền phức
        for (Appointment app : upcomingAppointments) {
            app.setReminderSent(true);
            appointmentRepository.save(app);

            // -------- BẠN CHÈN THÊM ĐOẠN NÀY VÀO ĐÂY --------
            // Bắn cảnh báo lịch hẹn sắp diễn ra cho nhân viên phụ trách
            if (notificationService != null && app.getEmployee() != null) {
                notificationService.createNotification(
                        app.getEmployee().getId(),
                        "!!! Lịch hẹn sắp diễn ra",
                        "Bạn có lịch hẹn: '" + app.getTitle() + "' với khách hàng " + app.getCustomer().getFullName() + " trong vòng chưa đầy 24h tới."
                );
            }
            // ------------------------------------------------
        }

        return upcomingAppointments.stream()
                .map(AppointmentResponse::fromEntity)
                .toList();
    }
}
