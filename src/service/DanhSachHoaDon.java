package service;

import iface.IFile;
import iface.IGiamGia;
import iface.IQuanLy;
import iface.IThongKe;
import model.ChiTietHoaDon;
import model.DongThongKe;
import model.HoaDon;
import model.HoaDon.PhuongThuc;
import model.HoaDon.TrangThai;
import model.KhachHang;
import model.KhuyenMai;
import model.NhanVien;
import model.SanPham;
import model.ThuNgan;
import model.QuanLy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Quản lý hoá đơn: lập hoá đơn, gọi món, thanh toán, huỷ, tìm kiếm, thống kê doanh thu
 * theo ngày / tháng / năm / sản phẩm / loại / nhân viên / phương thức thanh toán.
 * Định dạng file xem ở comment đầu lớp HoaDon. Dùng MẢNG HoaDon[].
 *
 * Quy ước thống kê: chỉ tính hoá đơn ĐÃ THANH TOÁN, tính theo NGÀY THANH TOÁN.
 * "Doanh thu" = tiền sau giảm giá, CHƯA gồm VAT (VAT là tiền thu hộ nhà nước).
 */
public class DanhSachHoaDon implements IQuanLy<HoaDon>, IFile, IThongKe {

    private static final String[] THU = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"};

    private HoaDon[] ds;
    private int n;

    // ===================== Constructor =====================
    public DanhSachHoaDon() {
        this(10);
    }

    public DanhSachHoaDon(int sucChua) {
        ds = new HoaDon[Math.max(sucChua, 1)];
        n = 0;
    }

    // ===================== TOOLS =====================
    private void moRong() {
        if (n == ds.length) {
            ds = Arrays.copyOf(ds, ds.length * 2);
        }
    }

    private int timViTri(String ma) {
        if (ma == null) {
            return -1;
        }
        for (int i = 0; i < n; i++) {
            if (ds[i].getMaHD().equalsIgnoreCase(ma.trim())) {
                return i;
            }
        }
        return -1;
    }

    private static String cat(String s, int dai) {
        return s.length() <= dai ? s : s.substring(0, dai - 1) + "…";
    }

    private static void kiemTraKhoang(LocalDate tu, LocalDate den) {
        if (tu == null || den == null || tu.isAfter(den)) {
            throw new IllegalArgumentException("Khoảng ngày thống kê k hợp lệ");
        }
    }

    // ===================== IQuanLy =====================
    @Override
    public int soLuong() {
        return n;
    }

    // lấy hoá đơn theo vị trí, sai vị trí thì NULL
    public HoaDon get(int i) {
        return (i >= 0 && i < n) ? ds[i] : null;
    }

    @Override
    public boolean them(HoaDon hd) {
        if (hd == null || timViTri(hd.getMaHD()) != -1) {
            return false;
        }
        moRong();
        ds[n++] = hd;
        return true;
    }

    // chỉ xoá được hoá đơn chưa thanh toán (đang mở / đã huỷ). Hoá đơn đã thanh toán là chứng từ kế toán, k xoá
    @Override
    public boolean xoa(String ma) {
        int vt = timViTri(ma);
        if (vt == -1 || ds[vt].getTrangThai() == TrangThai.DA_THANH_TOAN) {
            return false;
        }
        for (int i = vt; i < n - 1; i++) {
            ds[i] = ds[i + 1];
        }
        ds[--n] = null;
        return true;
    }

    @Override
    public HoaDon timTheoMa(String ma) {
        int vt = timViTri(ma);
        return vt == -1 ? null : ds[vt];
    }

    @Override
    public void hienThi() {
        System.out.println(HoaDon.tieuDeBang());
        for (int i = 0; i < n; i++) {
            System.out.println(ds[i]);
        }
        System.out.println("Tổng: " + n + " hoá đơn");
    }

    public static void inMang(HoaDon[] mang) {
        System.out.println(HoaDon.tieuDeBang());
        for (HoaDon hd : mang) {
            System.out.println(hd);
        }
        System.out.println("Tìm thấy: " + mang.length + " hoá đơn");
    }

    // ===================== Nghiệp vụ: lập hoá đơn - gọi món - thanh toán - huỷ =====================
    // lập hoá đơn mới (mở sẵn để gọi món). kh = null là khách vãng lai
    public HoaDon lapHoaDon(NhanVien nv, KhachHang kh) {
        return lapHoaDon(nv, kh, LocalDateTime.now());
    }

    // bản chọn thời điểm lập (nhập hoá đơn cũ / demo)
    public HoaDon lapHoaDon(NhanVien nv, KhachHang kh, LocalDateTime luc) {
        HoaDon hd = new HoaDon(nv, kh, luc);   // ném lỗi (và k tốn mã) nếu nv k được phép lập
        them(hd);
        return hd;
    }

    // lập hoá đơn theo mã: tra nhân viên trong dsNV, tra khách trong dsKH (maKH rỗng/null = khách vãng lai)
    public HoaDon lapHoaDon(DanhSachNhanVien dsNV, String maNV, DanhSachKhachHang dsKH, String maKH) {
        NhanVien nv = dsNV.timTheoMa(maNV);
        if (nv == null) {
            throw new IllegalArgumentException("Không có nhân viên " + maNV);
        }
        KhachHang kh = null;
        if (maKH != null && !maKH.trim().isEmpty()) {
            kh = dsKH.timTheoMa(maKH);
            if (kh == null) {
                throw new IllegalArgumentException("Không có khách hàng " + maKH);
            }
        }
        return lapHoaDon(nv, kh);
    }

    private HoaDon layHoaDon(String maHD) {
        HoaDon hd = timTheoMa(maHD);
        if (hd == null) {
            throw new IllegalArgumentException("Không có hoá đơn " + maHD);
        }
        return hd;
    }

    public void themMon(String maHD, SanPham sp, int soLuong) {
        layHoaDon(maHD).themMon(sp, soLuong);
    }

    public boolean giamMon(String maHD, String maSP, int soLuong) {
        return layHoaDon(maHD).giamMon(maSP, soLuong);
    }

    public boolean xoaMon(String maHD, String maSP) {
        return layHoaDon(maHD).xoaMon(maSP);
    }

    /**
     * Thanh toán hoá đơn: tính giảm giá (hạng + khuyến mãi + đổi điểm) + VAT, thu tiền,
     * rồi tích điểm / nâng hạng cho khách và cộng doanh thu quầy cho thu ngân.
     * @param dsKH   danh sách khách (null được nếu hoá đơn của khách vãng lai)
     * @param km     khuyến mãi muốn áp dụng (null = k dùng)
     * @param diemDoi số điểm muốn đổi (0 = k đổi)
     * @return tiền thối. Có lỗi thì ném ngoại lệ và k thay đổi gì
     */
    public double thanhToan(String maHD, DanhSachKhachHang dsKH, KhuyenMai km, int diemDoi,
                            PhuongThuc pt, double tienKhachDua) {
        return thanhToan(maHD, dsKH, km, diemDoi, pt, tienKhachDua, LocalDateTime.now());
    }

    public double thanhToan(String maHD, DanhSachKhachHang dsKH, KhuyenMai km, int diemDoi,
                            PhuongThuc pt, double tienKhachDua, LocalDateTime luc) {
        HoaDon hd = layHoaDon(maHD);
        KhachHang kh = null;
        if (!hd.getMaKH().isEmpty()) {
            if (dsKH == null) {
                throw new IllegalArgumentException("Hoá đơn của khách " + hd.getMaKH() + ", cần danh sách khách hàng");
            }
            kh = dsKH.timTheoMa(hd.getMaKH());
            if (kh == null) {
                throw new IllegalArgumentException("Không còn khách hàng " + hd.getMaKH() + " trong danh sách");
            }
        }
        double tienThoi = hd.thanhToan(kh, km, diemDoi, pt, tienKhachDua, luc);
        if (kh != null) {
            // số tiền thực trả -> tích điểm, đủ mức thì tự nâng lên khách thân thiết
            int diem = dsKH.ghiNhanMuaHang(kh.getMaKH(), hd.tongThanhToan());
            hd.setDiemTichDuoc(Math.max(diem, 0));
        }
        return tienThoi;
    }

    public void huyHoaDon(String maHD, String lyDo) {
        layHoaDon(maHD).huy(lyDo);
    }

    /**
     * Gắn lại đối tượng nhân viên cho hoá đơn đọc từ file để các hoá đơn chờ
     * thanh toán vẫn cập nhật đúng doanh thu cho thu ngân.
     */
    public int ganNhanVien(DanhSachNhanVien dsNV) {
        if (dsNV == null) {
            throw new IllegalArgumentException("Danh sách nhân viên không được null");
        }
        int dem = 0;
        for (int i = 0; i < n; i++) {
            NhanVien nv = dsNV.timTheoMa(ds[i].getMaNV());
            if (nv instanceof ThuNgan || nv instanceof QuanLy) {
                ds[i].ganNhanVien(nv);
                dem++;
            }
        }
        return dem;
    }

    // ===================== Tìm kiếm / lọc =====================
    private HoaDon[] loc(java.util.function.Predicate<HoaDon> dieuKien) {
        HoaDon[] tam = new HoaDon[n];
        int dem = 0;
        for (int i = 0; i < n; i++) {
            if (dieuKien.test(ds[i])) {
                tam[dem++] = ds[i];
            }
        }
        return Arrays.copyOf(tam, dem);
    }

    public HoaDon[] locTheoTrangThai(TrangThai tt) {
        return loc(hd -> hd.getTrangThai() == tt);
    }

    public HoaDon[] locTheoNhanVien(String maNV) {
        return loc(hd -> hd.getMaNV().equalsIgnoreCase(maNV));
    }

    public HoaDon[] locTheoKhachHang(String maKH) {
        return loc(hd -> !hd.getMaKH().isEmpty() && hd.getMaKH().equalsIgnoreCase(maKH));
    }

    // hoá đơn LẬP trong khoảng ngày (mọi trạng thái)
    public HoaDon[] locTheoNgayLap(LocalDate tu, LocalDate den) {
        kiemTraKhoang(tu, den);
        return loc(hd -> !hd.getNgayLap().toLocalDate().isBefore(tu) && !hd.getNgayLap().toLocalDate().isAfter(den));
    }

    // hoá đơn ĐÃ THANH TOÁN trong khoảng ngày [tu, den] (tính theo ngày thanh toán) - nền của mọi thống kê
    public HoaDon[] daThanhToanTrong(LocalDate tu, LocalDate den) {
        kiemTraKhoang(tu, den);
        return loc(hd -> hd.getTrangThai() == TrangThai.DA_THANH_TOAN
                && !hd.getNgayThanhToan().toLocalDate().isBefore(tu)
                && !hd.getNgayThanhToan().toLocalDate().isAfter(den));
    }

    // ===================== Thống kê: gom nhóm =====================
    // gom theo hoá đơn (mỗi hoá đơn vào đúng 1 nhóm): theo ngày, tháng, nhân viên, phương thức...
    private DongThongKe[] gomTheoHoaDon(LocalDate tu, LocalDate den,
                                        Function<HoaDon, String> khoa, Function<HoaDon, String> ten) {
        Map<String, DongThongKe> nhom = new LinkedHashMap<>();
        for (HoaDon hd : daThanhToanTrong(tu, den)) {
            DongThongKe d = nhom.computeIfAbsent(khoa.apply(hd), k -> new DongThongKe(k, ten.apply(hd)));
            d.cong(1, hd.tongSoLuong(), hd.getDoanhThu());
        }
        return nhom.values().toArray(new DongThongKe[0]);
    }

    // gom theo dòng món (1 hoá đơn có thể góp vào nhiều nhóm): theo sản phẩm, loại sản phẩm
    private DongThongKe[] gomTheoDong(LocalDate tu, LocalDate den,
                                      Function<ChiTietHoaDon, String> khoa, Function<ChiTietHoaDon, String> ten) {
        Map<String, DongThongKe> nhom = new LinkedHashMap<>();
        for (HoaDon hd : daThanhToanTrong(tu, den)) {
            Set<String> daDem = new HashSet<>();   // mỗi nhóm chỉ đếm hoá đơn 1 lần
            for (ChiTietHoaDon ct : hd.getDanhSachChiTiet()) {
                String k = khoa.apply(ct);
                DongThongKe d = nhom.computeIfAbsent(k, x -> new DongThongKe(x, ten.apply(ct)));
                d.cong(daDem.add(k) ? 1 : 0, ct.getSoLuong(), hd.doanhThuDong(ct));
            }
        }
        return nhom.values().toArray(new DongThongKe[0]);
    }

    private static DongThongKe[] sapTheoKhoa(DongThongKe[] mang) {
        Arrays.sort(mang, Comparator.comparing(DongThongKe::getKhoa));
        return mang;
    }

    private static DongThongKe[] sapTheoDoanhThu(DongThongKe[] mang) {
        Arrays.sort(mang, Comparator.comparingDouble(DongThongKe::getDoanhThu).reversed());
        return mang;
    }

    // ===================== Thống kê: doanh thu =====================
    @Override
    public double doanhThu(LocalDate tu, LocalDate den) {
        double tong = 0;
        for (HoaDon hd : daThanhToanTrong(tu, den)) {
            tong += hd.getDoanhThu();
        }
        return tong;
    }

    public int demHoaDonDaThanhToan(LocalDate tu, LocalDate den) {
        return daThanhToanTrong(tu, den).length;
    }

    // doanh thu từng NGÀY trong khoảng (chỉ các ngày có bán), xếp theo ngày tăng dần
    @Override
    public DongThongKe[] thongKeTheoNgay(LocalDate tu, LocalDate den) {
        return sapTheoKhoa(gomTheoHoaDon(tu, den,
                hd -> hd.getNgayThanhToan().toLocalDate().toString(),
                hd -> {
                    LocalDate d = hd.getNgayThanhToan().toLocalDate();
                    return THU[d.getDayOfWeek().getValue() - 1] + " " + String.format("%02d/%02d", d.getDayOfMonth(), d.getMonthValue());
                }));
    }

    // doanh thu 12 THÁNG của 1 năm (tháng k bán gì vẫn có dòng, bằng 0)
    @Override
    public DongThongKe[] thongKeTheoThang(int nam) {
        DongThongKe[] kq = new DongThongKe[12];
        for (int t = 1; t <= 12; t++) {
            kq[t - 1] = new DongThongKe(String.format("%d-%02d", nam, t), "Tháng " + t + "/" + nam);
        }
        for (HoaDon hd : daThanhToanTrong(LocalDate.of(nam, 1, 1), LocalDate.of(nam, 12, 31))) {
            kq[hd.getNgayThanhToan().getMonthValue() - 1].cong(1, hd.tongSoLuong(), hd.getDoanhThu());
        }
        return kq;
    }

    // theo sản phẩm, xếp doanh thu cao -> thấp (gom theo MÃ sản phẩm, tên/giá lấy lúc bán)
    @Override
    public DongThongKe[] thongKeTheoSanPham(LocalDate tu, LocalDate den) {
        return sapTheoDoanhThu(gomTheoDong(tu, den, ChiTietHoaDon::getMaSP, ChiTietHoaDon::getTenSP));
    }

    // k sản phẩm BÁN CHẠY nhất (theo số lượng; bằng nhau thì doanh thu cao hơn đứng trước)
    @Override
    public DongThongKe[] topSanPham(LocalDate tu, LocalDate den, int k) {
        DongThongKe[] mang = gomTheoDong(tu, den, ChiTietHoaDon::getMaSP, ChiTietHoaDon::getTenSP);
        Arrays.sort(mang, Comparator.comparingInt(DongThongKe::getSoLuong).reversed()
                .thenComparing(Comparator.comparingDouble(DongThongKe::getDoanhThu).reversed()));
        return Arrays.copyOf(mang, Math.min(Math.max(k, 0), mang.length));
    }

    // theo loại sản phẩm (Đồ ăn / Thức uống / Combo)
    public DongThongKe[] thongKeTheoLoai(LocalDate tu, LocalDate den) {
        return sapTheoDoanhThu(gomTheoDong(tu, den, ChiTietHoaDon::getLoai, ChiTietHoaDon::getLoai));
    }

    // theo nhân viên lập hoá đơn
    @Override
    public DongThongKe[] thongKeTheoNhanVien(LocalDate tu, LocalDate den) {
        return sapTheoDoanhThu(gomTheoHoaDon(tu, den, HoaDon::getMaNV, HoaDon::getTenNV));
    }

    public DongThongKe[] thongKeTheoPhuongThuc(LocalDate tu, LocalDate den) {
        return sapTheoDoanhThu(gomTheoHoaDon(tu, den,
                hd -> hd.getPhuongThuc().name(), hd -> hd.getPhuongThuc().getTen()));
    }

    // ===================== Báo cáo in ra màn hình =====================
    private static void inBang(String tieuDe, String nhanKhoa, String nhanTen, DongThongKe[] rows) {
        double tong = 0;
        for (DongThongKe d : rows) {
            tong += d.getDoanhThu();
        }
        inBang(tieuDe, nhanKhoa, nhanTen, rows, tong);
    }

    // tongChuan: tổng dùng làm mẫu số tính tỷ trọng (bảng Top N thì phải là tổng cả kỳ, k phải tổng N dòng)
    private static void inBang(String tieuDe, String nhanKhoa, String nhanTen, DongThongKe[] rows, double tongChuan) {
        System.out.println("--- " + tieuDe + " ---");
        System.out.printf("%-12s %-26s %6s %7s %16s %8s%n", nhanKhoa, nhanTen, "Số HĐ", "SL", "Doanh thu", "Tỷ trọng");
        if (rows.length == 0) {
            System.out.println("(k có dữ liệu)");
            return;
        }
        for (DongThongKe d : rows) {
            System.out.printf("%-12s %-26s %6d %7d %16s %7.1f%%%n", cat(d.getKhoa(), 12), cat(d.getTen(), 26),
                    d.getSoHoaDon(), d.getSoLuong(), IGiamGia.dinhDangTien(d.getDoanhThu()),
                    tongChuan > 0 ? d.getDoanhThu() / tongChuan * 100 : 0);
        }
    }

    // số liệu tổng hợp của khoảng ngày
    public void inBaoCaoTongHop(LocalDate tu, LocalDate den) {
        HoaDon[] daTra = daThanhToanTrong(tu, den);
        double tongHang = 0, giam = 0, doanhThu = 0, vat = 0, tongThu = 0;
        int soMon = 0;
        for (HoaDon hd : daTra) {
            tongHang += hd.tongTienHang();
            giam += hd.tongGiam();
            doanhThu += hd.getDoanhThu();
            vat += hd.tienVAT();
            tongThu += hd.tongThanhToan();
            soMon += hd.tongSoLuong();
        }
        int soHuy = loc(hd -> hd.getTrangThai() == TrangThai.DA_HUY
                && !hd.getNgayLap().toLocalDate().isBefore(tu) && !hd.getNgayLap().toLocalDate().isAfter(den)).length;

        System.out.println("Số hoá đơn đã thanh toán : " + daTra.length + "   (huỷ: " + soHuy + ")");
        System.out.println("Số món bán ra            : " + soMon);
        System.out.println("Tổng tiền hàng           : " + IGiamGia.dinhDangTien(tongHang));
        System.out.println("Giảm giá (hạng+KM+điểm)  : -" + IGiamGia.dinhDangTien(giam));
        System.out.println("DOANH THU (chưa VAT)     : " + IGiamGia.dinhDangTien(doanhThu));
        System.out.println("VAT thu hộ               : " + IGiamGia.dinhDangTien(vat));
        System.out.println("Tổng tiền đã thu         : " + IGiamGia.dinhDangTien(tongThu));
        if (daTra.length > 0) {
            System.out.println("Doanh thu TB / hoá đơn   : " + IGiamGia.dinhDangTien(doanhThu / daTra.length));
        }
    }

    @Override
    public void inBaoCaoNgay(LocalDate ngay) {
        System.out.println("========== BÁO CÁO DOANH THU NGÀY " + ngay.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " (" + THU[ngay.getDayOfWeek().getValue() - 1] + ") ==========");
        inBaoCaoTongHop(ngay, ngay);
        inBang("Theo sản phẩm", "Mã SP", "Tên", thongKeTheoSanPham(ngay, ngay));
        inBang("Theo nhân viên", "Mã NV", "Tên", thongKeTheoNhanVien(ngay, ngay));
        inBang("Theo phương thức thanh toán", "Mã", "Phương thức", thongKeTheoPhuongThuc(ngay, ngay));
    }

    @Override
    public void inBaoCaoThang(int thang, int nam) {
        YearMonth ym = YearMonth.of(nam, thang);
        LocalDate dau = ym.atDay(1);
        LocalDate cuoi = ym.atEndOfMonth();
        System.out.printf("========== BÁO CÁO DOANH THU THÁNG %02d/%d ==========%n", thang, nam);
        inBaoCaoTongHop(dau, cuoi);

        // so sánh với tháng trước
        YearMonth truoc = ym.minusMonths(1);
        double dtNay = doanhThu(dau, cuoi);
        double dtTruoc = doanhThuThang(truoc.getMonthValue(), truoc.getYear());
        if (dtTruoc > 0) {
            System.out.printf("So với tháng %02d/%d       : %+.1f%% (%s)%n", truoc.getMonthValue(), truoc.getYear(),
                    (dtNay - dtTruoc) / dtTruoc * 100, IGiamGia.dinhDangTien(dtTruoc));
        } else {
            System.out.printf("So với tháng %02d/%d       : k có dữ liệu tháng trước%n", truoc.getMonthValue(), truoc.getYear());
        }

        inBang("Doanh thu từng ngày", "Ngày", "Thứ", thongKeTheoNgay(dau, cuoi));
        DongThongKe[] top = topSanPham(dau, cuoi, 5);
        inBang("Top 5 sản phẩm bán chạy (theo số lượng)", "Mã SP", "Tên", top, dtNay);
        inBang("Theo loại sản phẩm", "Loại", "", thongKeTheoLoai(dau, cuoi));
        inBang("Theo nhân viên", "Mã NV", "Tên", thongKeTheoNhanVien(dau, cuoi));
        inBang("Theo phương thức thanh toán", "Mã", "Phương thức", thongKeTheoPhuongThuc(dau, cuoi));
    }

    @Override
    public void inBaoCaoNam(int nam) {
        System.out.println("========== BÁO CÁO DOANH THU NĂM " + nam + " ==========");
        inBaoCaoTongHop(LocalDate.of(nam, 1, 1), LocalDate.of(nam, 12, 31));
        inBang("Doanh thu từng tháng", "Tháng", "", thongKeTheoThang(nam));
    }

    public void inThongKeSanPham(LocalDate tu, LocalDate den) {
        inBang("Doanh thu theo sản phẩm " + tu + " -> " + den, "Mã SP", "Tên", thongKeTheoSanPham(tu, den));
    }

    public void inThongKeNhanVien(LocalDate tu, LocalDate den) {
        inBang("Doanh thu theo nhân viên " + tu + " -> " + den, "Mã NV", "Tên", thongKeTheoNhanVien(tu, den));
    }

    // ===================== Đọc / ghi file =====================

    /**
     * Đọc danh sách từ file (thay thế dữ liệu hiện tại). Dòng trống hoặc bắt đầu bằng '#' bị bỏ qua,
     * dòng sai định dạng bị báo lỗi ra System.err và bỏ qua.
     * @return số hoá đơn đọc được (file không tồn tại -> 0, giữ nguyên danh sách)
     */
    @Override
    public int docFile(String duongDan) throws IOException {
        Path path = Paths.get(duongDan);
        if (!Files.exists(path)) {
            return 0;
        }
        ds = new HoaDon[10];
        n = 0;
        int soDong = 0;
        for (String dong : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            soDong++;
            dong = dong.replace("\uFEFF", "").trim();
            if (dong.isEmpty() || dong.startsWith("#")) {
                continue;
            }
            try {
                if (!them(HoaDon.fromFileString(dong))) {
                    System.err.println("Dòng " + soDong + ": trùng mã hoá đơn, bỏ qua");
                }
            } catch (RuntimeException e) {
                System.err.println("Dòng " + soDong + " sai định dạng (" + e.getMessage() + "), bỏ qua");
            }
        }
        return n;
    }

    @Override
    public void ghiFile(String duongDan) throws IOException {
        Path path = Paths.get(duongDan);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        List<String> dong = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            dong.add(ds[i].toFileString());
        }
        Files.write(path, dong, StandardCharsets.UTF_8);
    }
}