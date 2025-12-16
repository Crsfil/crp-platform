package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.Stocktake;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StocktakeRepository extends JpaRepository<Stocktake, Long> {
    List<Stocktake> findTop100ByLocationIdOrderByCreatedAtDesc(Long locationId);
}

