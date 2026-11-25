package service;

import iface.IFile;
import iface.IQuanLy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import model.CongThuc;
import model.NguyenLieu;
import model.NhaCungCap;
import model.PhieuNhapKho;
import model.PhieuXuatKho;
import util.FileHelper;

public class DanhSachNguyenLieu implements IQuanLy<NguyenLieu>, IFile {
    private List<NguyenLieu> dsNguyenLieu;
    private List<NhaCungCap> dsNhaCungCap;
    private List<PhieuNhapKho> dsPhieuNhap;
    private List<PhieuXuatKho> dsPhieuXuat;

    public DanhSachNguyenLieu() {
        this.dsNguyenLieu = new ArrayList<>();
        this.dsNhaCungCap = new ArrayList<>();
        this.dsPhieuNhap = new ArrayList<>();
        this.dsPhieuXuat = new ArrayList<>();
    }

    // ===== IQuanLy<NguyenLieu>: để CongThuc / SanPham tra tồn kho qua interface =====
    @Override
    public boolean them(NguyenLieu nl) {
        return themNguyenLieu(nl);
    }

    @Override
    public boolean xoa(String ma) {
        return xoaNguyenLieu(ma);
    }

    @Override
    public void hienThi() {
        hienThiDanhSach();
    }

    @Override
    public int soLuong() {
        return dsNguyenLieu.size();
    }

    public List<PhieuXuatKho> getDsPhieuXuat() {
        return dsPhieuXuat;
    }

    public List<NguyenLieu> getDsNguyenLieu() {
        return dsNguyenLieu;
    }

    public List<NhaCungCap> getDsNhaCungCap() {
        return dsNhaCungCap;
    }

    public List<PhieuNhapKho> getDsPhieuNhap() {
        return dsPhieuNhap;
    }

    public boolean themNguyenLieu(NguyenLieu nl) {
        if (nl == null || timTheoMa(nl.getMaNL()) != null) {
            return false;
        }
        dsNguyenLieu.add(nl);
        return true;
    }

    @Override
    public NguyenLieu timTheoMa(String maNL) {
        if (maNL == null) {
            return null;
        }
        maNL = maNL.trim();
        for (NguyenLieu nl : dsNguyenLieu) {
            if (nl.getMaNL().equalsIgnoreCase(maNL)) {
                return nl;
            }
        }
        return null;
    }

    public List<NguyenLieu> timTheoTen(String tuKhoa) {
        List<NguyenLieu> ketQua = new ArrayList<>();
        String lower = tuKhoa.toLowerCase();
        for (NguyenLieu nl : dsNguyenLieu) {
            if (nl.getTenNL().toLowerCase().contains(lower)) {
                ketQua.add(nl);
            }
        }
        return ketQua;
    }

    public boolean xoaNguyenLieu(String maNL) {
        NguyenLieu nl = timTheoMa(maNL);
        return nl != null && dsNguyenLieu.remove(nl);
    }

    public boolean capNhatNguyenLieu(String maNL, String tenMoi, String donViMoi,
                                     double mucToiThieuMoi, double donGiaMoi, String maNCCMoi) {
        NguyenLieu nl = timTheoMa(maNL);
        if (nl == null) {
            return false;
        }
        nl.setTenNL(tenMoi);
        nl.setDonViTinh(donViMoi);
        nl.setMucToiThieu(mucToiThieuMoi);
        nl.setDonGiaNhap(donGiaMoi);
        nl.setMaNCC(maNCCMoi);
        return true;
    }

    public void hienThiDanhSach() {
        if (dsNguyenLieu.isEmpty()) {
            System.out.println("Danh sach nguyen lieu trong!");
            return;
        }
        inTieuDeBangNguyenLieu();
        for (NguyenLieu nl : dsNguyenLieu) {
            nl.xuat();
        }
        inDuongKeBangNguyenLieu();
    }

    private void inTieuDeBangNguyenLieu() {
        inDuongKeBangNguyenLieu();
        System.out.printf("| %-6s | %-22s | %-8s | %10s | %10s | %12s | %-8s | %-13s | %-5s |%n",
                "Ma NL", "Ten Nguyen Lieu", "Don Vi", "SL Ton", "Toi Thieu", "Don Gia", "Ma NCC", "Trang Thai", "Khoa");
        inDuongKeBangNguyenLieu();
    }

    private void inDuongKeBangNguyenLieu() {
        System.out.println("+--------+------------------------+----------+------------+------------+--------------+----------+---------------+-------+");
    }

    public boolean themNhaCungCap(NhaCungCap ncc) {
        if (ncc == null || timNhaCungCap(ncc.getMaNCC()) != null) {
            return false;
        }
        dsNhaCungCap.add(ncc);
        return true;
    }

    public NhaCungCap timNhaCungCap(String maNCC) {
        for (NhaCungCap ncc : dsNhaCungCap) {
            if (ncc.getMaNCC().equalsIgnoreCase(maNCC)) {
                return ncc;
            }
        }
        return null;
    }

    public void hienThiNhaCungCap() {
        if (dsNhaCungCap.isEmpty()) {
            System.out.println("Chua co du lieu nha cung cap.");
            return;
        }
        System.out.println("+----------+---------------------------+--------------+--------------------------------+");
        System.out.printf("| %-8s | %-25s | %-12s | %-30s |%n", "Ma NCC", "Ten Nha Cung Cap", "SDT", "Dia Chi");
        System.out.println("+----------+---------------------------+--------------+--------------------------------+");
        for (NhaCungCap ncc : dsNhaCungCap) {
            ncc.xuat();
        }
        System.out.println("+----------+---------------------------+--------------+--------------------------------+");
    }

    public boolean lapPhieuNhapKho(PhieuNhapKho phieu) {
        if (phieu == null || phieu.getSoLuongNhap() <= 0) {
            return false;
        }
        NguyenLieu nl = timTheoMa(phieu.getMaNL());
        if (nl == null) {
            System.out.println("Khong tim thay ma nguyen lieu: " + phieu.getMaNL());
            return false;
        }
        nl.nhapThem(phieu.getSoLuongNhap());
        if (phieu.getDonGiaNhap() > 0) {
            nl.setDonGiaNhap(phieu.getDonGiaNhap());
        }
        dsPhieuNhap.add(phieu);
        return true;
    }

    public boolean xuatKhoNguyenLieu(String maNL, double soLuong) {
        NguyenLieu nl = timTheoMa(maNL);
        return nl != null && nl.xuatKho(soLuong);
    }

    // ===================== Nhập kho nhanh (mã phiếu + ngày tự động) =====================

    // mã phiếu nhập mới dạng PN01, PN02... (lớn nhất hiện có + 1)
    public String maPhieuNhapMoi() {
        int max = 0;
        for (PhieuNhapKho pn : dsPhieuNhap) {
            max = Math.max(max, soTrongMa(pn.getMaPhieu(), "PN"));
        }
        return String.format("PN%02d", max + 1);
    }

    /**
     * Lập phiếu nhập cho 1 nguyên liệu đã có trong kho. donGia <= 0 = giữ đơn giá cũ, maNCC rỗng = nhà cung cấp mặc định.
     * Dữ liệu sai thì ném IllegalArgumentException và k đổi gì.
     */
    public PhieuNhapKho nhapKho(String maNV, String maNL, double soLuong, double donGia, String maNCC) {
        NguyenLieu nl = timTheoMa(maNL);
        if (nl == null) {
            throw new IllegalArgumentException("Không tìm thấy nguyên liệu " + maNL);
        }
        if (!Double.isFinite(soLuong) || soLuong <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải là số dương");
        }
        String ncc = (maNCC == null || maNCC.trim().isEmpty()) ? nl.getMaNCC() : maNCC.trim();
        if (!dsNhaCungCap.isEmpty() && timNhaCungCap(ncc) == null) {
            throw new IllegalArgumentException("Không tìm thấy nhà cung cấp " + ncc);
        }
        double gia = donGia > 0 ? donGia : nl.getDonGiaNhap();
        PhieuNhapKho pn = new PhieuNhapKho(maPhieuNhapMoi(),
                LocalDate.now().format(util.CauHinh.DINH_DANG_NGAY),
                maNV == null ? "" : maNV.trim().toUpperCase(), ncc, nl.getMaNL(), soLuong, gia);
        if (!lapPhieuNhapKho(pn)) {
            throw new IllegalArgumentException("Không lập được phiếu nhập");
        }
        return pn;
    }

    // ===================== Xuất kho bằng phiếu xuất =====================

    // mã phiếu xuất mới dạng PX01, PX02... tự sinh để lưu lại
    public String maPhieuXuatMoi() {
        int max = 0;
        for (PhieuXuatKho px : dsPhieuXuat) {
            max = Math.max(max, soTrongMa(px.getMaPhieu(), "PX"));
        }
        return String.format("PX%02d", max + 1);
    }

    /**
     * Lập phiếu xuất nhiều dòng (maNL -> số lượng) một lần: kiểm tra TẤT CẢ dòng trước, đủ hết mới trừ kho.
     * Sai (thiếu lý do, mã lạ, k đủ tồn...) thì ném IllegalArgumentException và k trừ gì cả.
     */
    public PhieuXuatKho lapPhieuXuat(String maNV, String noiDung, Map<String, Double> dong) {
        if (dong == null || dong.isEmpty()) {
            throw new IllegalArgumentException("Phiếu xuất phải có ít nhất 1 nguyên liệu");
        }
        PhieuXuatKho px = new PhieuXuatKho(maPhieuXuatMoi(), LocalDateTime.now(), maNV, noiDung);
        for (Map.Entry<String, Double> e : dong.entrySet()) {
            px.themDong(e.getKey(), e.getValue());
        }
        for (Map.Entry<String, Double> e : px.getChiTiet().entrySet()) {
            NguyenLieu nl = timTheoMa(e.getKey());
            if (nl == null) {
                throw new IllegalArgumentException("Không tìm thấy nguyên liệu " + e.getKey());
            }
            if (e.getValue() > nl.getSoLuongTon() + 1e-9) {
                throw new IllegalArgumentException(String.format(Locale.US,
                        "%s k đủ tồn kho (cần xuất %.2f, còn %.2f %s)",
                        nl.getTenNL(), e.getValue(), nl.getSoLuongTon(), nl.getDonViTinh()));
            }
        }
        for (Map.Entry<String, Double> e : px.getChiTiet().entrySet()) {
            NguyenLieu nl = timTheoMa(e.getKey());
            nl.setSoLuongTon(nl.getSoLuongTon() - e.getValue());
        }
        dsPhieuXuat.add(px);
        return px;
    }

    // ===================== Trừ / hoàn kho khi bán hàng =====================

    /**
     * Trừ nguyên liệu cho heSo phần theo công thức. Kiểm tra tồn kho hiện tại TRƯỚC: thiếu bất kỳ nguyên liệu nào
     * thì ném IllegalArgumentException (kèm danh sách thiếu) và k trừ gì cả. Công thức rỗng thì k làm gì.
     */
    public void truKhoTheoCongThuc(CongThuc congThuc, int heSo) {
        CongThuc can = nhanCongThuc(congThuc, heSo);
        if (can.isRong()) {
            return;
        }
        List<String> thieu = can.nguyenLieuKhongDu(this);
        if (!thieu.isEmpty()) {
            throw new IllegalArgumentException("K đủ nguyên liệu: " + String.join("; ", thieu));
        }
        for (Map.Entry<String, Double> e : can.getThanhPhan().entrySet()) {
            NguyenLieu nl = timTheoMa(e.getKey());
            nl.setSoLuongTon(lamTron(nl.getSoLuongTon() - e.getValue()));
        }
    }

    // cộng trả nguyên liệu cho heSo phần (khi bớt / xoá món, huỷ hoá đơn)
    public void hoanKhoTheoCongThuc(CongThuc congThuc, int heSo) {
        CongThuc tra = nhanCongThuc(congThuc, heSo);
        for (Map.Entry<String, Double> e : tra.getThanhPhan().entrySet()) {
            NguyenLieu nl = timTheoMa(e.getKey());
            if (nl != null) {
                nl.setSoLuongTon(lamTron(nl.getSoLuongTon() + e.getValue()));
            }
        }
    }

    private static CongThuc nhanCongThuc(CongThuc congThuc, int heSo) {
        CongThuc kq = new CongThuc();
        if (congThuc != null && heSo > 0) {
            kq.congThem(congThuc, heSo);
        }
        return kq;
    }

    // tránh sai số số thực khi cộng trừ nhiều lần (14.95 - 0.03 = 14.919999...)
    private static double lamTron(double so) {
        return Math.round(so * 10000) / 10000.0;
    }

    public PhieuXuatKho timPhieuXuat(String maPhieu) {
        for (PhieuXuatKho px : dsPhieuXuat) {
            if (px.getMaPhieu().equalsIgnoreCase(maPhieu == null ? "" : maPhieu.trim())) {
                return px;
            }
        }
        return null;
    }

    public void hienThiPhieuXuatKho() {
        if (dsPhieuXuat.isEmpty()) {
            System.out.println("Chua co phieu xuat kho nao.");
            return;
        }
        System.out.println("Ma     | Thoi gian        | Ma NV  | Noi dung                       | So dong");
        for (PhieuXuatKho px : dsPhieuXuat) {
            System.out.println(px);
        }
    }

    // ===================== Khoá / mở khoá nguyên liệu (khoá các món có nguyên liệu đó) =====================

    // false nếu k có mã nguyên liệu
    public boolean datKhoa(String maNL, boolean khoa) {
        NguyenLieu nl = timTheoMa(maNL);
        if (nl == null) {
            return false;
        }
        nl.setKhoa(khoa);
        return true;
    }

    public List<NguyenLieu> layDanhSachDangKhoa() {
        List<NguyenLieu> kq = new ArrayList<>();
        for (NguyenLieu nl : dsNguyenLieu) {
            if (nl.isKhoa()) {
                kq.add(nl);
            }
        }
        return kq;
    }

    // lấy phần số của mã: "PN07" -> 7, k đúng dạng -> 0
    private static int soTrongMa(String ma, String tienTo) {
        if (ma == null || !ma.toUpperCase().startsWith(tienTo)) {
            return 0;
        }
        try {
            return Integer.parseInt(ma.substring(tienTo.length()).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void hienThiPhieuNhapKho() {
        if (dsPhieuNhap.isEmpty()) {
            System.out.println("Chua co phieu nhap kho nao.");
            return;
        }
        System.out.println("+----------+--------------+----------+----------+----------+------------+--------------+----------------+");
        System.out.printf("| %-8s | %-12s | %-8s | %-8s | %-8s | %10s | %12s | %14s |%n",
                "Ma Phieu", "Ngay Nhap", "Ma NV", "Ma NCC", "Ma NL", "SL Nhap", "Don Gia", "Thanh Tien");
        System.out.println("+----------+--------------+----------+----------+----------+------------+--------------+----------------+");
        for (PhieuNhapKho pn : dsPhieuNhap) {
            pn.xuat();
        }
        System.out.println("+----------+--------------+----------+----------+----------+------------+--------------+----------------+");
    }
    public List<NguyenLieu> layDanhSachCanhBaoHetHang() {
        List<NguyenLieu> dsCanhBao = new ArrayList<>();
        for (NguyenLieu nl : dsNguyenLieu) {
            if (nl.isCanhBaoHetHang()) {
                dsCanhBao.add(nl);
            }
        }
        return dsCanhBao;
    }

    public void canhBaoHetHang() {
        List<NguyenLieu> dsCanhBao = layDanhSachCanhBaoHetHang();
        System.out.println("\n================ CANH BAO TON KHO / HET HANG ================");
        if (dsCanhBao.isEmpty()) {
            System.out.println("Tat ca nguyen lieu deu o muc an toan (du ton kho).");
            return;
        }
        System.out.println("Phat hien " + dsCanhBao.size() + " nguyen lieu can nhap them:");
        inTieuDeBangNguyenLieu();
        for (NguyenLieu nl : dsCanhBao) {
            nl.xuat();
        }
        inDuongKeBangNguyenLieu();
        for (NguyenLieu nl : dsCanhBao) {
            double canNhap = Math.max(0, nl.getMucToiThieu() * 2 - nl.getSoLuongTon());
            System.out.printf(" -> [%s] %s (%s): Ton %.2f %s (Toi thieu: %.2f %s) | Goi y nhap: %.2f %s (NCC: %s)%n",
                    nl.getTrangThaiTonKho(), nl.getTenNL(), nl.getMaNL(),
                    nl.getSoLuongTon(), nl.getDonViTinh(),
                    nl.getMucToiThieu(), nl.getDonViTinh(),
                    canNhap, nl.getDonViTinh(), nl.getMaNCC());
        }
    }

    public void baoCaoTongHop() {
        int tongSoLoai = dsNguyenLieu.size();
        int soHetHang = 0, soSapHet = 0, soConHang = 0;
        double tongGiaTriTonKho = 0;

        for (NguyenLieu nl : dsNguyenLieu) {
            tongGiaTriTonKho += nl.tinhGiaTriTonKho();
            if (nl.isHetHang()) {
                soHetHang++;
            } else if (nl.isCanhBaoHetHang()) {
                soSapHet++;
            } else {
                soConHang++;
            }
        }

        double tongChiPhiNhap = 0;
        for (PhieuNhapKho pn : dsPhieuNhap) {
            tongChiPhiNhap += pn.tinhThanhTien();
        }

        System.out.println("\n==================== BAO CAO TONG HOP KHO ====================");
        System.out.printf("- Tong so loai nguyen lieu      : %d loai%n", tongSoLoai);
        System.out.printf("- Nguyen lieu du ton (An toan)  : %d loai%n", soConHang);
        System.out.printf("- Nguyen lieu sap het hang      : %d loai%n", soSapHet);
        System.out.printf("- Nguyen lieu da het hang       : %d loai%n", soHetHang);
        System.out.printf("- Tong gia tri ton kho hien tai : %,.0f VND%n", tongGiaTriTonKho);
        System.out.printf("- Tong so nha cung cap          : %d NCC%n", dsNhaCungCap.size());
        System.out.printf("- Tong so phieu nhap kho        : %d phieu%n", dsPhieuNhap.size());
        System.out.printf("- Tong chi phi nhap hang        : %,.0f VND%n", tongChiPhiNhap);
        System.out.printf("- Tong so phieu xuat kho        : %d phieu%n", dsPhieuXuat.size());
        System.out.println("--------------------------------------------------------------");
        System.out.println("+--------+------------------------+------------+--------------+----------------+---------------+");
        System.out.printf("| %-6s | %-22s | %10s | %12s | %14s | %-13s |%n",
                "Ma NL", "Ten Nguyen Lieu", "SL Ton", "Don Gia", "Gia Tri Ton", "Trang Thai");
        System.out.println("+--------+------------------------+------------+--------------+----------------+---------------+");
        for (NguyenLieu nl : dsNguyenLieu) {
            System.out.printf("| %-6s | %-22s | %10.2f | %12.0f | %14.0f | %-13s |%n",
                    nl.getMaNL(), nl.getTenNL(), nl.getSoLuongTon(),
                    nl.getDonGiaNhap(), nl.tinhGiaTriTonKho(), nl.getTrangThaiTonKho());
        }
        System.out.println("+--------+------------------------+------------+--------------+----------------+---------------+");
    }
    @Override
    public int docFile(String tenFile) {
        List<String> lines = FileHelper.docFile(tenFile);
        dsNguyenLieu.clear();
        dsNhaCungCap.clear();
        dsPhieuNhap.clear();
        dsPhieuXuat.clear();

        for (String line : lines) {
            if (line.startsWith("NCC|")) {
                NhaCungCap ncc = NhaCungCap.fromFileString(line.substring(4));
                if (ncc != null) dsNhaCungCap.add(ncc);
            } else if (line.startsWith("PN|")) {
                PhieuNhapKho pn = PhieuNhapKho.fromFileString(line.substring(3));
                if (pn != null) dsPhieuNhap.add(pn);
            } else if (line.startsWith("PX|")) {
                PhieuXuatKho px = PhieuXuatKho.fromFileString(line.substring(3));
                if (px != null) dsPhieuXuat.add(px);
            } else if (line.startsWith("NL|")) {
                NguyenLieu nl = NguyenLieu.fromFileString(line.substring(3));
                if (nl != null) dsNguyenLieu.add(nl);
            } else {
                NguyenLieu nl = NguyenLieu.fromFileString(line);
                if (nl != null) dsNguyenLieu.add(nl);
            }
        }
        return dsNguyenLieu.size();
    }

    @Override
    public void ghiFile(String tenFile) {
        List<String> lines = new ArrayList<>();
        lines.add("# DANH SACH NGUYEN LIEU (maNL;tenNL;donViTinh;soLuongTon;mucToiThieu;donGiaNhap;maNCC;khoa)");
        for (NguyenLieu nl : dsNguyenLieu) {
            lines.add(nl.toFileString());
        }
        if (!dsNhaCungCap.isEmpty()) {
            lines.add("# DANH SACH NHA CUNG CAP (NCC|maNCC;tenNCC;soDienThoai;diaChi)");
            for (NhaCungCap ncc : dsNhaCungCap) {
                lines.add("NCC|" + ncc.toFileString());
            }
        }
        if (!dsPhieuNhap.isEmpty()) {
            lines.add("# DANH SACH PHIEU NHAP KHO (PN|maPhieu;ngayNhap;maNV;maNCC;maNL;soLuongNhap;donGiaNhap)");
            for (PhieuNhapKho pn : dsPhieuNhap) {
                lines.add("PN|" + pn.toFileString());
            }
        }
        if (!dsPhieuXuat.isEmpty()) {
            lines.add("# DANH SACH PHIEU XUAT KHO (PX|maPhieu;thoiGian;maNV;noiDung;maNL:soLuong,maNL:soLuong)");
            for (PhieuXuatKho px : dsPhieuXuat) {
                lines.add("PX|" + px.toFileString());
            }
        }
        FileHelper.ghiFile(tenFile, lines);
    }

    public void menuKho(Scanner sc, String duongDanFile) {
        int chon = -1;
        do {
            System.out.println("\n================ QUAN LY KHO & NGUYEN LIEU ================");
            System.out.println("1. Hien thi danh sach nguyen lieu");
            System.out.println("2. Them nguyen lieu moi");
            System.out.println("3. Tim kiem nguyen lieu theo ma / ten");
            System.out.println("4. Lap phieu nhap kho");
            System.out.println("5. Xuat kho nguyen lieu");
            System.out.println("6. Quan ly nha cung cap & lich su phieu nhap");
            System.out.println("7. Canh bao het hang / sap het hang");
            System.out.println("8. Bao cao tong hop kho");
            System.out.println("9. Luu du lieu kho ra file");
            System.out.println("0. Quay lai");
            System.out.print("Chon chuc nang: ");
            try {
                chon = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                chon = -1;
            }
            switch (chon) {
                case 1: hienThiDanhSach(); break;
                case 2:
                    NguyenLieu nlMoi = new NguyenLieu();
                    nlMoi.nhap(sc);
                    System.out.println(themNguyenLieu(nlMoi) ? "Them thanh cong!" : "Ma NL da ton tai!");
                    break;
                case 3:
                    System.out.print("Nhap ma hoac ten can tim: ");
                    String tk = sc.nextLine().trim();
                    NguyenLieu theoMa = timTheoMa(tk);
                    if (theoMa != null) {
                        inTieuDeBangNguyenLieu(); theoMa.xuat(); inDuongKeBangNguyenLieu();
                    } else {
                        List<NguyenLieu> kq = timTheoTen(tk);
                        if (kq.isEmpty()) System.out.println("Khong tim thay!");
                        else { inTieuDeBangNguyenLieu(); for (NguyenLieu x : kq) x.xuat(); inDuongKeBangNguyenLieu(); }
                    }
                    break;
                case 4:
                    PhieuNhapKho pn = new PhieuNhapKho();
                    pn.nhap(sc);
                    if (lapPhieuNhapKho(pn)) System.out.printf(Locale.US, "Nhap kho thanh cong! Thanh tien: %.0f VND%n", pn.tinhThanhTien());
                    break;
                case 5:
                    System.out.print("Nhap ma NL can xuat: "); String maXuat = sc.nextLine().trim();
                    System.out.print("Nhap so luong xuat: "); double slXuat = Double.parseDouble(sc.nextLine().trim());
                    System.out.println(xuatKhoNguyenLieu(maXuat, slXuat) ? "Xuat kho thanh cong!" : "Xuat kho that bai!");
                    break;
                case 6: hienThiNhaCungCap(); hienThiPhieuNhapKho(); break;
                case 7: canhBaoHetHang(); break;
                case 8: baoCaoTongHop(); break;
                case 9: ghiFile(duongDanFile); System.out.println("Da luu file: " + duongDanFile); break;
                case 0: break;
                default: System.out.println("Lua chon khong hop le!");
            }
        } while (chon != 0);
    }
}

