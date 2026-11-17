package com.gtsvn.hrm.repository;

import com.gtsvn.hrm.entity.ChinhSachNhanSu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChinhSachNhanSuRepository extends JpaRepository<ChinhSachNhanSu, Long> {

    Optional<ChinhSachNhanSu> findByMaDieuKhoan(String maDieuKhoan);

    List<ChinhSachNhanSu> findByDanhMuc(String danhMuc);

    @Query("SELECT c FROM ChinhSachNhanSu c WHERE " +
           "LOWER(c.tieuDe) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.noiDungChiTiet) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.danhMuc) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<ChinhSachNhanSu> searchByKeyword(@Param("keyword") String keyword);
}
