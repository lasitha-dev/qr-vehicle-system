package com.uop.qrvehicle.repository;

import com.uop.qrvehicle.model.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Visitor Repository - Data access for Visitor entity
 */
@Repository
public interface VisitorRepository extends JpaRepository<Visitor, String> {

    List<Visitor> findByNameContainingIgnoreCase(String name);

    @Query("SELECT v FROM Visitor v WHERE :now BETWEEN v.dateFrom AND v.dateTo")
    List<Visitor> findActiveVisitors(LocalDateTime now);

    @Query("SELECT v FROM Visitor v WHERE v.dateTo < :now")
    List<Visitor> findExpiredVisitors(LocalDateTime now);

    List<Visitor> findAllByOrderByIdDesc();
}
