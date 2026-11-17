package com.gtsvn.hrm.repository;

import com.gtsvn.hrm.entity.HopDongLaoDong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HopDongLaoDongRepository extends JpaRepository<HopDongLaoDong, Long> {
    Optional<HopDongLaoDong> findBySoHopDong(String soHopDong);
}
