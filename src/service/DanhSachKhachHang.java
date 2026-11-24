package service;

import iface.IFile;
import iface.IGiamGia;
import iface.IQuanLy;
import model.KhachHang;
import model.KhachHangThanThiet;
import model.KhachHangThanThiet.Hang;
import util.CauHinh;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Định dạng mỗi dòng trong data/khachhang.txt (ngăn cách bằng '|'):
 *   LOAI|maKH|hoTen|sdt|diemTichLuy|tongChiTieu
 *   LOAI: THUONG hoặc THANTHIET (hạng tự tính theo tổng chi tiêu)
 */
public class DanhSachKhachHang implements IQuanLy<KhachHang>, IFile {

    // khách thường chi tiêu từ mức này trở lên thì tự nâng lên thân thiết
    public static final double NGUONG_THAN_THIET = 500000;

    private KhachHang[] ds;
    private int n;

    // ===================== Constructor =====================
    public DanhSachKhachHang() {
        this(10);
    }

    public DanhSachKhachHang(int sucChua) {
        ds = new KhachHang[Math.max(sucChua, 1)];
        n = 0;
    }

    // ===================== TOOLS =====================
    // mảng đầy thì nhân đôi sức chứa
    private void moRong() {
        if (n == ds.length) {
            ds = Arrays.copyOf(ds, ds.length * 2);
        }
    }

    // trả về vị trí trong mảng, k thấy thì -1
    private int timViTri(String ma) {
        if (ma == null) {
            return -1;
        }
        for (int i = 0; i < n; i++) {
            if (ds[i].getMaKH().equalsIgnoreCase(ma.trim())) {
                return i;
            }
        }
        return -1;
    }

    // "Nguyễn Đình" -> "nguyen dinh" (dùng cho tìm gần đúng)
    public static String boDau(String s) {
        if (s == null) {
            return "";
        }
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return t.replace('đ', 'd').replace('Đ', 'D').toLowerCase().trim();
    }

    // cắt mảng tạm cho vừa số phần tử tìm được
    private static KhachHang[] catMang(KhachHang[] tam, int dem) {
        return Arrays.copyOf(tam, dem);
    }

    public int soLuong() {
        return n;
    }

    // lấy khách theo vị trí, sai vị trí thì NULL
    public KhachHang get(int i) {
        return (i >= 0 && i < n) ? ds[i] : null;
    }

    // ===================== A. Nhập n phần tử đầu tiên =====================
    // nhập n khách từ bàn phím: từng mục phải nhập ĐÚNG mới được qua mục tiếp theo, sai thì báo lỗi và hỏi lại đúng mục đó
    public void nhapN(Scanner sc) {
        try {
            int soLuongNhap = docSoLuong(sc);
            for (int i = 1; i <= soLuongNhap; i++) {
                System.out.println("--- Khách hàng thứ " + i + " ---");
                KhachHang kh = nhapMotKhach(sc);
                if (kh == null) {
                    return;   // hết dữ liệu nhập
                }
                them(kh);
                System.out.println("Đã thêm " + kh.getMaKH() + " - " + kh.getLoai());
            }
        } catch (NoSuchElementException | IllegalStateException e) {
            // hết dữ liệu nhập khi đang hỏi số lượng thì dừng, k để chương trình bị văng
            System.out.println("Hết dữ liệu nhập, dừng nhập khách hàng.");
        }
    }

    // nhập 1 khách (dùng chung cho "nhập n khách" và "đăng ký nhanh"): hỏi loại -> họ tên -> sđt, mục nào sai thì hỏi lại mục đó
    // trả về khách hợp lệ CHƯA thêm vào danh sách (nơi gọi tự gọi them), trả về null nếu hết dữ liệu nhập
    public KhachHang nhapMotKhach(Scanner sc) {
        try {
            boolean thanThiet = docLoai(sc);
            String ten = docHoTen(sc);
            String sdt = docSDT(sc);
            // đã kiểm tra hết từng mục ở trên rồi mới tạo đối tượng, nên k bao giờ lỗi và mã tự sinh k bị nhảy số
            return thanThiet ? new KhachHangThanThiet(ten, sdt) : new KhachHang(ten, sdt);
        } catch (NoSuchElementException | IllegalStateException e) {
            System.out.println("Hết dữ liệu nhập, dừng nhập khách hàng.");
            return null;
        }
    }

    // số lượng khách cần nhập: số nguyên từ 1 đến 100 (chữ, số âm, số 0, số thập phân đều bị từ chối)
    private static int docSoLuong(Scanner sc) {
        while (true) {
            System.out.print("Nhập số khách hàng cần thêm (1 - 100): ");
            String s = sc.nextLine().trim();
            if (s.matches("\\d{1,3}")) {
                int so = Integer.parseInt(s);
                if (so >= 1 && so <= 100) {
                    return so;
                }
            }
            System.out.println("Bạn đã điền sai: phải nhập số nguyên từ 1 đến 100, nhập lại!");
        }
    }

    // loại khách: chỉ nhận đúng 1 (thường) hoặc 2 (thân thiết). true = thân thiết
    private static boolean docLoai(Scanner sc) {
        while (true) {
            System.out.print("Loại khách (1 = thường, 2 = thân thiết): ");
            String s = sc.nextLine().trim();
            if (s.equals("1")) {
                return false;
            }
            if (s.equals("2")) {
                return true;
            }
            System.out.println("Bạn đã điền sai: chỉ được nhập 1 hoặc 2, nhập lại!");
        }
    }

    // họ tên: 2 - 50 ký tự, chỉ gồm chữ cái (có dấu), khoảng trắng, dấu chấm, nháy, gạch nối; k có số, k có '|'
    private static boolean hopLeHoTen(String ten) {
        return ten.length() >= 2 && ten.length() <= 50 && ten.matches("[\\p{L}\\p{M} .'-]+") && ten.matches(".*\\p{L}.*");
    }

    private static String docHoTen(Scanner sc) {
        while (true) {
            System.out.print("Họ tên: ");
            String ten = sc.nextLine().trim().replaceAll("\\s+", " ");
            if (hopLeHoTen(ten)) {
                return ten;
            }
            System.out.println("Bạn đã điền sai: họ tên 2 - 50 ký tự, chỉ gồm chữ cái (k số, k ký tự đặc biệt), nhập lại!");
        }
    }

    // số điện thoại: đúng 10 số, bắt đầu bằng 0, k trùng khách đã có (kể cả khách vừa nhập ở các lượt trước)
    private String docSDT(Scanner sc) {
        while (true) {
            System.out.print("Số điện thoại: ");
            String sdt = sc.nextLine().trim();
            if (!KhachHang.hopLeSDT(sdt)) {
                System.out.println("Bạn đã điền sai: số điện thoại phải gồm đúng 10 chữ số, bắt đầu bằng 0, nhập lại!");
            } else if (timTheoSDT(sdt) != null) {
                System.out.println("Bạn đã điền sai: số điện thoại này đã có khách khác dùng, nhập lại!");
            } else {
                return sdt;
            }
        }
    }

    // ===================== B. Xuất danh sách =====================
    @Override
    public void hienThi() {
        System.out.println(KhachHang.tieuDeBang());
        for (int i = 0; i < n; i++) {
            System.out.println(ds[i]);
        }
        System.out.println("Tổng: " + n + " khách hàng");
    }

    public static void inMang(KhachHang[] mang) {
        System.out.println(KhachHang.tieuDeBang());
        for (KhachHang kh : mang) {
            System.out.println(kh);
        }
        System.out.println("Tìm thấy: " + mang.length + " khách hàng");
    }

    // ===================== C. Thêm 1 phần tử, k phần tử =====================
    @Override
    public boolean them(KhachHang kh) {
        if (kh == null || timViTri(kh.getMaKH()) != -1 || timTheoSDT(kh.getSoDienThoai()) != null) {
            return false;
        }
        moRong();
        ds[n++] = kh;
        return true;
    }

    // thêm k khách cùng lúc, trả về số khách thêm thành công
    public int themK(KhachHang[] mang) {
        int dem = 0;
        for (KhachHang kh : mang) {
            if (them(kh)) {
                dem++;
            }
        }
        return dem;
    }

    // đăng ký nhanh khách mới (mã tự sinh), menu đã đọc sẵn tên / sđt / loại rồi mới gọi hàm này
    // hàm tự kiểm tra: tên hoặc sđt sai (hoặc sđt trùng) thì báo lỗi và hỏi lại ĐÚNG mục đó cho tới khi nhập đúng mới cho qua
    // lưu ý: loại khách (thanThiet) do menu đọc nên hàm k kiểm tra được
    public KhachHang dangKy(String hoTen, String sdt, boolean thanThiet) {
        Scanner sc = CauHinh.SCANNER;
        try {
            String ten = hoTen == null ? "" : hoTen.trim().replaceAll("\\s+", " ");
            if (!hopLeHoTen(ten)) {
                System.out.println("Bạn đã điền sai: họ tên 2 - 50 ký tự, chỉ gồm chữ cái (k số, k ký tự đặc biệt), nhập lại!");
                ten = docHoTen(sc);
            }
            String soDT = sdt == null ? "" : sdt.trim();
            if (!KhachHang.hopLeSDT(soDT)) {
                System.out.println("Bạn đã điền sai: số điện thoại phải gồm đúng 10 chữ số, bắt đầu bằng 0, nhập lại!");
                soDT = docSDT(sc);
            } else if (timTheoSDT(soDT) != null) {
                System.out.println("Bạn đã điền sai: số điện thoại này đã có khách khác dùng, nhập lại!");
                soDT = docSDT(sc);
            }
            // đã kiểm tra xong mới tạo đối tượng nên mã tự sinh k bị nhảy số
            KhachHang kh = thanThiet ? new KhachHangThanThiet(ten, soDT) : new KhachHang(ten, soDT);
            them(kh);
            return kh;
        } catch (NoSuchElementException | IllegalStateException e) {
            // hết dữ liệu nhập giữa chừng thì dừng, menu sẽ in thông báo lỗi
            throw new IllegalStateException("Hết dữ liệu nhập, dừng đăng ký khách hàng");
        }
    }

    // ===================== D. Sửa theo mã =====================
    // tenMoi / sdtMoi: null hoặc rỗng = giữ nguyên mục đó
    // kiểm tra hết các mục trước, đúng hết mới sửa (sai 1 mục thì k sửa mục nào)
    // trả về true khi có thay đổi thật sự; false khi k sửa (đã in lý do cụ thể ra màn hình)
    public boolean sua(String ma, String tenMoi, String sdtMoi) {
        KhachHang kh = timTheoMa(ma == null ? null : ma.trim());
        if (kh == null) {
            System.out.println("Không tìm thấy khách hàng có mã \"" + (ma == null ? "" : ma.trim()) + "\".");
            return false;
        }

        boolean nhapTen = tenMoi != null && !tenMoi.trim().isEmpty();
        boolean nhapSDT = sdtMoi != null && !sdtMoi.trim().isEmpty();
        if (!nhapTen && !nhapSDT) {
            System.out.println("Chưa nhập tên mới hoặc số điện thoại mới nên k có gì để sửa.");
            return false;
        }

        // --- kiểm tra ---
        String ten = null;
        if (nhapTen) {
            ten = tenMoi.trim().replaceAll("\\s+", " ");
            if (!hopLeHoTen(ten)) {
                System.out.println("Tên mới k hợp lệ: phải 2 - 50 ký tự, chỉ gồm chữ cái (k số, k ký tự đặc biệt như | @ #). K sửa gì cả.");
                return false;
            }
        }
        String sdt = null;
        if (nhapSDT) {
            sdt = sdtMoi.trim();
            if (!KhachHang.hopLeSDT(sdt)) {
                System.out.println("SĐT mới k hợp lệ: phải gồm đúng 10 chữ số, bắt đầu bằng 0. K sửa gì cả.");
                return false;
            }
            KhachHang trung = timTheoSDT(sdt);
            if (trung != null && trung != kh) {
                System.out.println("SĐT " + sdt + " đã thuộc về khách " + trung.getMaKH() + " (" + trung.getHoTen() + "). K sửa gì cả.");
                return false;
            }
        }

        // --- chỉ ghi nhận mục nào thật sự khác giá trị cũ ---
        boolean doiTen = ten != null && !ten.equals(kh.getHoTen());
        boolean doiSDT = sdt != null && !sdt.equals(kh.getSoDienThoai());
        if (!doiTen && !doiSDT) {
            System.out.println("Thông tin mới giống hệt thông tin hiện tại của " + kh.getMaKH() + " nên k có gì thay đổi.");
            return false;
        }

        // --- đã kiểm tra xong, sửa ---
        StringBuilder sb = new StringBuilder("Đã sửa " + kh.getMaKH() + ":");
        if (doiTen) {
            sb.append(" tên \"").append(kh.getHoTen()).append("\" -> \"").append(ten).append("\";");
            kh.setHoTen(ten);
        }
        if (doiSDT) {
            sb.append(" SĐT ").append(kh.getSoDienThoai()).append(" -> ").append(sdt).append(";");
            kh.setSoDienThoai(sdt);
        }
        System.out.println(sb.substring(0, sb.length() - 1));
        return true;
    }

    // ===================== E. Xoá theo mã =====================
    @Override
    public boolean xoa(String ma) {
        int vt = timViTri(ma);
        if (vt == -1) {
            return false;
        }
        for (int i = vt; i < n - 1; i++) {
            ds[i] = ds[i + 1];
        }
        ds[--n] = null;
        return true;
    }

    // ===================== F. Tìm kiếm: từ chính xác đến gần đúng, nhiều khoá =====================
    // tìm chính xác theo mã, k có thì NULL
    @Override
    public KhachHang timTheoMa(String ma) {
        int vt = timViTri(ma);
        return vt == -1 ? null : ds[vt];
    }

    // tìm chính xác theo sdt
    public KhachHang timTheoSDT(String sdt) {
        for (int i = 0; i < n; i++) {
            if (ds[i].getSoDienThoai().equals(sdt)) {
                return ds[i];
            }
        }
        return null;
    }

    // tìm gần đúng: từ khoá khớp 1 phần mã, tên (bỏ dấu, k phân biệt hoa thường) hoặc sdt
    public KhachHang[] timGanDung(String tuKhoa) {
        String k = boDau(tuKhoa);
        KhachHang[] tam = new KhachHang[n];
        int dem = 0;
        for (int i = 0; i < n; i++) {
            if (boDau(ds[i].getMaKH()).contains(k) || boDau(ds[i].getHoTen()).contains(k)
                    || ds[i].getSoDienThoai().contains(k)) {
                tam[dem++] = ds[i];
            }
        }
        return catMang(tam, dem);
    }

    // tìm kết hợp nhiều khoá, khoá nào null/rỗng thì bỏ qua
    // ten: tên chứa chuỗi này (gần đúng, bỏ dấu)
    // loai: "THUONG", "THANTHIET" hoặc tên hạng (DONG, BAC, VANG, KIM_CUONG)
    // chiTieuTu / chiTieuDen: khoảng tổng chi tiêu cần tìm
    public KhachHang[] timNangCao(String ten, String loai, Double chiTieuTu, Double chiTieuDen) {
        KhachHang[] tam = new KhachHang[n];
        int dem = 0;
        String k = boDau(ten);
        for (int i = 0; i < n; i++) {
            KhachHang kh = ds[i];
            if (!k.isEmpty() && !boDau(kh.getHoTen()).contains(k)) {
                continue;
            }
            if (!khopLoai(kh, loai)) {
                continue;
            }
            if (chiTieuTu != null && kh.getTongChiTieu() < chiTieuTu) {
                continue;
            }
            if (chiTieuDen != null && kh.getTongChiTieu() > chiTieuDen) {
                continue;
            }
            tam[dem++] = kh;
        }
        return catMang(tam, dem);
    }

    private boolean khopLoai(KhachHang kh, String loai) {
        if (loai == null || loai.trim().isEmpty()) {
            return true;
        }
        String l = loai.trim().toUpperCase();
        if (l.equals("THUONG")) {
            return !(kh instanceof KhachHangThanThiet);
        }
        if (l.equals("THANTHIET")) {
            return kh instanceof KhachHangThanThiet;
        }
        try {
            Hang h = Hang.valueOf(l);
            return kh instanceof KhachHangThanThiet && ((KhachHangThanThiet) kh).getHang() == h;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ===================== G. Thống kê theo nhiều khoá =====================
    public int demThanThiet() {
        int dem = 0;
        for (int i = 0; i < n; i++) {
            if (ds[i] instanceof KhachHangThanThiet) {
                dem++;
            }
        }
        return dem;
    }

    public int demTheoHang(Hang h) {
        int dem = 0;
        for (int i = 0; i < n; i++) {
            if (ds[i] instanceof KhachHangThanThiet && ((KhachHangThanThiet) ds[i]).getHang() == h) {
                dem++;
            }
        }
        return dem;
    }

    public double tongChiTieuToanBo() {
        double tong = 0;
        for (int i = 0; i < n; i++) {
            tong += ds[i].getTongChiTieu();
        }
        return tong;
    }

    public int tongDiemTichLuy() {
        int tong = 0;
        for (int i = 0; i < n; i++) {
            tong += ds[i].getDiemTichLuy();
        }
        return tong;
    }

    // số khách có tổng chi tiêu trong khoảng [tu, den)
    public int demTheoKhoangChiTieu(double tu, double den) {
        int dem = 0;
        for (int i = 0; i < n; i++) {
            if (ds[i].getTongChiTieu() >= tu && ds[i].getTongChiTieu() < den) {
                dem++;
            }
        }
        return dem;
    }

    // in báo cáo thống kê tổng hợp
    public void inThongKe() {
        System.out.println("===== THỐNG KÊ KHÁCH HÀNG =====");
        System.out.println("Tổng số khách       : " + n);
        System.out.println("Khách thường        : " + (n - demThanThiet()));
        System.out.println("Khách thân thiết    : " + demThanThiet());
        for (Hang h : Hang.values()) {
            System.out.println("  - Hạng " + String.format("%-10s", h.getTen()) + ": " + demTheoHang(h));
        }
        System.out.println("Theo mức chi tiêu:");
        System.out.println("  - Dưới 500.000 VND  : " + demTheoKhoangChiTieu(0, 500000));
        System.out.println("  - 500.000 - 2 triệu  : " + demTheoKhoangChiTieu(500000, 2000000));
        System.out.println("  - 2 triệu - 5 triệu : " + demTheoKhoangChiTieu(2000000, 5000000));
        System.out.println("  - Từ 5 triệu        : " + demTheoKhoangChiTieu(5000000, Double.MAX_VALUE));
        System.out.println("Tổng chi tiêu       : " + IGiamGia.dinhDangTien(tongChiTieuToanBo()));
        System.out.println("Tổng điểm tích luỹ  : " + tongDiemTichLuy());
    }

    // ===================== H. Nghiệp vụ khác: tích điểm, nâng cấp, đổi điểm, top khách =====================
    // khách mua hàng: tích điểm, đủ điều kiện thì tự nâng lên thân thiết
    // trả về số điểm được cộng, -1 nếu k có khách này
    public int ghiNhanMuaHang(String ma, double soTienThucTra) {
        int vt = timViTri(ma);
        if (vt == -1) {
            return -1;
        }
        int diem = ds[vt].tichDiem(soTienThucTra);
        if (!(ds[vt] instanceof KhachHangThanThiet) && ds[vt].getTongChiTieu() >= NGUONG_THAN_THIET) {
            ds[vt] = new KhachHangThanThiet(ds[vt]);
            System.out.println(">> Chúc mừng " + ds[vt].getHoTen() + " trở thành khách hàng thân thiết!");
        }
        return diem;
    }

    // đổi điểm lấy tiền giảm, trả về -1 nếu k có khách hoặc k đủ điểm
    public double doiDiem(String ma, int soDiem) {
        KhachHang kh = timTheoMa(ma);
        if (kh == null) {
            return -1;
        }
        try {
            return kh.doiDiem(soDiem);
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    // k khách chi tiêu nhiều nhất (sắp xếp trên bản sao, k đổi thứ tự list gốc)
    public KhachHang[] topKhach(int k) {
        KhachHang[] ban = Arrays.copyOf(ds, n);
        for (int i = 0; i < ban.length - 1; i++) {
            for (int j = i + 1; j < ban.length; j++) {
                if (ban[j].getTongChiTieu() > ban[i].getTongChiTieu()) {
                    KhachHang t = ban[i];
                    ban[i] = ban[j];
                    ban[j] = t;
                }
            }
        }
        return Arrays.copyOf(ban, Math.min(Math.max(k, 0), ban.length));
    }

    // ===================== I. Đa hình: ưu đãi của từng khách trên cùng một hoá đơn =====================
    public void inUuDaiTheoKhach(double tongTien) {
        System.out.println("Ưu đãi cho hoá đơn " + IGiamGia.dinhDangTien(tongTien) + ":");
        for (int i = 0; i < n; i++) {
            IGiamGia uuDai = ds[i];   // tham chiếu interface -> tự gọi đúng hàm của từng loại khách
            System.out.printf("  %-22s giảm %12s  (%s)%n", ds[i].getHoTen(),
                    IGiamGia.dinhDangTien(uuDai.tinhGiamGia(tongTien)), uuDai.getMoTa());
        }
    }

    // ===================== Đọc / ghi file =====================

    /**
     * Đọc danh sách từ file (thay thế dữ liệu hiện tại). Dòng trống hoặc bắt đầu bằng '#' bị bỏ qua,
     * dòng sai định dạng bị báo lỗi ra System.err và bỏ qua.
     * @return số phần tử đọc được (file không tồn tại -> 0, giữ nguyên danh sách)
     */
    @Override
    public int docFile(String duongDan) throws IOException {
        Path path = Paths.get(duongDan);
        if (!Files.exists(path)) {
            return 0;
        }
        ds = new KhachHang[10];
        n = 0;
        int soDong = 0;
        for (String dong : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            soDong++;
            dong = dong.replace("\uFEFF", "").trim();
            if (dong.isEmpty() || dong.startsWith("#")) {
                continue;
            }
            try {
                if (!them(KhachHang.fromFileString(dong))) {
                    System.err.println("Dòng " + soDong + ": trùng mã / SĐT, bỏ qua");
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
