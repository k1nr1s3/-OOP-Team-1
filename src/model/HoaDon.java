package model;

import iface.IGiamGia;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

/**
 * Hoá đơn bán hàng. Vòng đời: CHO_THANH_TOAN (đang gọi món) -> DA_THANH_TOAN hoặc DA_HUY.
 * Cách tính tiền:
 *   tổng tiền hàng - giảm theo hạng khách (IGiamGia) - khuyến mãi - đổi điểm = tiền sau giảm (= DOANH THU)
 *   tiền sau giảm + VAT = tổng thanh toán (số khách phải trả)
 * Sau khi thanh toán, mọi con số được chốt lại (k đổi nữa dù cấu hình VAT / giá sản phẩm thay đổi).
 *
 * Định dạng 1 dòng trong data/hoadon.txt (ngăn cách bằng '|'):
 *   maHD|ngayLap|maNV|tenNV|maKH|tenKH|trangThai|ngayThanhToan|phuongThuc|maKM|tyLeVAT
 *      |giamHang|giamKM|giamDiem|diemDoi|diemTichDuoc|tienKhachDua|ghiChu|chiTiet
 *   chiTiet: các món ngăn cách bằng ';', mỗi món dạng maSP~tenSP~loai~donGia~soLuong
 *   ngày giờ dạng yyyy-MM-dd'T'HH:mm:ss
 */
public class HoaDon {

    // ===================== Enum =====================
    public enum TrangThai {
        CHO_THANH_TOAN("Chờ thanh toán"),
        DA_THANH_TOAN("Đã thanh toán"),
        DA_HUY("Đã huỷ");

        private final String ten;

        TrangThai(String ten) { this.ten = ten; }

        public String getTen() { return ten; }
    }

    public enum PhuongThuc {
        TIEN_MAT("Tiền mặt"),
        CHUYEN_KHOAN("Chuyển khoản"),
        THE("Thẻ"),
        VI_DIEN_TU("Ví điện tử");

        private final String ten;

        PhuongThuc(String ten) { this.ten = ten; }

        public String getTen() { return ten; }
    }

    // ===================== Static =====================
    public static final String TEN_CUA_HANG = "FAST FOOD & DRINK";
    public static final String TEN_KHACH_VANG_LAI = "Khách vãng lai";
    public static final DateTimeFormatter DINH_DANG_HIEN_THI = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DINH_DANG_FILE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final int DO_RONG_BILL = 60;

    private static double tyLeVATMacDinh = 0.08;   // VAT áp cho hoá đơn thanh toán từ giờ (đổi bằng datTyLeVAT)
    private static int demHD = 0;                  // đếm để tự sinh mã HD00001, HD00002...

    public static String taoMaTuDong() {
        return String.format("HD%05d", ++demHD);
    }

    // cập nhật bộ đếm để mã tự sinh sau này k trùng với mã đọc từ file
    public static void capNhatBoDem(String ma) {
        try {
            int so = Integer.parseInt(ma.substring(2));
            if (so > demHD) {
                demHD = so;
            }
        } catch (RuntimeException e) {
            // mã k đúng dạng HDxxxxx thì bỏ qua
        }
    }

    public static double getTyLeVATMacDinh() {
        return tyLeVATMacDinh;
    }

    // tyLe trong [0, 1], ví dụ 0.08 = 8%. Chỉ ảnh hưởng hoá đơn thanh toán sau này, hoá đơn cũ giữ nguyên
    public static void datTyLeVAT(double tyLe) {
        if (tyLe < 0 || tyLe > 1) {
            throw new IllegalArgumentException("Tỷ lệ VAT phải trong khoảng 0 - 1");
        }
        tyLeVATMacDinh = tyLe;
    }

    // 1.234.567 (k kèm chữ VND, dùng cho bill cho gọn)
    public static String dinhDangSo(double soTien) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(Math.round(soTien));
    }

    // ===================== Thuộc tính =====================
    private String maHD;
    private LocalDateTime ngayLap;
    private String maNV;
    private String tenNV;
    private String maKH;                  // rỗng = khách vãng lai
    private String tenKH;
    private NhanVien nhanVien;            // chỉ có khi lập hoá đơn trong phiên chạy (đọc từ file thì null)

    private TrangThai trangThai;
    private LocalDateTime ngayThanhToan;
    private PhuongThuc phuongThuc;
    private String maKM;                  // rỗng = k dùng khuyến mãi
    private double tyLeVAT;
    private double tienGiamHang;          // giảm theo hạng thành viên
    private double tienGiamKM;            // giảm do khuyến mãi
    private double tienGiamDiem;          // giảm do đổi điểm
    private int diemDoi;                  // số điểm đã dùng
    private int diemTichDuoc;             // số điểm cộng thêm sau khi mua
    private double tienKhachDua;
    private String ghiChu;                // lý do huỷ...

    private ChiTietHoaDon[] chiTiet;      // MẢNG các dòng món
    private int soDong;

    // ===================== Constructor =====================
    // lập hoá đơn mới ngay bây giờ, mã tự sinh. kh = null nghĩa là khách vãng lai
    public HoaDon(NhanVien nv, KhachHang kh) {
        this(nv, kh, LocalDateTime.now());
    }

    // chọn thời điểm lập (nhập hoá đơn cũ / demo). Kiểm tra quyền TRƯỚC rồi mới cấp mã, để mã hoá đơn k bị nhảy số
    public HoaDon(NhanVien nv, KhachHang kh, LocalDateTime ngayLap) {
        this(maMoiChoNhanVien(nv), ngayLap, nv, kh);
    }

    public HoaDon(String maHD, LocalDateTime ngayLap, NhanVien nv, KhachHang kh) {
        this(maHD, ngayLap, kiemTraNhanVien(nv).getMaNV(), nv.getHoTen(),
                kh == null ? "" : kh.getMaKH(), kh == null ? "" : kh.getHoTen());
        this.nhanVien = nv;
    }

    /**
     * Gắn lại nhân viên tương ứng cho hóa đơn được đọc từ file.
     */
    public void ganNhanVien(NhanVien nv) {
        if (nv == null || !maNV.equalsIgnoreCase(nv.getMaNV())) {
            throw new IllegalArgumentException("Nhân viên không khớp hóa đơn");
        }
        if (!(nv instanceof ThuNgan) && !(nv instanceof QuanLy)) {
            throw new IllegalArgumentException("Nhân viên không có quyền lập hóa đơn");
        }
        this.nhanVien = nv;
    }

    // constructor gốc, dùng chung (cũng là đường đọc từ file)
    private HoaDon(String maHD, LocalDateTime ngayLap, String maNV, String tenNV, String maKH, String tenKH) {
        if (maHD == null || maHD.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã hoá đơn không được rỗng");
        }
        if (ngayLap == null) {
            throw new IllegalArgumentException("Ngày lập không được rỗng");
        }
        this.maHD = maHD.trim();
        this.ngayLap = ngayLap;
        this.maNV = ChiTietHoaDon.lamSach(maNV);
        this.tenNV = ChiTietHoaDon.lamSach(tenNV);
        this.maKH = ChiTietHoaDon.lamSach(maKH);
        this.tenKH = ChiTietHoaDon.lamSach(tenKH);
        this.trangThai = TrangThai.CHO_THANH_TOAN;
        this.maKM = "";
        this.ghiChu = "";
        this.tyLeVAT = tyLeVATMacDinh;
        this.chiTiet = new ChiTietHoaDon[8];
        this.soDong = 0;
        capNhatBoDem(this.maHD);
    }

    private static String maMoiChoNhanVien(NhanVien nv) {
        kiemTraNhanVien(nv);
        return taoMaTuDong();
    }

    // chỉ thu ngân và quản lý được lập hoá đơn, pha chế thì k (đa hình: kiểm tra theo kiểu thật của đối tượng)
    private static NhanVien kiemTraNhanVien(NhanVien nv) {
        if (nv == null) {
            throw new IllegalArgumentException("Hoá đơn phải có nhân viên lập");
        }
        if (!(nv instanceof ThuNgan) && !(nv instanceof QuanLy)) {
            throw new IllegalArgumentException("Nhân viên " + nv.getHoTen() + " (" + nv.getVaiTro()
                    + ") k có quyền lập hoá đơn");
        }
        return nv;
    }

    // ===================== Gọi món (chỉ khi hoá đơn còn mở) =====================
    private void yeuCauDangMo() {
        if (trangThai != TrangThai.CHO_THANH_TOAN) {
            throw new IllegalStateException("Hoá đơn " + maHD + " " + trangThai.getTen().toLowerCase()
                    + ", k thể sửa");
        }
    }

    private int timDong(String ma) {
        if (ma == null) {
            return -1;
        }
        for (int i = 0; i < soDong; i++) {
            if (chiTiet[i].getMaSP().equalsIgnoreCase(ma.trim())) {
                return i;
            }
        }
        return -1;
    }

    private void themDong(ChiTietHoaDon ct) {
        if (soDong == chiTiet.length) {
            chiTiet = Arrays.copyOf(chiTiet, chiTiet.length * 2);
        }
        chiTiet[soDong++] = ct;
    }

    // thêm món, gọi lại món đã có thì cộng dồn số lượng vào dòng cũ
    public void themMon(SanPham sp, int soLuong) {
        yeuCauDangMo();
        ChiTietHoaDon moi = new ChiTietHoaDon(sp, soLuong);
        int vt = timDong(moi.getMaSP());
        if (vt == -1) {
            themDong(moi);
        } else {
            chiTiet[vt].tangSoLuong(soLuong);
        }
    }

    // bớt số lượng, về 0 (hoặc ít hơn) thì bỏ luôn dòng. Trả về false nếu k có món này
    public boolean giamMon(String maSP, int soLuong) {
        yeuCauDangMo();
        int vt = timDong(maSP);
        if (vt == -1 || soLuong <= 0) {
            return false;
        }
        int conLai = chiTiet[vt].getSoLuong() - soLuong;
        if (conLai <= 0) {
            return xoaMon(maSP);
        }
        chiTiet[vt].setSoLuong(conLai);
        return true;
    }

    public boolean xoaMon(String maSP) {
        yeuCauDangMo();
        int vt = timDong(maSP);
        if (vt == -1) {
            return false;
        }
        for (int i = vt; i < soDong - 1; i++) {
            chiTiet[i] = chiTiet[i + 1];
        }
        chiTiet[--soDong] = null;
        return true;
    }

    // ===================== Tính tiền =====================
    public double tongTienHang() {
        double tong = 0;
        for (int i = 0; i < soDong; i++) {
            tong += chiTiet[i].thanhTien();
        }
        return tong;
    }

    public int tongSoLuong() {
        int tong = 0;
        for (int i = 0; i < soDong; i++) {
            tong += chiTiet[i].getSoLuong();
        }
        return tong;
    }

    public double tongGiam() {
        return tienGiamHang + tienGiamKM + tienGiamDiem;
    }

    // hoá đơn còn mở thì dùng VAT hiện hành, đã chốt thì dùng VAT lúc thanh toán
    private double tyLeVATApDung() {
        return trangThai == TrangThai.CHO_THANH_TOAN ? tyLeVATMacDinh : tyLeVAT;
    }

    public double tienSauGiam() {
        return Math.max(0, tongTienHang() - tongGiam());
    }

    public double tienVAT() {
        return Math.round(tienSauGiam() * tyLeVATApDung());
    }

    public double tongThanhToan() {
        return tienSauGiam() + tienVAT();
    }

    public double tienThoi() {
        return trangThai == TrangThai.DA_THANH_TOAN ? Math.max(0, tienKhachDua - tongThanhToan()) : 0;
    }

    // doanh thu của hoá đơn = tiền sau giảm (chưa VAT), chỉ tính khi đã thanh toán
    public double getDoanhThu() {
        return trangThai == TrangThai.DA_THANH_TOAN ? tienSauGiam() : 0;
    }

    // doanh thu của riêng 1 dòng món: giảm giá được chia theo tỉ lệ, nên cộng các dòng lại = đúng doanh thu hoá đơn
    // (để thống kê theo sản phẩm / loại khớp với thống kê theo ngày, tháng)
    public double doanhThuDong(ChiTietHoaDon ct) {
        double tong = tongTienHang();
        return tong <= 0 ? 0 : ct.thanhTien() * getDoanhThu() / tong;
    }

    // kết quả tạm của 1 lần tính tiền (chưa ghi vào hoá đơn)
    private static class KetQuaTinh {
        double giamHang;
        double giamKM;
        double giamDiem;
        int diemDung;
        double sauGiam;
        double vat;
        double tong;
    }

    // tính tiền theo khách + khuyến mãi + điểm muốn đổi, k thay đổi gì ở hoá đơn / khách. Sai điều kiện thì ném lỗi
    private KetQuaTinh tinh(KhachHang kh, KhuyenMai km, int diemMuonDoi, LocalDate ngay) {
        KetQuaTinh r = new KetQuaTinh();
        double tong = tongTienHang();

        IGiamGia theoHang = kh;   // đa hình: khách thường -> 0, khách thân thiết -> theo hạng
        r.giamHang = theoHang == null ? 0 : Math.min(Math.round(theoHang.tinhGiamGia(tong)), tong);

        if (km != null) {
            double giam = Math.round(km.tinhGiamGia(tong, ngay));
            if (giam <= 0) {
                throw new IllegalArgumentException("Khuyến mãi " + km.getMaKM()
                        + " k áp dụng được (hết hạn, chưa tới ngày hoặc chưa đủ đơn tối thiểu)");
            }
            r.giamKM = Math.min(giam, tong - r.giamHang);
        }

        double conLai = tong - r.giamHang - r.giamKM;
        if (diemMuonDoi < 0) {
            throw new IllegalArgumentException("Số điểm đổi k được âm");
        }
        if (diemMuonDoi > 0) {
            if (kh == null) {
                throw new IllegalArgumentException("Khách vãng lai k có điểm để đổi");
            }
            if (diemMuonDoi > kh.getDiemTichLuy()) {
                throw new IllegalArgumentException("Khách chỉ có " + kh.getDiemTichLuy() + " điểm");
            }
            // k đổi quá số tiền còn phải trả
            r.diemDung = (int) Math.min(diemMuonDoi, Math.floor(conLai / KhachHang.GIA_TRI_DIEM));
            r.giamDiem = (double) r.diemDung * KhachHang.GIA_TRI_DIEM;
        }

        r.sauGiam = conLai - r.giamDiem;
        r.vat = Math.round(r.sauGiam * tyLeVATMacDinh);
        r.tong = r.sauGiam + r.vat;
        return r;
    }

    // xem trước số tiền phải trả (cho thu ngân báo khách) mà chưa thanh toán
    public double xemTruocThanhToan(KhachHang kh, KhuyenMai km, int diemMuonDoi) {
        yeuCauDangMo();
        doiChieuKhach(kh);
        return tinh(kh, km, diemMuonDoi, LocalDate.now()).tong;
    }

    // khách truyền vào phải đúng khách đã gắn trên hoá đơn
    private void doiChieuKhach(KhachHang kh) {
        if (kh == null && !maKH.isEmpty()) {
            throw new IllegalArgumentException("Hoá đơn gắn với khách " + maKH + ", cần truyền khách này vào");
        }
        if (kh != null && !kh.getMaKH().equalsIgnoreCase(maKH)) {
            throw new IllegalArgumentException("Khách " + kh.getMaKH() + " k phải khách của hoá đơn " + maHD);
        }
    }

    // ===================== Thanh toán / huỷ =====================
    // thanh toán ngay bây giờ, trả về tiền thối. Có lỗi thì ném ngoại lệ và KHÔNG thay đổi gì (kể cả điểm của khách)
    public double thanhToan(KhachHang kh, KhuyenMai km, int diemMuonDoi, PhuongThuc pt, double tienKhachDua) {
        return thanhToan(kh, km, diemMuonDoi, pt, tienKhachDua, LocalDateTime.now());
    }

    // bản có chọn thời điểm thanh toán (dùng để nhập hoá đơn cũ / chạy demo)
    public double thanhToan(KhachHang kh, KhuyenMai km, int diemMuonDoi, PhuongThuc pt,
                            double tienKhachDua, LocalDateTime luc) {
        yeuCauDangMo();
        if (soDong == 0) {
            throw new IllegalStateException("Hoá đơn chưa có món nào");
        }
        if (pt == null) {
            throw new IllegalArgumentException("Phải chọn phương thức thanh toán");
        }
        if (luc == null || luc.isBefore(ngayLap)) {
            throw new IllegalArgumentException("Thời điểm thanh toán k hợp lệ");
        }
        doiChieuKhach(kh);

        // --- kiểm tra hết trước, đúng rồi mới thay đổi ---
        KetQuaTinh r = tinh(kh, km, diemMuonDoi, luc.toLocalDate());
        double khachDua = (pt == PhuongThuc.TIEN_MAT) ? tienKhachDua : r.tong;
        if (khachDua < r.tong) {
            throw new IllegalArgumentException("Khách đưa thiếu: cần " + IGiamGia.dinhDangTien(r.tong)
                    + ", mới đưa " + IGiamGia.dinhDangTien(khachDua));
        }

        if (r.diemDung > 0) {
            kh.doiDiem(r.diemDung);
        }
        this.tienGiamHang = r.giamHang;
        this.tienGiamKM = r.giamKM;
        this.tienGiamDiem = r.giamDiem;
        this.diemDoi = r.diemDung;
        this.maKM = km == null ? "" : km.getMaKM();
        this.tyLeVAT = tyLeVATMacDinh;
        this.phuongThuc = pt;
        this.tienKhachDua = khachDua;
        this.ngayThanhToan = luc;
        this.trangThai = TrangThai.DA_THANH_TOAN;

        // cộng doanh thu quầy cho thu ngân (để tính hoa hồng), quản lý thì k có
        if (nhanVien instanceof ThuNgan) {
            ((ThuNgan) nhanVien).themDoanhThu(r.sauGiam);
        }
        return tienThoi();
    }

    public void huy(String lyDo) {
        yeuCauDangMo();
        String s = ChiTietHoaDon.lamSach(lyDo);
        this.ghiChu = s.isEmpty() ? "Không rõ lý do" : s;
        this.trangThai = TrangThai.DA_HUY;
    }

    // DanhSachHoaDon gọi sau khi tích điểm cho khách xong
    public void setDiemTichDuoc(int diem) {
        if (trangThai != TrangThai.DA_THANH_TOAN || diem < 0) {
            throw new IllegalStateException("Chỉ ghi điểm tích được cho hoá đơn đã thanh toán");
        }
        this.diemTichDuoc = diem;
    }

    // ===================== Đọc - ghi file =====================
    public String toFileString() {
        StringBuilder ds = new StringBuilder();
        for (int i = 0; i < soDong; i++) {
            if (i > 0) {
                ds.append(';');
            }
            ds.append(chiTiet[i].toFileString());
        }
        return String.join("|",
                maHD, ngayLap.format(DINH_DANG_FILE), maNV, tenNV, maKH, tenKH,
                trangThai.name(),
                ngayThanhToan == null ? "" : ngayThanhToan.format(DINH_DANG_FILE),
                phuongThuc == null ? "" : phuongThuc.name(),
                maKM, String.valueOf(tyLeVAT),
                String.valueOf(Math.round(tienGiamHang)), String.valueOf(Math.round(tienGiamKM)),
                String.valueOf(Math.round(tienGiamDiem)), String.valueOf(diemDoi), String.valueOf(diemTichDuoc),
                String.valueOf(Math.round(tienKhachDua)), ghiChu, ds.toString());
    }

    public static HoaDon fromFileString(String dong) {
        String[] f = dong.split("\\|", -1);
        if (f.length < 19) {
            throw new IllegalArgumentException("Thiếu dữ liệu (" + f.length + "/19 trường)");
        }
        HoaDon hd = new HoaDon(f[0], LocalDateTime.parse(f[1].trim(), DINH_DANG_FILE), f[2], f[3], f[4], f[5]);
        hd.trangThai = TrangThai.valueOf(f[6].trim());
        hd.ngayThanhToan = f[7].trim().isEmpty() ? null : LocalDateTime.parse(f[7].trim(), DINH_DANG_FILE);
        hd.phuongThuc = f[8].trim().isEmpty() ? null : PhuongThuc.valueOf(f[8].trim());
        hd.maKM = ChiTietHoaDon.lamSach(f[9]);
        hd.tyLeVAT = Double.parseDouble(f[10].trim());
        hd.tienGiamHang = Double.parseDouble(f[11].trim());
        hd.tienGiamKM = Double.parseDouble(f[12].trim());
        hd.tienGiamDiem = Double.parseDouble(f[13].trim());
        hd.diemDoi = Integer.parseInt(f[14].trim());
        hd.diemTichDuoc = Integer.parseInt(f[15].trim());
        hd.tienKhachDua = Double.parseDouble(f[16].trim());
        hd.ghiChu = ChiTietHoaDon.lamSach(f[17]);
        if (!f[18].trim().isEmpty()) {
            for (String ct : f[18].split(";")) {
                hd.themDong(ChiTietHoaDon.fromFileString(ct));
            }
        }
        if (hd.trangThai == TrangThai.DA_THANH_TOAN && (hd.ngayThanhToan == null || hd.phuongThuc == null)) {
            throw new IllegalArgumentException("Hoá đơn đã thanh toán nhưng thiếu ngày / phương thức thanh toán");
        }
        return hd;
    }

    // ===================== Get =====================
    public String getMaHD() { return maHD; }
    public LocalDateTime getNgayLap() { return ngayLap; }
    public String getMaNV() { return maNV; }
    public String getTenNV() { return tenNV; }
    public String getMaKH() { return maKH; }
    public String getTenKH() { return tenKH.isEmpty() ? TEN_KHACH_VANG_LAI : tenKH; }
    public TrangThai getTrangThai() { return trangThai; }
    public LocalDateTime getNgayThanhToan() { return ngayThanhToan; }
    public PhuongThuc getPhuongThuc() { return phuongThuc; }
    public String getMaKM() { return maKM; }
    public double getTyLeVAT() { return tyLeVATApDung(); }
    public double getTienGiamHang() { return tienGiamHang; }
    public double getTienGiamKM() { return tienGiamKM; }
    public double getTienGiamDiem() { return tienGiamDiem; }
    public int getDiemDoi() { return diemDoi; }
    public int getDiemTichDuoc() { return diemTichDuoc; }
    public double getTienKhachDua() { return tienKhachDua; }
    public String getGhiChu() { return ghiChu; }
    public int getSoDong() { return soDong; }

    // lấy dòng món theo vị trí, sai vị trí thì NULL
    public ChiTietHoaDon getChiTiet(int i) {
        return (i >= 0 && i < soDong) ? chiTiet[i] : null;
    }

    // bản sao mảng các dòng món (sửa mảng trả về k ảnh hưởng hoá đơn)
    public ChiTietHoaDon[] getDanhSachChiTiet() {
        return Arrays.copyOf(chiTiet, soDong);
    }

    // ===================== Hiển thị =====================
    public static String tieuDeBang() {
        return String.format("%-9s %-16s %-20s %-18s %-15s %16s",
                "Mã HĐ", "Thời gian", "Nhân viên", "Khách hàng", "Trạng thái", "Thanh toán");
    }

    @Override
    public String toString() {
        return String.format("%-9s %-16s %-20s %-18s %-15s %16s",
                maHD, ngayLap.format(DINH_DANG_HIEN_THI), cat(tenNV, 20), cat(getTenKH(), 18),
                trangThai.getTen(), IGiamGia.dinhDangTien(tongThanhToan()));
    }

    private static String cat(String s, int dai) {
        return s.length() <= dai ? s : s.substring(0, dai - 1) + "…";
    }

    private static String dong(String nhan, String giaTri) {
        return String.format("%-24s %s%n", nhan, giaTri);
    }

    // chuỗi để in hoá đơn ra màn hình (bill)
    public String taoBill() {
        String gach = "=".repeat(DO_RONG_BILL);
        String gachNhat = "-".repeat(DO_RONG_BILL);
        StringBuilder sb = new StringBuilder();
        sb.append(gach).append(System.lineSeparator());
        sb.append(canGiua(TEN_CUA_HANG)).append(System.lineSeparator());
        String tieuDe = "HOÁ ĐƠN BÁN HÀNG";
        if (trangThai == TrangThai.CHO_THANH_TOAN) {
            tieuDe += " (TẠM TÍNH)";
        } else if (trangThai == TrangThai.DA_HUY) {
            tieuDe += " (ĐÃ HUỶ)";
        }
        sb.append(canGiua(tieuDe)).append(System.lineSeparator());
        sb.append(gach).append(System.lineSeparator());
        sb.append(dong("Số hoá đơn:", maHD));
        sb.append(dong("Ngày lập:", ngayLap.format(DINH_DANG_HIEN_THI)));
        if (ngayThanhToan != null) {
            sb.append(dong("Thanh toán lúc:", ngayThanhToan.format(DINH_DANG_HIEN_THI)));
        }
        sb.append(dong("Nhân viên:", tenNV + " (" + maNV + ")"));
        sb.append(dong("Khách hàng:", maKH.isEmpty() ? TEN_KHACH_VANG_LAI : tenKH + " (" + maKH + ")"));
        sb.append(gachNhat).append(System.lineSeparator());
        sb.append(String.format("%-3s %-26s %3s %11s %12s%n", "STT", "Tên món", "SL", "Đơn giá", "Thành tiền"));
        for (int i = 0; i < soDong; i++) {
            ChiTietHoaDon ct = chiTiet[i];
            sb.append(String.format("%-3d %-26s %3d %11s %12s%n", i + 1, cat(ct.getTenSP(), 26),
                    ct.getSoLuong(), dinhDangSo(ct.getDonGia()), dinhDangSo(ct.thanhTien())));
        }
        sb.append(gachNhat).append(System.lineSeparator());
        sb.append(dong("Tổng tiền hàng:", IGiamGia.dinhDangTien(tongTienHang())));
        if (tienGiamHang > 0) {
            sb.append(dong("Giảm theo hạng:", "-" + IGiamGia.dinhDangTien(tienGiamHang)));
        }
        if (tienGiamKM > 0) {
            sb.append(dong("Khuyến mãi " + maKM + ":", "-" + IGiamGia.dinhDangTien(tienGiamKM)));
        }
        if (tienGiamDiem > 0) {
            sb.append(dong("Đổi " + diemDoi + " điểm:", "-" + IGiamGia.dinhDangTien(tienGiamDiem)));
        }
        sb.append(dong("Tiền sau giảm:", IGiamGia.dinhDangTien(tienSauGiam())));
        sb.append(dong("VAT " + String.format("%.0f", tyLeVATApDung() * 100) + "%:", IGiamGia.dinhDangTien(tienVAT())));
        sb.append(dong("TỔNG THANH TOÁN:", IGiamGia.dinhDangTien(tongThanhToan())));
        if (trangThai == TrangThai.DA_THANH_TOAN) {
            sb.append(dong("Hình thức:", phuongThuc.getTen()));
            if (phuongThuc == PhuongThuc.TIEN_MAT) {
                sb.append(dong("Khách đưa:", IGiamGia.dinhDangTien(tienKhachDua)));
                sb.append(dong("Tiền thối:", IGiamGia.dinhDangTien(tienThoi())));
            }
            if (diemTichDuoc > 0) {
                sb.append(dong("Điểm tích luỹ lần này:", "+" + diemTichDuoc));
            }
        }
        if (trangThai == TrangThai.DA_HUY) {
            sb.append(dong("Lý do huỷ:", ghiChu));
        }
        sb.append(gach).append(System.lineSeparator());
        sb.append(canGiua("Cảm ơn quý khách, hẹn gặp lại!")).append(System.lineSeparator());
        return sb.toString();
    }

    private static String canGiua(String s) {
        int le = Math.max(0, (DO_RONG_BILL - s.length()) / 2);
        return " ".repeat(le) + s;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof HoaDon && maHD.equalsIgnoreCase(((HoaDon) o).maHD);
    }

    @Override
    public int hashCode() {
        return maHD.toLowerCase().hashCode();
    }
}