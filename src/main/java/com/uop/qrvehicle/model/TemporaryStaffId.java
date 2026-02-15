package com.uop.qrvehicle.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for TemporaryStaff entity (SalDt, EmpNo, EmpCat1Nm)
 * Matches the actual temporarystaff table PRIMARY KEY (`SalDt`,`EmpNo`,`EmpCat1Nm`)
 */
public class TemporaryStaffId implements Serializable {

    private String salaryDate;
    private String empNo;
    private String category;

    public TemporaryStaffId() {
    }

    public TemporaryStaffId(String salaryDate, String empNo, String category) {
        this.salaryDate = salaryDate;
        this.empNo = empNo;
        this.category = category;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemporaryStaffId that = (TemporaryStaffId) o;
        return Objects.equals(salaryDate, that.salaryDate)
                && Objects.equals(empNo, that.empNo)
                && Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(salaryDate, empNo, category);
    }
}
