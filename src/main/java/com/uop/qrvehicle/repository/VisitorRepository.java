package com.uop.qrvehicle.repository;

import com.uop.qrvehicle.model.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Visitor Repository - Data access for Visitor entity
 */
@Repository
public interface VisitorRepository extends JpaRepository<Visitor, Long> {

    List<Visitor> findByNameContainingIgnoreCase(String name);

    @Query("SELECT v FROM Visitor v WHERE :today BETWEEN v.dateFrom AND v.dateTo")
    List<Visitor> findActiveVisitors(LocalDate today);

    @Query("SELECT v FROM Visitor v WHERE v.dateTo < :today")
    List<Visitor> findExpiredVisitors(LocalDate today);

    List<Visitor> findAllByOrderByIdDesc();
}
