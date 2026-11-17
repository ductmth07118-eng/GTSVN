-- ===================================================================
-- HỆ THỐNG QUẢN TRỊ NHÂN SỰ GTS VIỆT NAM (GTS-HRM)
-- DATABASE SCHEMA SCRIPT (MICROSOFT SQL SERVER)
-- ===================================================================

-- 1. Bảng Đơn xin phép (Tích hợp AI Forensics & Traffic Light)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'DonXinPhep')
BEGIN
    CREATE TABLE DonXinPhep (
        Id BIGINT IDENTITY(1,1) PRIMARY KEY,
        NhanVienId BIGINT NOT NULL,
        LoaiNghiPhep NVARCHAR(50) NOT NULL,              -- 'NGHI_OM', 'VIEC_RIENG', 'THAI_SAN'
        NgayBatDau DATE NOT NULL,
        NgayKetThuc DATE NOT NULL,
        FileMinhChungUrl NVARCHAR(500),
        ChanDoanBenh NVARCHAR(MAX),                      -- Chẩn đoán y tế (cần Role-Based Masking)
        TrangThai NVARCHAR(30) DEFAULT 'CHO_DUYET',      -- 'CHO_DUYET', 'DA_DUYET', 'TU_CHOI'
        
        -- Phân luồng rủi ro & AI Forensics
        DoTinCay INT DEFAULT 100,                        -- 0 - 100 (Thang điểm tự tin)
        NhanRuiRo NVARCHAR(20) DEFAULT 'CO_XANH',        -- 'CO_XANH', 'CO_VANG', 'CO_DO'
        LyDoRuiRo NVARCHAR(500),                         -- Diễn giải cờ rủi ro (Photoshop, Ngày khám bất thường...)
        IsKhanCap BIT DEFAULT 0,                         -- 1: Bypassed blackout period, chuyển thẳng C-Level
        CreatedAt DATETIME2 DEFAULT SYSUTCDATETIME()
    );
END
GO

-- 2. Bảng Lịch sử truy cập (Audit Log phục vụ Data Governance & HIPAA/BHXH)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'LichSuTruyCap')
BEGIN
    CREATE TABLE LichSuTruyCap (
        Id BIGINT IDENTITY(1,1) PRIMARY KEY,
        TaiKhoanTruyCap NVARCHAR(100) NOT NULL,
        RoleName NVARCHAR(50) NOT NULL,
        DonXinPhepId BIGINT NOT NULL,
        HanhDong NVARCHAR(100) NOT NULL,                 -- 'XEM_CHAN_DOAN_BENH_NHAY_CAM'
        IpAddress NVARCHAR(50),
        CreatedAt DATETIME2 DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_LichSu_DonXinPhep FOREIGN KEY (DonXinPhepId) REFERENCES DonXinPhep(Id)
    );
END
GO

-- 3. Bảng Hợp đồng lao động (Đối chiếu Key-Entities JSON Diff)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'HopDongLaoDong')
BEGIN
    CREATE TABLE HopDongLaoDong (
        Id BIGINT IDENTITY(1,1) PRIMARY KEY,
        NhanVienId BIGINT NOT NULL,
        SoHopDong NVARCHAR(100) UNIQUE NOT NULL,
        JsonGoc NVARCHAR(MAX) NOT NULL,                  -- Ví dụ: {"MucLuong": 15000000, "ChucDanh": "Dev", "NgayBatDau": "2026-12-01"}
        JsonScanBocTach NVARCHAR(MAX),                   -- AI bóc tách từ bản scan
        FileScanUrl NVARCHAR(500),
        KetQuaDoiChieu NVARCHAR(20) DEFAULT 'KHOP',      -- 'KHOP', 'LECH_DIEU_KHOAN'
        ChiTietLech NVARCHAR(MAX),                       -- Lưu cụ thể trường nào bị sửa lén
        CreatedAt DATETIME2 DEFAULT SYSUTCDATETIME()
    );
END
GO

-- 4. Bảng Hàng đợi chống sập (Dead-Letter Queue khi AI quá tải / rớt mạng)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'DeadLetter_Queue')
BEGIN
    CREATE TABLE DeadLetter_Queue (
        Id BIGINT IDENTITY(1,1) PRIMARY KEY,
        LoaiTienTrinh NVARCHAR(50) NOT NULL,             -- 'OCR_GIAY_KHAM', 'DIFF_HOP_DONG'
        Payload NVARCHAR(MAX) NOT NULL,                  -- Tham số/File cần xử lý lại
        SoLanDaThu INT DEFAULT 0,
        LoiCuoiCung NVARCHAR(MAX),
        TrangThai NVARCHAR(30) DEFAULT 'PENDING',        -- 'PENDING', 'RESOLVED', 'FAILED'
        CreatedAt DATETIME2 DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2 DEFAULT SYSUTCDATETIME()
    );
END
GO

-- 5. Bảng Nhân viên & Dữ liệu mẫu (GTS Employee Directory)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'NhanVien')
BEGIN
    CREATE TABLE NhanVien (
        Id BIGINT PRIMARY KEY,
        HoTen NVARCHAR(100) NOT NULL,
        Email NVARCHAR(100),
        PhongBan NVARCHAR(100),
        ChucVu NVARCHAR(100),
        SoNgayPhepConLai INT DEFAULT 12,
        SoNgayDaNghi INT DEFAULT 0
    );

    INSERT INTO NhanVien (Id, HoTen, Email, PhongBan, ChucVu, SoNgayPhepConLai, SoNgayDaNghi) VALUES
    (101, N'Trần Hữu Tiến Minh', 'minh.tht@gtsvn.com.vn', N'Kỹ thuật Phần mềm', 'Senior Developer', 12, 3),
    (102, N'Lê Trọng Đạt', 'dat.lt@gtsvn.com.vn', N'Kinh doanh & Dự án', 'Account Manager', 5, 7),
    (103, N'Minh Đức Trịnh', 'duc.tm@gtsvn.com.vn', N'Nhân sự & Vận hành', 'HR C&B Lead', 10, 2);
END
GO
