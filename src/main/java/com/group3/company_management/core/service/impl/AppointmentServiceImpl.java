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

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByEmployee(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        return appointmentRepository.findByEmployeeIdOrderByAppointmentTimeAsc(user.getId())
                .stream()
                .map(AppointmentResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public void createAppointment(AppointmentRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Appointment appointment = new Appointment();
        appointment.setTitle(request.getTitle());
        appointment.setDescription(request.getDescription());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setStatus("PENDING"); // Mặc định khi tạo mới là chờ diễn ra
        appointment.setReminderSent(false); // Chưa gửi nhắc nhở
        appointment.setEmployee(user);
        appointment.setCustomer(customer);

        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        
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
        }

        return upcomingAppointments.stream()
                .map(AppointmentResponse::fromEntity)
                .toList();
    }
}