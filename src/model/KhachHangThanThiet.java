package model;

// khách thân thiết, kế thừa KhachHang
// có hạng (Đồng/Bạc/Vàng/Kim cương) tự tính theo tổng chi tiêu, hạng càng cao thì nhân điểm nhiều + giảm sâu hơn --trung
public class KhachHangThanThiet extends KhachHang {

    // các hạng thành viên
    public enum Hang {
        DONG("Đồng", 0, 2, 1.0),
        BAC("Bạc", 2000000, 5, 1.2),
        VANG("Vàng", 5000000, 8, 1.5),
        KIM_CUONG("Kim cương", 10000000, 12, 2.0);

        private final String ten;
        private final double nguongChiTieu;
        private final double phanTramGiam;
        private final double heSoDiem;

        Hang(String ten, double nguongChiTieu, double phanTramGiam, double heSoDiem) {
            this.ten = ten;
            this.nguongChiTieu = nguongChiTieu;
            this.phanTramGiam = phanTramGiam;
            this.heSoDiem = heSoDiem;
        }

        public String getTen() { return ten; }
        public double getNguongChiTieu() { return nguongChiTieu; }
        public double getPhanTramGiam() { return phanTramGiam; }
        public double getHeSoDiem() { return heSoDiem; }

        // lấy hạng theo tổng chi tiêu
        public static Hang tuChiTieu(double tongChiTieu) {
            Hang kq = DONG;
            for (Hang h : values()) {
                if (tongChiTieu >= h.nguongChiTieu) {
                    kq = h;
                }
            }
            return kq;
        }

        // hạng kế tiếp, null nếu đang cao nhất rồi
        public Hang hangTiepTheo() {
            Hang[] ds = values();
            return ordinal() + 1 < ds.length ? ds[ordinal() + 1] : null;
        }
    }

    // ===================== Constructor =====================
    public KhachHangThanThiet(String hoTen, String soDienThoai) {
        super(hoTen, soDienThoai);
    }

    public KhachHangThanThiet(String maKH, String hoTen, String soDienThoai) {
        super(maKH, hoTen, soDienThoai);
    }

    public KhachHangThanThiet(String maKH, String hoTen, String soDienThoai, int diemTichLuy, double tongChiTieu) {
        super(maKH, hoTen, soDienThoai, diemTichLuy, tongChiTieu);
    }

    // nâng cấp từ khách thường, giữ nguyên mã, thông tin, điểm với chi tiêu
    public KhachHangThanThiet(KhachHang kh) {
        super(kh.getMaKH(), kh.getHoTen(), kh.getSoDienThoai(), kh.getDiemTichLuy(), kh.getTongChiTieu());
    }

    // ===================== Hạng =====================
    public Hang getHang() {
        return Hang.tuChiTieu(tongChiTieu);
    }

    // còn thiếu bao nhiêu tiền để lên hạng tiếp (0 nếu cao nhất rồi)
    public double chiTieuConThieuDeLenHang() {
        Hang tiep = getHang().hangTiepTheo();
        return tiep == null ? 0 : tiep.getNguongChiTieu() - tongChiTieu;
    }

    // ===================== Ghi đè (đa hình) =====================
    @Override
    protected double heSoTichDiem() {
        return getHang().getHeSoDiem();
    }

    @Override
    public double tinhGiamGia(double tongTien) {
        return tongTien * getHang().getPhanTramGiam() / 100;
    }

    @Override
    public String getMoTa() {
        Hang h = getHang();
        return "Thành viên " + h.getTen() + " - giảm " + h.getPhanTramGiam()
                + "%, tích điểm x" + h.getHeSoDiem();
    }

    @Override
    public String getLoai() {
        return "Thân thiết-" + getHang().getTen();
    }

    @Override
    protected String maLoai() {
        return "THANTHIET";
    }
}
