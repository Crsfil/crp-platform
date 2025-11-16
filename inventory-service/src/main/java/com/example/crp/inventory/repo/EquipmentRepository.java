package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;


public interface EquipmentRepository extends JpaRepository<Equipment, Long> {}

