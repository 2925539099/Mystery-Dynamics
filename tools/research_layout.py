# -*- coding: utf-8 -*-
"""扫描本模组的研究数据，列出坐标并画成研究书网格，便于规划排列。

坐标约定（与 port.265 的 ThaumonomiconScreen 一致）：
- 屏幕位置 = (16 + col * 24, 17 + row * 24) - 视图偏移；
- col 向右递增，row 向下递增，每格 24 像素，(0, 0) 是分类中央。
"""
import glob
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PATTERN = os.path.join(ROOT, "src", "main", "resources", "data", "*", "research", "**", "*.json")


def load_entries():
    entries = []
    for path in sorted(glob.glob(PATTERN, recursive=True)):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        icon = data.get("item") or data.get("icon") or ""
        entries.append({
            "file": os.path.relpath(path, ROOT),
            "key": data.get("key", "?"),
            "category": data.get("category", "?"),
            "col": int(data.get("col", 0)),
            "row": int(data.get("row", 0)),
            "order": int(data.get("order", 0)),
            "parents": data.get("parents", []),
            "icon": icon,
            "pages": len(data.get("pages", [])),
        })
    return entries


def print_table(entries):
    print("| key | 分类 | col | row | order | 前置 | 图标 | 页面数 |")
    print("|---|---|---|---|---|---|---|---|")
    for e in sorted(entries, key=lambda x: (x["category"], x["row"], x["col"])):
        parents = ", ".join(e["parents"]) if e["parents"] else "—"
        print(f"| {e['key']} | {e['category']} | {e['col']} | {e['row']} | {e['order']} | {parents} | {e['icon']} | {e['pages']} |")


def print_grid(entries, category, cell=14):
    items = [e for e in entries if e["category"] == category]
    if not items:
        print(f"\n[{category}] 没有条目")
        return
    cols = [e["col"] for e in items]
    rows = [e["row"] for e in items]
    # 至少展示 (0,0) 周围 9×7 的规划区，方便继续排研究
    c0, c1 = min(cols + [0, -4]), max(cols + [0, 4])
    r0, r1 = min(rows + [0, -3]), max(rows + [0, 3])
    occupied = {(e["col"], e["row"]): e["key"] for e in items}

    print(f"\n[{category}] 网格（col {c0}..{c1} →，row {r0}..{r1} ↓；★ 为 0,0）")
    header = " " * 6 + "".join(f"{c:^{cell}}" for c in range(c0, c1 + 1))
    print(header)
    for r in range(r0, r1 + 1):
        cells = []
        for c in range(c0, c1 + 1):
            key = occupied.get((c, r))
            if key is None:
                text = "·"
            else:
                text = (key[:cell - 2] + "..") if len(key) > cell - 1 else key
            cells.append(f"{text:^{cell}}")
        mark = "★" if r == 0 else " "
        print(f"row {r:>3}{mark}" + "".join(cells))
    print(" " * 6 + "".join(f"{c:^{cell}}" for c in range(c0, c1 + 1)))

    dup = {}
    for e in items:
        dup.setdefault((e["col"], e["row"]), []).append(e["key"])
    conflicts = {k: v for k, v in dup.items() if len(v) > 1}
    if conflicts:
        print("坐标冲突：", conflicts)


def main():
    entries = load_entries()
    print(f"共 {len(entries)} 条研究\n")
    print_table(entries)
    for category in sorted({e["category"] for e in entries}):
        print_grid(entries, category)


if __name__ == "__main__":
    main()
