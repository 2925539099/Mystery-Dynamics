# -*- coding: utf-8 -*-
"""把研究坐标画成图片：现有研究 + 推荐空位 + 坐标规则。"""
import glob
import json
import os

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "research_grid.png")

W, H = 2000, 1150
BG_TOP = (32, 23, 46)
BG_BOTTOM = (18, 13, 26)
GRID_BG = (37, 27, 52)
GRID_LINE = (58, 44, 78)
TEXT = (232, 220, 245)
MUTED = (168, 150, 190)
ACCENT = (185, 139, 224)
ACCENT_FILL = (74, 45, 104)
GREEN = (122, 205, 138)

GRID_COLS = (-4, 4)
GRID_ROWS = (-3, 3)
CELL = 120
GX, GY = 100, 180

RING = [(0, -2), (2, -2), (2, 0), (2, 2), (0, 2), (-2, 2), (-2, 0), (-2, -2)]


def font(size, bold=False):
    name = "msyhbd.ttc" if bold else "msyh.ttc"
    path = os.path.join(r"C:\Windows\Fonts", name)
    if not os.path.exists(path):
        path = os.path.join(r"C:\Windows\Fonts", "simhei.ttf")
    return ImageFont.truetype(path, size)


def fit_font(draw, text, max_width, size, bold=False, min_size=12):
    while size > min_size:
        f = font(size, bold)
        if draw.textlength(text, font=f) <= max_width:
            return f
        size -= 1
    return font(min_size, bold)


def dashed_rect(draw, box, color, dash=8, gap=6, width=2):
    x0, y0, x1, y1 = box
    for x in range(x0, x1, dash + gap):
        draw.line([(x, y0), (min(x + dash, x1), y0)], fill=color, width=width)
        draw.line([(x, y1), (min(x + dash, x1), y1)], fill=color, width=width)
    for y in range(y0, y1, dash + gap):
        draw.line([(x0, y), (x0, min(y + dash, y1))], fill=color, width=width)
        draw.line([(x1, y), (x1, min(y + dash, y1))], fill=color, width=width)


def ellipsize(draw, text, f, max_width):
    if draw.textlength(text, font=f) <= max_width:
        return text
    while len(text) > 3:
        text = text[:-1]
        candidate = text + "…"
        if draw.textlength(candidate, font=f) <= max_width:
            return candidate
    return text

def load_entries():
    pattern = os.path.join(ROOT, "src", "main", "resources", "data", "*", "research", "**", "*.json")
    entries = []
    for path in sorted(glob.glob(pattern, recursive=True)):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        entries.append({
            "key": data.get("key", "?"),
            "category": data.get("category", "?"),
            "col": int(data.get("col", 0)),
            "row": int(data.get("row", 0)),
        })
    return entries


def main():
    entries = [e for e in load_entries() if e["category"] == "MYSTERY_DYNAMICS"]
    occupied = {(e["col"], e["row"]): e for e in entries}

    img = Image.new("RGB", (W, H), BG_BOTTOM)
    draw = ImageDraw.Draw(img)
    for y in range(H):
        t = y / H
        color = tuple(int(BG_TOP[i] * (1 - t) + BG_BOTTOM[i] * t) for i in range(3))
        draw.line([(0, y), (W, y)], fill=color)

    draw.text((GX, 52), "神秘动力学 · 研究排列坐标图", font=font(44, True), fill=TEXT)
    draw.text((GX, 112), "Mystery Dynamics — Thaumonomicon Research Grid",
              font=font(22), fill=MUTED)

    c0, c1 = GRID_COLS
    r0, r1 = GRID_ROWS
    gw = (c1 - c0 + 1) * CELL
    gh = (r1 - r0 + 1) * CELL
    draw.rounded_rectangle([GX - 14, GY - 14, GX + gw + 14, GY + gh + 14],
                           radius=18, fill=GRID_BG, outline=GRID_LINE, width=2)
    for c in range(c0, c1 + 1):
        for r in range(r0, r1 + 1):
            x = GX + (c - c0) * CELL
            y = GY + (r - r0) * CELL
            draw.rectangle([x, y, x + CELL, y + CELL], outline=GRID_LINE, width=1)

    for c in range(c0, c1 + 1):
        x = GX + (c - c0) * CELL
        label = str(c)
        f = font(20, True)
        w = draw.textlength(label, font=f)
        draw.text((x + CELL / 2 - w / 2, GY - 44), label, font=f,
                  fill=ACCENT if c == 0 else MUTED)
    for r in range(r0, r1 + 1):
        y = GY + (r - r0) * CELL
        label = str(r)
        f = font(20, True)
        w = draw.textlength(label, font=f)
        draw.text((GX - 34 - w, y + CELL / 2 - 12), label, font=f,
                  fill=ACCENT if r == 0 else MUTED)

    draw.text((GX + gw - 190, GY - 78), "col → 右", font=font(20), fill=MUTED)
    draw.text((GX, GY + gh + 18), "row ↓ 下", font=font(20), fill=MUTED)

    # 现有研究
    for (col, row), e in occupied.items():
        x = GX + (col - c0) * CELL
        y = GY + (row - r0) * CELL
        draw.rounded_rectangle([x + 4, y + 4, x + CELL - 4, y + CELL - 4],
                               radius=12, fill=ACCENT_FILL, outline=ACCENT, width=3)
        f = fit_font(draw, e["key"], CELL - 18, 19, True)
        label = ellipsize(draw, e["key"], f, CELL - 18)
        draw.text((x + CELL / 2, y + CELL / 2 - 12), label, font=f,
                  fill=TEXT, anchor="mm")
        draw.text((x + CELL / 2, y + CELL / 2 + 22), f"({col}, {row})",
                  font=font(16), fill=MUTED, anchor="mm")

    # 推荐空位
    for idx, (col, row) in enumerate(RING, 1):
        if (col, row) in occupied:
            continue
        x = GX + (col - c0) * CELL
        y = GY + (row - r0) * CELL
        dashed_rect(draw, (x + 6, y + 6, x + CELL - 6, y + CELL - 6), GREEN, width=2)
        draw.text((x + CELL / 2, y + CELL / 2 - 16), f"空位 {idx}",
                  font=font(17, True), fill=GREEN, anchor="mm")
        draw.text((x + CELL / 2, y + CELL / 2 + 14), f"({col}, {row})",
                  font=font(16), fill=MUTED, anchor="mm")

    # 右侧信息面板
    px = GX + gw + 80
    pw = W - px - 70
    draw.rounded_rectangle([px, GY - 14, px + pw, GY + gh + 14], radius=18,
                           fill=(26, 19, 38), outline=GRID_LINE, width=2)
    tx = px + 34
    ty = GY + 18

    draw.text((tx, ty), "坐标规则", font=font(30, True), fill=TEXT)
    ty += 52
    rules = [
        "col 向右递增，row 向下递增",
        "(0, 0) 是分类正中央",
        "每格 24 px；节点框 26 px",
        "相邻 1 格会轻微重叠，建议间隔 2 格",
        "order 只影响列表排序，不影响地图位置",
        "parents 只负责画连线，不影响坐标",
    ]
    for line in rules:
        draw.text((tx, ty), "·", font=font(22, True), fill=ACCENT)
        draw.text((tx + 22, ty), line, font=font(20), fill=TEXT)
        ty += 34

    ty = GY + 290
    draw.text((tx, ty), f"当前研究（{len(entries)}）", font=font(26, True), fill=TEXT)
    ty += 42
    draw.text((tx, ty), "key", font=font(17, True), fill=MUTED)
    draw.text((tx + 250, ty), "col", font=font(17, True), fill=MUTED)
    draw.text((tx + 340, ty), "row", font=font(17, True), fill=MUTED)
    ty += 30
    for e in entries:
        draw.text((tx, ty), e["key"], font=font(19), fill=TEXT)
        draw.text((tx + 250, ty), str(e["col"]), font=font(19), fill=TEXT)
        draw.text((tx + 340, ty), str(e["row"]), font=font(19), fill=TEXT)
        ty += 32

    ty = max(GY + 420, ty + 24)
    draw.text((tx, ty), "推荐空位（8 个，间隔 2 格）", font=font(26, True), fill=GREEN)
    ty += 46
    col_w = (pw - 68) // 2
    for i, (col, row) in enumerate(RING):
        cx2 = tx + (i % 2) * col_w
        cy2 = ty + (i // 2) * 30
        draw.text((cx2, cy2), f"{i + 1}.  ({col}, {row})", font=font(20), fill=TEXT)

    # 图例
    ly = GY + 750
    draw.rounded_rectangle([tx, ly, tx + 26, ly + 26], radius=6,
                           fill=ACCENT_FILL, outline=ACCENT, width=2)
    draw.text((tx + 38, ly + 2), "已有研究", font=font(19), fill=TEXT)
    dashed_rect(draw, (tx + 170, ly, tx + 196, ly + 26), GREEN, width=2)
    draw.text((tx + 208, ly + 2), "推荐空位", font=font(19), fill=TEXT)

    # 新增研究说明
    ty = GY + 620
    draw.text((tx, ty), "新增研究", font=font(24, True), fill=TEXT)
    ty += 38
    for line in (
        "文件：data/mystery_dynamics/research/<目录>/<名称>.json",
        "字段：key / category / col / row / item",
        "复查：python tools/research_layout.py",
    ):
        draw.text((tx, ty), line, font=font(17), fill=MUTED)
        ty += 28

    draw.text((GX, H - 42), "生成工具：tools/research_layout.py + tools/render_research_grid.py",
              font=font(17), fill=(110, 96, 130))

    img.save(OUT)
    print("saved", OUT)


if __name__ == "__main__":
    main()
