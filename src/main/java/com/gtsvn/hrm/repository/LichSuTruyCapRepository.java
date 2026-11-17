package com.gtsvn.hrm.repository;

import com.gtsvn.hrm.entity.LichSuTruyCap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuTruyCapRepository extends JpaRepository<LichSuTruyCap, Long> {
    List<LichSuTruyCap> findByDonXinPhepId(Long donXinPhepId);
}
