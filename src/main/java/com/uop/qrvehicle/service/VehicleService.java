package com.uop.qrvehicle.service;

import com.uop.qrvehicle.model.Vehicle;
import com.uop.qrvehicle.model.VehicleId;
import com.uop.qrvehicle.model.VehicleType;
import com.uop.qrvehicle.repository.VehicleRepository;
import com.uop.qrvehicle.repository.VehicleTypeRepository;
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
    private final VehicleTypeRepository vehicleTypeRepository;

    public VehicleService(VehicleRepository vehicleRepository,
                          VehicleTypeRepository vehicleTypeRepository) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
    }

    /**
     * Get all active vehicle types
     */
    public List<VehicleType> getActiveVehicleTypes() {
        return vehicleTypeRepository.findByIsActiveTrueOrderByTypeName();
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
     * Add a new vehicle (legacy method - without new fields)
     */
    public Vehicle addVehicle(String empId, String vehicleNo, String owner, 
                              String type, String createdBy) {
        return addVehicle(empId, vehicleNo, owner, type, null, null, null, createdBy);
    }

    /**
     * Add a new vehicle with all fields (including vehicle type, mobile, email)
     */
    public Vehicle addVehicle(String empId, String vehicleNo, String owner, 
                              String type, Integer vehicleTypeId, 
                              String mobile, String email, String createdBy) {
        // Check if vehicle already exists for this employee
        if (vehicleRepository.existsByEmpIdAndVehicleNo(empId, vehicleNo)) {
            throw new IllegalArgumentException("Vehicle already registered for this employee");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setEmpId(empId);
        vehicle.setVehicleNo(vehicleNo.toUpperCase());
        vehicle.setOwner(owner);
        vehicle.setType(type);
        vehicle.setMobile(mobile);
        vehicle.setEmail(email);
        vehicle.setCreatedBy(createdBy);
        vehicle.setApprovalStatus("Pending");
        vehicle.setCreateDate(LocalDateTime.now());

        // Set vehicle type if provided
        if (vehicleTypeId != null) {
            vehicleTypeRepository.findById(vehicleTypeId)
                .ifPresent(vehicle::setVehicleType);
        }

        return vehicleRepository.save(vehicle);
    }

    /**
     * Update an existing vehicle
     */
    public Vehicle updateVehicle(String empId, String oldVehicleNo, String newVehicleNo,
                                 String owner, String approvalStatus, String updatedBy) {
        Vehicle vehicle = vehicleRepository.findByEmpIdAndVehicleNo(empId, oldVehicleNo)
            .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        // If vehicle number changed, need to delete old and create new
        if (!oldVehicleNo.equals(newVehicleNo)) {
            vehicleRepository.delete(vehicle);
            
            Vehicle newVehicle = new Vehicle();
            newVehicle.setEmpId(empId);
            newVehicle.setVehicleNo(newVehicleNo.toUpperCase());
            newVehicle.setOwner(owner);
            newVehicle.setType(vehicle.getType());
            newVehicle.setApprovalStatus(approvalStatus != null ? approvalStatus : vehicle.getApprovalStatus());
            newVehicle.setCreatedBy(updatedBy);
            newVehicle.setCreateDate(LocalDateTime.now());
            return vehicleRepository.save(newVehicle);
        }
        
        vehicle.setOwner(owner);
        vehicle.setApprovalStatus(approvalStatus != null ? approvalStatus : vehicle.getApprovalStatus());
        vehicle.setCreatedBy(updatedBy);

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
     * Approve a vehicle by empId and vehicleNo
     */
    public Vehicle approveVehicle(String empId, String vehicleNo) {
        Vehicle vehicle = vehicleRepository.findByEmpIdAndVehicleNo(empId, vehicleNo)
            .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        vehicle.setApprovalStatus("Approved");
        vehicle.setApprovalDate(LocalDateTime.now());
        return vehicleRepository.save(vehicle);
    }

    /**
     * Reject a vehicle by empId and vehicleNo
     */
    public Vehicle rejectVehicle(String empId, String vehicleNo) {
        Vehicle vehicle = vehicleRepository.findByEmpIdAndVehicleNo(empId, vehicleNo)
            .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        vehicle.setApprovalStatus("Rejected");
        vehicle.setApprovalDate(LocalDateTime.now());
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
