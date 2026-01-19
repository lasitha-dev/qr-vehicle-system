package com.uop.qrvehicle.repository;

import com.uop.qrvehicle.model.TemporaryStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TemporaryStaff Repository - Data access for temporary, casual, contract, institute staff
 */
@Repository
public interface TemporaryStaffRepository extends JpaRepository<TemporaryStaff, String> {

    Optional<TemporaryStaff> findByEmpNo(String empNo);

    List<TemporaryStaff> findByCategoryContainingIgnoreCase(String category);

    @Query("SELECT t FROM TemporaryStaff t WHERE LOWER(t.category) LIKE LOWER(CONCAT(:category, '%')) ORDER BY t.empNo")
    List<TemporaryStaff> findByCategory(String category);

    List<TemporaryStaff> findByEmpNameContainingIgnoreCase(String name);

    @Query("SELECT t FROM TemporaryStaff t ORDER BY t.empNo")
    List<TemporaryStaff> findAllOrderByEmpNo();
}
