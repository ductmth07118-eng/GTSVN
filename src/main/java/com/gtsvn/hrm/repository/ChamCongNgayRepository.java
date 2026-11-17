package com.gtsvn.hrm.repository;

import com.gtsvn.hrm.entity.ChamCongNgay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChamCongNgayRepository extends JpaRepository<ChamCongNgay, Long> {
    List<ChamCongNgay> findByNhanVienIdOrderByNgayLamViecAsc(Long nhanVienId);
    List<ChamCongNgay> findByNhanVienIdAndNgayLamViecBetweenOrderByNgayLamViecAsc(
            Long nhanVienId, LocalDate start, LocalDate end
    );
    Optional<ChamCongNgay> findByNhanVienIdAndNgayLamViec(Long nhanVienId, LocalDate ngayLamViec);
}
