# -*- coding: utf-8 -*-
"""把研究坐标画成图片：现有研究 + 推荐空位 + 坐标规则（网格按内容自动扩展）。"""
import glob
import json
import os

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "research_grid.png")

CELL = 90
GX, GY = 100, 180
MARGIN_BOTTOM = 160

BG_TOP = (32, 23, 46)
BG_BOTTOM = (18, 13, 26)
GRID_BG = (37, 27, 52)
GRID_LINE = (58, 44, 78)
TEXT = (232, 220, 245)
MUTED = (168, 150, 190)
ACCENT = (185, 139, 224)
ACCENT_FILL = (74, 45, 104)
GREEN = (122, 205, 138)

RING = [(0, -2), (2, -2), (2, 0), (2, 2), (0, 2), (-2, 2), (-2, 0), (-2, -2)]
CHAIN_FREE = [(0, 14), (0, 16)]


def font(size, bold=False):
    name = "msyhbd.ttc" if bold else "msyh.ttc"
    path = os.path.join(r"C:\Windows\Fonts", name)
    if not os.path.exists(path):
        path = os.path.join(r"C:\Windows\Fonts", "simhei.ttf")
    return ImageFont.truetype(path, size)


def ellipsize(draw, text, f, max_width):
    if draw.textlength(text, font=f) <= max_width:
        return text
    while len(text) > 3:
        text = text[:-1]
        candidate = text + "…"
        if draw.textlength(candidate, font=f) <= max_width:
            return candidate
    return text


def dashed_rect(draw, box, color, dash=8, gap=6, width=2):
    x0, y0, x1, y1 = box
    for x in range(x0, x1, dash + gap):
        draw.line([(x, y0), (min(x + dash, x1), y0)], fill=color, width=width)
        draw.line([(x, y1), (min(x + dash, x1), y1)], fill=color, width=width)
    for y in range(y0, y1, dash + gap):
        draw.line([(x0, y), (x0, min(y + dash, y1))], fill=color, width=width)
        draw.line([(x1, y), (x1, min(y + dash, y1))], fill=color, width=width)


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
    entries.sort(key=lambda e: (e["row"], e["col"]))
    occupied = {(e["col"], e["row"]): e for e in entries}

    c0 = min([-4] + [e["col"] for e in entries])
    c1 = max([4] + [e["col"] for e in entries])
    r0 = min([-3] + [e["row"] for e in entries])
    r1 = max([16] + [e["row"] for e in entries])

    gw = (c1 - c0 + 1) * CELL
    gh = (r1 - r0 + 1) * CELL
    W = 2000
    H = GY + gh + MARGIN_BOTTOM

    img = Image.new("RGB", (W, H), BG_BOTTOM)
    draw = ImageDraw.Draw(img)
    for y in range(H):
        t = y / H
        color = tuple(int(BG_TOP[i] * (1 - t) + BG_BOTTOM[i] * t) for i in range(3))
        draw.line([(0, y), (W, y)], fill=color)

    draw.text((GX, 46), "神秘动力学 · 研究排列坐标图", font=font(44, True), fill=TEXT)
    draw.text((GX, 104), "Mystery Dynamics — Thaumonomicon Research Grid",
              font=font(22), fill=MUTED)

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
        f = font(17, True)
        w = draw.textlength(label, font=f)
        draw.text((x + CELL / 2 - w / 2, GY - 38), label, font=f,
                  fill=ACCENT if c == 0 else MUTED)
    for r in range(r0, r1 + 1):
        y = GY + (r - r0) * CELL
        label = str(r)
        f = font(17, True)
        w = draw.textlength(label, font=f)
        draw.text((GX - 28 - w, y + CELL / 2 - 10), label, font=f,
                  fill=ACCENT if r == 0 else MUTED)

    draw.text((GX, GY + gh + 16), "row ↓ 下", font=font(18), fill=MUTED)

    # 现有研究
    for (col, row), e in occupied.items():
        x = GX + (col - c0) * CELL
        y = GY + (row - r0) * CELL
        draw.rounded_rectangle([x + 4, y + 4, x + CELL - 4, y + CELL - 4],
                               radius=10, fill=ACCENT_FILL, outline=ACCENT, width=2)
        f = fit = font(15, True)
        label = ellipsize(draw, e["key"], f, CELL - 12)
        draw.text((x + CELL / 2, y + CELL / 2 - 12), label, font=f,
                  fill=TEXT, anchor="mm")
        draw.text((x + CELL / 2, y + CELL / 2 + 15), f"({col}, {row})",
                  font=font(13), fill=MUTED, anchor="mm")

    # 推荐空位
    free_cells = []
    for c, r in RING + CHAIN_FREE:
        if (c, r) in occupied:
            continue
        free_cells.append((c, r, len(free_cells) + 1))
    for col, row, idx in free_cells:
        x = GX + (col - c0) * CELL
        y = GY + (row - r0) * CELL
        dashed_rect(draw, (x + 5, y + 5, x + CELL - 5, y + CELL - 5), GREEN, width=2)
        draw.text((x + CELL / 2, y + CELL / 2 - 12), f"空位 {idx}",
                  font=font(14, True), fill=GREEN, anchor="mm")
        draw.text((x + CELL / 2, y + CELL / 2 + 12), f"({col}, {row})",
                  font=font(13), fill=MUTED, anchor="mm")

    # 右侧信息面板
    px = GX + gw + 80
    pw = W - px - 70
    panel_h = 1060
    draw.rounded_rectangle([px, GY - 14, px + pw, GY - 14 + panel_h], radius=18,
                           fill=(26, 19, 38), outline=GRID_LINE, width=2)
    tx = px + 34
    ty = GY + 14

    draw.text((tx, ty), "坐标规则", font=font(28, True), fill=TEXT)
    ty += 50
    rules = [
        "col 向右递增，row 向下递增",
        "(0, 0) 是分类正中央",
        "每格 24 px；节点框 26 px",
        "相邻 1 格会轻微重叠，建议间隔 2 格",
        "order 只影响列表排序，不影响地图位置",
        "parents 只负责画连线，不影响坐标",
    ]
    for line in rules:
        draw.text((tx, ty), "·", font=font(20, True), fill=ACCENT)
        draw.text((tx + 20, ty), line, font=font(18), fill=TEXT)
        ty += 31
    ty += 14

    draw.text((tx, ty), f"当前研究（{len(entries)}）", font=font(24, True), fill=TEXT)
    ty += 38
    draw.text((tx, ty), "key", font=font(16, True), fill=MUTED)
    draw.text((tx + 300, ty), "col", font=font(16, True), fill=MUTED)
    draw.text((tx + 380, ty), "row", font=font(16, True), fill=MUTED)
    ty += 26
    for e in entries:
        draw.text((tx, ty), ellipsize(draw, e["key"], font(18), 280),
                  font=font(18), fill=TEXT)
        draw.text((tx + 300, ty), str(e["col"]), font=font(18), fill=TEXT)
        draw.text((tx + 380, ty), str(e["row"]), font=font(18), fill=TEXT)
        ty += 29
    ty += 18

    draw.text((tx, ty), "推荐空位（间隔 2 格）", font=font(24, True), fill=GREEN)
    ty += 38
    col_w = (pw - 68) // 2
    for i, (col, row, idx) in enumerate(free_cells):
        cx2 = tx + (i % 2) * col_w
        cy2 = ty + (i // 2) * 28
        draw.text((cx2, cy2), f"{idx}.  ({col}, {row})", font=font(17), fill=TEXT)
    ty += ((len(free_cells) + 1) // 2) * 28 + 20

    # 图例
    draw.rounded_rectangle([tx, ty, tx + 24, ty + 24], radius=5,
                           fill=ACCENT_FILL, outline=ACCENT, width=2)
    draw.text((tx + 34, ty + 1), "已有研究", font=font(17), fill=TEXT)
    dashed_rect(draw, (tx + 160, ty, tx + 184, ty + 24), GREEN, width=2)
    draw.text((tx + 194, ty + 1), "推荐空位", font=font(17), fill=TEXT)
    ty += 40

    draw.text((tx, ty), "新增研究", font=font(22, True), fill=TEXT)
    ty += 32
    for line in (
        "文件：data/mystery_dynamics/research/<目录>/<名称>.json",
        "字段：key / category / col / row / item",
        "复查：python tools/research_layout.py",
    ):
        draw.text((tx, ty), line, font=font(15), fill=MUTED)
        ty += 25

    draw.text((GX, H - 36), "生成工具：tools/research_layout.py + tools/render_research_grid.py",
              font=font(16), fill=(110, 96, 130))

    img.save(OUT)
    print("saved", OUT, f"{W}x{H}")


if __name__ == "__main__":
    main()
