package service;

import iface.IFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import model.NguyenLieu;
import model.NhaCungCap;
import model.PhieuNhapKho;
import util.FileHelper;

public class DanhSachNguyenLieu implements IFile {
    private List<NguyenLieu> dsNguyenLieu;
    private List<NhaCungCap> dsNhaCungCap;
    private List<PhieuNhapKho> dsPhieuNhap;

    public DanhSachNguyenLieu() {
        this.dsNguyenLieu = new ArrayList<>();
        this.dsNhaCungCap = new ArrayList<>();
        this.dsPhieuNhap = new ArrayList<>();
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

    private static int laySo(String ma, String tienTo) {
        if (ma == null || !ma.toUpperCase().startsWith(tienTo)) {
            return 0;
        }
        try {
            return Integer.parseInt(ma.substring(tienTo.length()).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String taoMaNL() {
        int max = 0;
        for (NguyenLieu nl : dsNguyenLieu) {
            max = Math.max(max, laySo(nl.getMaNL(), "NL"));
        }
        return String.format("NL%02d", max + 1);
    }

    public String taoMaPhieu() {
        int max = 0;
        for (PhieuNhapKho pn : dsPhieuNhap) {
            max = Math.max(max, laySo(pn.getMaPhieu(), "PN"));
        }
        return String.format("PN%02d", max + 1);
    }

    public boolean themNguyenLieu(NguyenLieu nl) {
        if (nl == null || timTheoMa(nl.getMaNL()) != null) {
            return false;
        }
        dsNguyenLieu.add(nl);
        return true;
    }

    public NguyenLieu timTheoMa(String maNL) {
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
        System.out.printf("| %-6s | %-22s | %-8s | %10s | %10s | %12s | %-8s | %-13s |%n",
                "Ma NL", "Ten Nguyen Lieu", "Don Vi", "SL Ton", "Toi Thieu", "Don Gia", "Ma NCC", "Trang Thai");
        inDuongKeBangNguyenLieu();
    }

    private void inDuongKeBangNguyenLieu() {
        System.out.println("+--------+------------------------+----------+------------+------------+--------------+----------+---------------+");
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

        for (String line : lines) {
            if (line.startsWith("NCC|")) {
                NhaCungCap ncc = NhaCungCap.fromFileString(line.substring(4));
                if (ncc != null) dsNhaCungCap.add(ncc);
            } else if (line.startsWith("PN|")) {
                PhieuNhapKho pn = PhieuNhapKho.fromFileString(line.substring(3));
                if (pn != null) dsPhieuNhap.add(pn);
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
        lines.add("# DANH SACH NGUYEN LIEU (maNL;tenNL;donViTinh;soLuongTon;mucToiThieu;donGiaNhap;maNCC)");
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
                    nlMoi.setMaNL(taoMaNL());
                    System.out.println("Ma nguyen lieu tu dong: " + nlMoi.getMaNL());
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
                    pn.setMaPhieu(taoMaPhieu());
                    System.out.println("Ma phieu nhap tu dong: " + pn.getMaPhieu());
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

