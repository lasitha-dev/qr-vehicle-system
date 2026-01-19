package com.uop.qrvehicle.service;

import com.uop.qrvehicle.model.Vehicle;
import com.uop.qrvehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Vehicle Service - Business logic for vehicle management
 */
@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    /**
     * Get all vehicles for a specific employee/student
     */
    public List<Vehicle> getVehiclesByEmpId(String empId) {
        return vehicleRepository.findByEmpIdOrderByCreateDateDesc(empId);
    }

    /**
     * Get a specific vehicle by employee ID and vehicle number
     */
    public Optional<Vehicle> getVehicle(String empId, String vehicleNo) {
        return vehicleRepository.findByEmpIdAndVehicleNo(empId, vehicleNo);
    }

    /**
     * Add a new vehicle
     */
    public Vehicle addVehicle(String empId, String vehicleNo, String owner, 
                              String type, String createdBy) {
        // Check if vehicle already exists for this employee
        if (vehicleRepository.existsByEmpIdAndVehicleNo(empId, vehicleNo)) {
            throw new IllegalArgumentException("Vehicle already registered for this employee");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setEmpId(empId);
        vehicle.setVehicleNo(vehicleNo.toUpperCase());
        vehicle.setOwner(owner);
        vehicle.setType(type);
        vehicle.setCreatedBy(createdBy);
        vehicle.setApprovalStatus("Pending");
        vehicle.setCreateDate(LocalDateTime.now());

        return vehicleRepository.save(vehicle);
    }

    /**
     * Update an existing vehicle
     */
    public Vehicle updateVehicle(String empId, String oldVehicleNo, String newVehicleNo,
                                 String owner, String approvalStatus, String updatedBy) {
        Vehicle vehicle = vehicleRepository.findByEmpIdAndVehicleNo(empId, oldVehicleNo)
            .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        vehicle.setVehicleNo(newVehicleNo.toUpperCase());
        vehicle.setOwner(owner);
        vehicle.setApprovalStatus(approvalStatus);
        vehicle.setCreatedBy(updatedBy);
        vehicle.setCreateDate(LocalDateTime.now());

        return vehicleRepository.save(vehicle);
    }

    /**
     * Delete a vehicle
     */
    public void deleteVehicle(String empId, String vehicleNo) {
        Vehicle vehicle = vehicleRepository.findByEmpIdAndVehicleNo(empId, vehicleNo)
            .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        vehicleRepository.delete(vehicle);
    }

    /**
     * Approve a vehicle
     */
    public Vehicle approveVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
            .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        vehicle.setApprovalStatus("Approved");
        return vehicleRepository.save(vehicle);
    }

    /**
     * Reject a vehicle
     */
    public Vehicle rejectVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
            .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        vehicle.setApprovalStatus("Rejected");
        return vehicleRepository.save(vehicle);
    }

    /**
     * Search vehicles by vehicle number
     */
    public List<Vehicle> searchByVehicleNo(String vehicleNo) {
        return vehicleRepository.findByVehicleNoContainingIgnoreCase(vehicleNo);
    }

    /**
     * Get pending vehicles count
     */
    public long getPendingCount() {
        return vehicleRepository.countPendingVehicles();
    }

    /**
     * Get all pending vehicles
     */
    public List<Vehicle> getPendingVehicles() {
        return vehicleRepository.findByApprovalStatus("Pending");
    }
}
