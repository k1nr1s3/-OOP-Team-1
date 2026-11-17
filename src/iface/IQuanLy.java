package iface;


public interface IQuanLy<T> {
    boolean them(T doiTuong);
    boolean xoa(String ma);
    T timTheoMa(String ma);
    void hienThi();
    int soLuong();
}
