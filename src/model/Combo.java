package model;

import iface.IGiamGia;
import iface.IQuanLy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Combo extends SanPham {

    private String[] maThanhPhan;
    // has-a: Combo chứa các món thành phần (SanPham thật, liên kết từ mã bằng lienKet). Mã lặp lại = nhiều phần.
    private final List<SanPham> monThanhPhan = new ArrayList<>();

    public Combo(String maSP, String tenSP, double donGia, String dsMa) {
        super(maSP, tenSP, donGia);
        setThanhPhan(dsMa);
    }

    @Override
    public String getLoai() { return "Combo"; }

    @Override
    protected String maLoai() { return "COMBO"; }

    @Override
    public String moTaThem() {
        if (monThanhPhan.isEmpty()) {
            return "Gồm: " + String.join(", ", maThanhPhan);
        }
        StringBuilder sb = new StringBuilder("Gồm: ");
        for (int i = 0; i < monThanhPhan.size(); i++) {
            sb.append(i > 0 ? ", " : "").append(monThanhPhan.get(i).getTenSP());
        }
        return sb.toString();
    }

    @Override
    protected String chuoiRieng() { return String.join(";", maThanhPhan); }

    @Override
    public void hienThiThongTin() {
        System.out.println("Combo: " + tenSP + " (" + maSP + ") - Giá: " + IGiamGia.dinhDangTien(donGia)
                + " - " + moTaThem());
        for (SanPham sp : monThanhPhan) {
            System.out.println("   + " + sp.getTenSP() + " (" + sp.getMaSP() + "): " + sp.getCongThuc().moTa(null));
        }
    }

    // ===== has-a: liên kết mã thành phần với sản phẩm thật, trả về danh sách mã không liên kết được =====
    // (k tìm thấy mã, hoặc thành phần cũng là Combo -> bỏ qua để tránh vòng lặp công thức)
    public List<String> lienKet(IQuanLy<SanPham> ds) {
        monThanhPhan.clear();
        List<String> loi = new ArrayList<>();
        for (String ma : maThanhPhan) {
            SanPham sp = ds.timTheoMa(ma);
            if (sp == null || sp instanceof Combo) {
                loi.add(ma);
            } else {
                monThanhPhan.add(sp);
            }
        }
        return loi;
    }

    public List<SanPham> getMonThanhPhan() { return Collections.unmodifiableList(monThanhPhan); }

    // công thức của combo = tổng công thức các món thành phần (tính lại mỗi lần nên luôn khớp khi sửa công thức món)
    @Override
    public CongThuc getCongThuc() {
        CongThuc tong = new CongThuc();
        for (SanPham sp : monThanhPhan) {
            tong.congThem(sp.getCongThuc(), 1);
        }
        return tong;
    }

    @Override
    public void setCongThuc(CongThuc congThuc) {
        throw new UnsupportedOperationException("Combo k có công thức riêng, hãy sửa công thức của các món thành phần");
    }

    @Override
    protected String chuoiCongThucFile() { return ""; }

    // tổng giá nếu mua lẻ từng món (mã k có trong danh sách thì bỏ qua)
    public double tinhGiaLe(IQuanLy<SanPham> ds) {
        double tong = 0;
        for (String ma : maThanhPhan) {
            SanPham sp = ds.timTheoMa(ma);
            if (sp != null) {
                tong += sp.getDonGia();
            }
        }
        return tong;
    }

    // số tiền khách tiết kiệm khi mua combo so với mua lẻ
    public double tienTietKiem(IQuanLy<SanPham> ds) {
        return Math.max(tinhGiaLe(ds) - donGia, 0);
    }

    public String[] getMaThanhPhan() { return maThanhPhan.clone(); }

    public void setThanhPhan(String dsMa) {
        if (dsMa == null || dsMa.trim().isEmpty()) {
            throw new IllegalArgumentException("Combo phải có ít nhất 1 sản phẩm thành phần");
        }
        String[] p = dsMa.split("[;,]");
        for (int i = 0; i < p.length; i++) {
            p[i] = p[i].trim();
            if (p[i].isEmpty()) {
                throw new IllegalArgumentException("Danh sách mã thành phần k hợp lệ: " + dsMa);
            }
        }
        this.maThanhPhan = p;
        monThanhPhan.clear();   // mã đổi thì liên kết cũ hết hiệu lực, cần lienKet lại
    }
}
