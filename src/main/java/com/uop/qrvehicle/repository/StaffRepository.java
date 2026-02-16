package com.uop.qrvehicle.repository;

import com.uop.qrvehicle.model.Staff;
import com.uop.qrvehicle.model.StaffId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Staff Repository - Data access for permanent Staff entity
 */
@Repository
public interface StaffRepository extends JpaRepository<Staff, StaffId> {

    Optional<Staff> findByEmpNo(String empNo);

    @Query(value = """
        SELECT s.* FROM slipspaymentsdetailall s
        INNER JOIN (
            SELECT EmpNo, MAX(SalDt) AS LatestSalDt
            FROM slipspaymentsdetailall
            GROUP BY EmpNo
        ) latest ON s.EmpNo = latest.EmpNo AND s.SalDt = latest.LatestSalDt
        ORDER BY s.EmpNo ASC
        """, nativeQuery = true)
    List<Staff> findAllLatestRecords();

    @Query(value = """
        SELECT s.* FROM slipspaymentsdetailall s
        INNER JOIN (
            SELECT EmpNo, MAX(SalDt) AS LatestSalDt
            FROM slipspaymentsdetailall
            WHERE EmpNo = :empNo
            GROUP BY EmpNo
        ) latest ON s.EmpNo = latest.EmpNo AND s.SalDt = latest.LatestSalDt
        """, nativeQuery = true)
    Optional<Staff> findLatestByEmpNo(String empNo);

    @Query(value = """
        SELECT s.* FROM slipspaymentsdetailall s
        INNER JOIN (
            SELECT EmpNo, MAX(SalDt) AS LatestSalDt
            FROM slipspaymentsdetailall
            GROUP BY EmpNo
        ) latest ON s.EmpNo = latest.EmpNo AND s.SalDt = latest.LatestSalDt
        WHERE s.EmpNm LIKE CONCAT('%', :name, '%')
        ORDER BY s.EmpNo ASC
        """, nativeQuery = true)
    List<Staff> searchByName(String name);

    @Query(value = """
        SELECT s.* FROM slipspaymentsdetailall s
        INNER JOIN (
            SELECT EmpNo, MAX(SalDt) AS LatestSalDt
            FROM slipspaymentsdetailall
            GROUP BY EmpNo
        ) latest ON s.EmpNo = latest.EmpNo AND s.SalDt = latest.LatestSalDt
        WHERE s.EmpTypCd = 'Non Academic'
        ORDER BY s.EmpNo ASC
        """, nativeQuery = true)
    List<Staff> findAllLatestNonAcademicRecords();

    @Query(value = """
        SELECT s.* FROM slipspaymentsdetailall s
        INNER JOIN (
            SELECT EmpNo, MAX(SalDt) AS LatestSalDt
            FROM slipspaymentsdetailall
            GROUP BY EmpNo
        ) latest ON s.EmpNo = latest.EmpNo AND s.SalDt = latest.LatestSalDt
        WHERE s.EmpTypCd <> 'Non Academic'
        ORDER BY s.EmpNo ASC
        """, nativeQuery = true)
    List<Staff> findAllLatestAcademicRecords();
}
