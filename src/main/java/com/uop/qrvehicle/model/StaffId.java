package com.uop.qrvehicle.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for Staff entity (SalDt, EmpNo)
 */
public class StaffId implements Serializable {

    private String salaryDate;
    private String empNo;

    public StaffId() {
    }

    public StaffId(String salaryDate, String empNo) {
        this.salaryDate = salaryDate;
        this.empNo = empNo;
    }

    public String getSalaryDate() {
        return salaryDate;
    }

    public void setSalaryDate(String salaryDate) {
        this.salaryDate = salaryDate;
    }

    public String getEmpNo() {
        return empNo;
    }

    public void setEmpNo(String empNo) {
        this.empNo = empNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaffId staffId = (StaffId) o;
        return Objects.equals(salaryDate, staffId.salaryDate) && Objects.equals(empNo, staffId.empNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(salaryDate, empNo);
    }
}
