# -*- coding: utf-8 -*-
"""生成“驭能管家”启动图标：绿色渐变底 + 白色叶片 + 绿色闪电。"""
from PIL import Image, ImageDraw, ImageFilter
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
S = 1024


def bezier(p0, p1, p2, p3, n=80):
    pts = []
    for i in range(n + 1):
        t = i / n
        u = 1 - t
        x = u**3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t**3 * p3[0]
        y = u**3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t**3 * p3[1]
        pts.append((x, y))
    return pts


def leaf_path():
    # 从叶尖顺时针画一片对称叶子
    pts = [(S * 0.5, S * 0.10)]
    pts += bezier((S * 0.5, S * 0.10), (S * 0.22, S * 0.26), (S * 0.17, S * 0.48), (S * 0.19, S * 0.58))
    pts += bezier((S * 0.19, S * 0.58), (S * 0.22, S * 0.72), (S * 0.35, S * 0.83), (S * 0.5, S * 0.90))
    pts += bezier((S * 0.5, S * 0.90), (S * 0.65, S * 0.83), (S * 0.78, S * 0.72), (S * 0.81, S * 0.58))
    pts += bezier((S * 0.81, S * 0.58), (S * 0.83, S * 0.48), (S * 0.78, S * 0.26), (S * 0.5, S * 0.10))
    return pts


def make_icon():
    # 圆角矩形渐变
    grad = Image.new("RGB", (S, S), "#00C853")
    d = ImageDraw.Draw(grad)
    for y in range(S):
        t = y / S
        # 顶部亮绿，底部深绿
        r = int(0x00 * (1 - t) + 0x00 * t)
        g = int(0xE0 * (1 - t) + 0xA0 * t)
        b = int(0x5A * (1 - t) + 0x3E * t)
        d.line([(0, y), (S, y)], fill=(r, g, b))
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, S - 1, S - 1], radius=int(S * 0.22), fill=255)
    icon = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    icon.paste(grad, (0, 0), mask)

    # 叶片阴影
    shadow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).polygon(leaf_path(), fill=(0, 90, 35, 90))
    shadow = shadow.filter(ImageFilter.GaussianBlur(18))
    icon.alpha_composite(shadow, (0, 14))

    # 叶片
    d = ImageDraw.Draw(icon)
    d.polygon(leaf_path(), fill=(255, 255, 255, 255))

    # 叶脉：主脉 + 两侧侧脉（绿色）
    vein = (0x00, 0xA8, 0x44, 255)
    d.line([(S * 0.5, S * 0.17), (S * 0.5, S * 0.84)], fill=vein, width=int(S * 0.022))
    for i in range(4):
        y = S * (0.34 + i * 0.12)
        w = S * (0.20 - i * 0.025)
        d.line([(S * 0.5, y), (S * 0.5 - w, y + S * 0.08)], fill=vein, width=int(S * 0.015))
        d.line([(S * 0.5, y), (S * 0.5 + w, y + S * 0.08)], fill=vein, width=int(S * 0.015))

    # 中央闪电（绿色，压在叶脉上）
    bolt = [
        (S * 0.545, S * 0.27), (S * 0.405, S * 0.55), (S * 0.492, S * 0.55),
        (S * 0.445, S * 0.77), (S * 0.620, S * 0.47), (S * 0.525, S * 0.47), (S * 0.585, S * 0.27),
    ]
    d.polygon(bolt, fill=vein)
    return icon


def main():
    icon = make_icon()
    # 保留 512 预览图
    icon.resize((512, 512), Image.LANCZOS).save(os.path.join(ROOT, "app-icon-512.png"))
    sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for name, px in sizes.items():
        folder = os.path.join(RES, "mipmap-" + name)
        os.makedirs(folder, exist_ok=True)
        icon.resize((px, px), Image.LANCZOS).save(os.path.join(folder, "ic_launcher.png"))
        # 圆形图标
        rounded = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        mask = Image.new("L", (px, px), 0)
        ImageDraw.Draw(mask).ellipse([0, 0, px - 1, px - 1], fill=255)
        rounded.paste(icon.resize((px, px), Image.LANCZOS), (0, 0), mask)
        rounded.save(os.path.join(folder, "ic_launcher_round.png"))
    print("图标已生成：", sizes)


if __name__ == "__main__":
    main()
