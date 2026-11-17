package com.gtsvn.hrm.repository;

import com.gtsvn.hrm.entity.NgayLe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NgayLeRepository extends JpaRepository<NgayLe, Long> {
    List<NgayLe> findByNgayLeBetween(LocalDate startDate, LocalDate endDate);
    boolean existsByNgayLe(LocalDate date);
}
