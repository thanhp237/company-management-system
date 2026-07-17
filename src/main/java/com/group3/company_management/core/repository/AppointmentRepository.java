package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // 1. Lấy danh sách lịch hẹn của riêng từng nhân viên Sales (Sắp xếp theo thời gian tăng dần)
    List<Appointment> findByEmployeeIdOrderByAppointmentTimeAsc(Long employeeId);

    List<Appointment> findByEmployeeIdInOrderByAppointmentTimeAsc(List<Long> employeeIds);

    long countByEmployeeUsername(String username);

    long countByEmployeeUsernameAndStatus(String username, String status);

    // 2. Tìm các lịch hẹn sắp diễn ra trong vòng 1 ngày tới mà CHƯA gửi thông báo nhắc nhở
    @Query("SELECT a FROM Appointment a WHERE a.status = 'PENDING' " +
           "AND a.reminderSent = false " +
           "AND a.appointmentTime >= :now " +
           "AND a.appointmentTime <= :reminderTime")
    List<Appointment> findAppointmentsNeedReminder(
            @Param("now") LocalDateTime now, 
            @Param("reminderTime") LocalDateTime reminderTime
    );
}
