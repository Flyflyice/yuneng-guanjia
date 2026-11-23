"""把 d8 生成的 classes.dex 写入未签名 APK。

aapt2 link 只产出资源，不含 dex；这里以 zip 追加方式写入，
随后由 zipalign 与 apksigner 完成剩余步骤。
"""
import sys
import zipfile


def main() -> int:
    if len(sys.argv) != 3:
        print("用法: python add_dex.py classes.dex app.apk")
        return 2
    dex_path, apk_path = sys.argv[1], sys.argv[2]
    with zipfile.ZipFile(apk_path, "a", compression=zipfile.ZIP_DEFLATED) as zf:
        zf.write(dex_path, "classes.dex")
    print(f"已写入 classes.dex -> {apk_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
