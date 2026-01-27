package com.uop.qrvehicle.repository;

import com.uop.qrvehicle.model.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for VehicleType entity
 */
@Repository
public interface VehicleTypeRepository extends JpaRepository<VehicleType, Integer> {

    /**
     * Find all active vehicle types ordered by name
     */
    List<VehicleType> findByIsActiveTrueOrderByTypeName();

    /**
     * Find vehicle type by name
     */
    VehicleType findByTypeName(String typeName);
}
