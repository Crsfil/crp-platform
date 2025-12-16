package com.example.crp.inventory.repo;

import com.example.crp.inventory.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByCode(String code);

    List<Location> findByParentIdOrderByCodeAsc(Long parentId);

    List<Location> findByPathStartingWithOrderByPathAsc(String pathPrefix);
}
