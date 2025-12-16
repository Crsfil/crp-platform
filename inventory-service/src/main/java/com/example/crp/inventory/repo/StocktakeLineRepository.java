package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.StocktakeLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StocktakeLineRepository extends JpaRepository<StocktakeLine, Long> {
    List<StocktakeLine> findByStocktakeIdOrderByIdAsc(Long stocktakeId);
    Optional<StocktakeLine> findByStocktakeIdAndEquipmentId(Long stocktakeId, Long equipmentId);
}

