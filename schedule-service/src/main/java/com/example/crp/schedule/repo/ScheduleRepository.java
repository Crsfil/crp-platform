package com.example.crp.schedule.repo;

import com.example.crp.schedule.domain.ScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<ScheduleItem, Long> {
    List<ScheduleItem> findByDueDateAndStatus(LocalDate dueDate, String status);
    List<ScheduleItem> findByAgreementIdOrderByDueDate(Long agreementId);
}

