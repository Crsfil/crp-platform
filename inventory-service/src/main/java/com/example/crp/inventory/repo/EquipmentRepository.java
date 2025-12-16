package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.Equipment;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Equipment e set e.status = :newStatus where e.id = :id and upper(e.status) = upper(:expectedStatus)")
    int updateStatusIfEquals(@Param("id") Long id,
                             @Param("expectedStatus") String expectedStatus,
                             @Param("newStatus") String newStatus);
}

