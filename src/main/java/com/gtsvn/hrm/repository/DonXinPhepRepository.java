package com.gtsvn.hrm.repository;

import com.gtsvn.hrm.entity.DonXinPhep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DonXinPhepRepository extends JpaRepository<DonXinPhep, Long> {
    List<DonXinPhep> findByNhanRuiRo(String nhanRuiRo);
    List<DonXinPhep> findByTrangThai(String trangThai);

    @Query("SELECT d FROM DonXinPhep d WHERE d.nhanVienId = :nhanVienId " +
           "AND d.trangThai NOT IN ('TU_CHOI', 'TAM_KHOA_GIAN_LAN', 'BI_CHAN_BLACKOUT') " +
           "AND (d.ngayBatDau <= :end AND d.ngayKetThuc >= :start)")
    List<DonXinPhep> findOverlappingLeaves(
            @Param("nhanVienId") Long nhanVienId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
