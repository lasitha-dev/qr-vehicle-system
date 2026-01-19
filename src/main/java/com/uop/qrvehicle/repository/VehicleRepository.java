package com.uop.qrvehicle.repository;

import com.uop.qrvehicle.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Vehicle Repository - Data access for Vehicle entity
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByEmpIdOrderByCreateDateDesc(String empId);

    Optional<Vehicle> findByEmpIdAndVehicleNo(String empId, String vehicleNo);

    List<Vehicle> findByVehicleNoContainingIgnoreCase(String vehicleNo);

    List<Vehicle> findByApprovalStatus(String status);

    @Query("SELECT v FROM Vehicle v WHERE v.type = :type ORDER BY v.createDate DESC")
    List<Vehicle> findByType(String type);

    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.approvalStatus = 'Pending'")
    long countPendingVehicles();

    boolean existsByEmpIdAndVehicleNo(String empId, String vehicleNo);
}
