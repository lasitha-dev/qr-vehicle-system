package com.uop.qrvehicle.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for Vehicle entity (EmpID, Vehino)
 */
public class VehicleId implements Serializable {

    private String empId;
    private String vehicleNo;

    public VehicleId() {
    }

    public VehicleId(String empId, String vehicleNo) {
        this.empId = empId;
        this.vehicleNo = vehicleNo;
    }

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public String getVehicleNo() {
        return vehicleNo;
    }

    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleId vehicleId = (VehicleId) o;
        return Objects.equals(empId, vehicleId.empId) && Objects.equals(vehicleNo, vehicleId.vehicleNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(empId, vehicleNo);
    }
}
