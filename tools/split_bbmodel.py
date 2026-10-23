#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把一个跨多个方块的 Blockbench .bbmodel 拆成逐方块模型。

用法示例：

    python tools/split_bbmodel.py big_machine.bbmodel \
        --out src/main/resources \
        --namespace mystery_dynamics \
        --prefix big_machine

输出：

    assets/<ns>/models/block/<prefix>_000.json ...
    assets/<ns>/blockstates/<prefix>_000.json ...
    assets/<ns>/textures/block/*.png
    blueprint/<prefix>_blueprint.json
    blueprint/<prefix>_blueprint.csv
    blueprint/<prefix>_report.json

说明：
- 方块模型坐标只能是 0..16；本脚本按 16 像素网格把元素切开。
- 默认自动拆分跨格元素，并按面方向裁剪 UV。
- 带旋转的元素/面如果跨格，会退回“放到中心格”并给出警告；
  单轴旋转会转换成 block model 的 rotation。
- 纹理可以是 bbmodel 内嵌 base64，也可以是同目录下的相对路径。
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import math
import os
import re
import sys
from pathlib import Path

BLOCK = 16.0
EPS = 1e-6

# face -> (u_axis, v_axis, u_inverted, v_inverted)
FACE_AXES = {
    "north": ("x", "y", True, True),
    "south": ("x", "y", False, True),
    "west": ("z", "y", False, True),
    "east": ("z", "y", True, True),
    "up": ("x", "z", False, False),
    "down": ("x", "z", False, True),
}


def warn(msg: str) -> None:
    print(f"[warn] {msg}", file=sys.stderr)


def sanitize(name: str, fallback: str) -> str:
    name = os.path.basename(name or fallback)
    name = re.sub(r"[^A-Za-z0-9_.-]+", "_", name).strip("._")
    return name or fallback


def parse_vec(value, default):
    if value is None:
        return list(default)
    return [float(v) for v in value]


def cell_range(lo: float, hi: float):
    """元素在某一轴上覆盖的方块格范围（闭区间）。"""
    c0 = math.floor(lo / BLOCK)
    c1 = math.floor((hi - EPS) / BLOCK)
    return c0, c1


def fraction(lo: float, hi: float, clo: float, chi: float, inverted: bool):
    span = hi - lo
    if abs(span) < EPS:
        return 0.0, 1.0
    a = (clo - lo) / span
    b = (chi - lo) / span
    if inverted:
        a, b = 1.0 - b, 1.0 - a
    return a, b


def crop_uv(uv, from_, to, clipped_from, clipped_to, face: str):
    """按被裁剪后的世界坐标，取原 UV 的子矩形。"""
    u_axis, v_axis, u_inv, v_inv = FACE_AXES[face]
    idx = {"x": 0, "y": 1, "z": 2}
    ua, va = idx[u_axis], idx[v_axis]

    fu0, fu1 = fraction(from_[ua], to[ua], clipped_from[ua], clipped_to[ua], u_inv)
    fv0, fv1 = fraction(from_[va], to[va], clipped_from[va], clipped_to[va], v_inv)

    u1, v1, u2, v2 = [float(x) for x in uv]
    nu1 = u1 + (u2 - u1) * fu0
    nu2 = u1 + (u2 - u1) * fu1
    nv1 = v1 + (v2 - v1) * fv0
    nv2 = v1 + (v2 - v1) * fv1
    return [round(nu1, 4), round(nv1, 4), round(nu2, 4), round(nv2, 4)]


def has_face_rotation(element) -> bool:
    return any(float(f.get("rotation", 0) or 0) != 0 for f in element.get("faces", {}).values())


def rotation_info(element):
    rot = parse_vec(element.get("rotation"), [0, 0, 0])
    nonzero = [(axis, rot[i]) for i, axis in enumerate(("x", "y", "z")) if abs(rot[i]) > EPS]
    return nonzero, parse_vec(element.get("origin"), [0, 0, 0])


class Splitter:
    def __init__(self, args):
        self.args = args
        self.ns = args.namespace
        self.prefix = args.prefix
        self.part_block = args.part_block
        self.origin = [float(v) for v in args.origin.split(",")] if isinstance(args.origin, str) else [float(v) for v in args.origin]
        self.out = Path(args.out)
        self.model_dir = self.out / "assets" / self.ns / "models" / "block"
        self.state_dir = self.out / "assets" / self.ns / "blockstates"
        self.tex_dir = self.out / "assets" / self.ns / "textures" / "block"
        self.bp_dir = self.out / args.blueprint_dir
        for d in (self.model_dir, self.state_dir, self.tex_dir, self.bp_dir):
            d.mkdir(parents=True, exist_ok=True)

        self.tex_map = {}        # bbmodel texture index -> "ns:block/name"
        self.tex_written = {}    # content hash -> resource path
        self.model_by_hash = {}  # hash -> model name
        self.models = 0
        self.placements = {}     # (x,y,z) -> model name
        self.cell_elements = {}  # (x,y,z) -> [element]
        self.pieces = 0
        self.warnings = []

    def warn(self, msg: str):
        self.warnings.append(msg)
        warn(msg)

    # ---------- textures ----------

    def load_textures(self, bb, bb_path: Path):
        resolution = bb.get("resolution") or {}
        self.res_w = float(resolution.get("width") or 16)
        self.res_h = float(resolution.get("height") or 16)

        for i, tex in enumerate(bb.get("textures") or []):
            name = sanitize(tex.get("name") or f"texture_{i}", f"texture_{i}")
            if not name.lower().endswith(".png"):
                name += ".png"
            source = tex.get("source")
            data = None
            if isinstance(source, str) and source.startswith("data:image"):
                try:
                    data = base64.b64decode(source.split(",", 1)[1])
                except Exception as exc:  # noqa: BLE001
                    self.warn(f"纹理 {name} base64 解码失败：{exc}")
                    continue
            elif isinstance(source, str) and source:
                rel = source.replace("\\", "/")
                candidate = (bb_path.parent / rel).resolve()
                if candidate.is_file():
                    data = candidate.read_bytes()
                else:
                    self.warn(f"纹理 {name} 引用的文件不存在：{rel}")
                    continue
            if not data:
                self.warn(f"纹理 {name} 没有 source，已跳过")
                continue

            digest = hashlib.sha1(data).hexdigest()
            if digest in self.tex_written:
                self.tex_map[i] = self.tex_written[digest]
                continue

            # 同名但内容不同：加后缀，避免互相覆盖
            stem = Path(name).stem
            suffix = Path(name).suffix or ".png"
            final = f"{stem}{suffix}"
            n = 2
            while (self.tex_dir / final).exists():
                final = f"{stem}_{n}{suffix}"
                n += 1
            (self.tex_dir / final).write_bytes(data)
            ref = f"{self.ns}:block/{stem if final == name else Path(final).stem}"
            self.tex_written[digest] = ref
            self.tex_map[i] = ref

    def scaled_uv(self, element, face: dict):
        uv = face.get("uv")
        if not uv:
            return [0.0, 0.0, self.res_w, self.res_h]
        uv = [float(v) for v in uv]
        # Blockbench 的 UV 是纹理像素；block model 用 0..16。
        max_uv = max(uv)
        if self.res_w > 16 and max_uv > 16.0001:
            uv = [v * 16.0 / self.res_w for v in uv]
        return uv

    def texture_ref(self, element, face: dict):
        idx = face.get("texture", 0)
        try:
            idx = int(idx)
        except (TypeError, ValueError):
            idx = 0
        return self.tex_map.get(idx)

    # ---------- model generation ----------

    def make_face(self, element, face_name, face, uv, rotation=None):
        ref = self.texture_ref(element, face)
        if not ref:
            self.warn(f"元素 {element.get('name', '?')} 的面 {face_name} 引用了不存在的纹理，已跳过")
            return None
        out = {
            "uv": uv,
            "texture": "#" + self.texture_key(ref),
        }
        if rotation:
            out["rotation"] = int(rotation)
        tint = face.get("tintindex", face.get("tint_index"))
        if tint not in (None, 0):
            out["tintindex"] = int(tint)
        return out

    def texture_key(self, ref: str) -> str:
        # 用于 textures 映射的稳定 key
        return "t_" + re.sub(r"[^A-Za-z0-9_]+", "_", ref.split(":", 1)[-1])

    def build_element(self, element, clipped_from, clipped_to, cell, uv_crop: bool):
        faces = {}
        rot_nonzero, rot_origin = rotation_info(element)
        face_rotation = has_face_rotation(element)

        for face_name in ("north", "south", "west", "east", "up", "down"):
            face = element.get("faces", {}).get(face_name)
            if not face:
                continue
            uv = self.scaled_uv(element, face)
            if uv_crop:
                # element 的 from/to 是模型原点坐标；clipped_from/to 是世界坐标。
                local_from = [float(v) for v in element["from"]]
                local_to = [float(v) for v in element["to"]]
                crop_from = [clipped_from[i] - self.origin[i] for i in range(3)]
                crop_to = [clipped_to[i] - self.origin[i] for i in range(3)]
                uv = crop_uv(uv, local_from, local_to, crop_from, crop_to, face_name)
            rotation = face.get("rotation")
            built = self.make_face(element, face_name, face, uv, rotation)
            if built:
                faces[face_name] = built

        if not faces:
            return None

        local_from = [round(clipped_from[i] - cell[i] * BLOCK, 4) for i in range(3)]
        local_to = [round(clipped_to[i] - cell[i] * BLOCK, 4) for i in range(3)]
        out = {"from": local_from, "to": local_to, "faces": faces}
        if element.get("shade") is False:
            out["shade"] = False

        # 旋转：单轴才转换；多轴 block model 不支持
        if rot_nonzero and not uv_crop:
            if len(rot_nonzero) == 1:
                axis, angle = rot_nonzero[0]
                out["rotation"] = {
                    "origin": [round(rot_origin[i] + self.origin[i] - cell[i] * BLOCK, 4) for i in range(3)],
                    "axis": axis,
                    "angle": round(angle, 4),
                }
            else:
                self.warn(f"元素 {element.get('name', '?')} 有多轴旋转，block model 只支持单轴，已忽略旋转")
        return out

    def store_model(self, elements, textures):
        payload = {
            "parent": "minecraft:block/block",
            "render_type": self.args.render_type,
            "textures": textures,
            "elements": elements,
        }
        raw = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
        digest = hashlib.sha1(raw.encode("utf-8")).hexdigest()

        if not self.args.no_dedup and digest in self.model_by_hash:
            return self.model_by_hash[digest]

        name = f"{self.prefix}_{self.models:03d}"
        self.models += 1
        self.model_by_hash[digest] = name
        (self.model_dir / f"{name}.json").write_text(
            json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        (self.state_dir / f"{name}.json").write_text(
            json.dumps({"variants": {"": {"model": f"{self.ns}:block/{name}"}}},
                       ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        return name

    # ---------- main ----------

    def run(self, bb_path: Path):
        bb = json.loads(bb_path.read_text(encoding="utf-8"))
        self.load_textures(bb, bb_path)

        textures = {"particle": next(iter(self.tex_map.values()), "minecraft:block/stone")}
        for idx, ref in self.tex_map.items():
            textures[self.texture_key(ref)] = ref
        self.textures = textures

        elements = bb.get("elements") or []
        for element in elements:
            if not element.get("export", True):
                continue
            if "from" not in element or "to" not in element:
                continue

            world_from = [float(v) + o for v, o in zip(element["from"], self.origin)]
            world_to = [float(v) + o for v, o in zip(element["to"], self.origin)]
            if any(world_to[i] <= world_from[i] + EPS for i in range(3)):
                self.warn(f"元素 {element.get('name', '?')} 尺寸为 0，已跳过")
                continue

            rot_nonzero, _ = rotation_info(element)
            can_split = (not rot_nonzero) and (not has_face_rotation(element))
            crossed = any(
                cell_range(world_from[i], world_to[i])[0] != cell_range(world_from[i], world_to[i])[1]
                for i in range(3)
            )

            if self.args.no_split or not can_split:
                # 不拆分，或旋转元素无法正确裁剪：放到中心格
                center = [(world_from[i] + world_to[i]) / 2 for i in range(3)]
                cell = tuple(math.floor(center[i] / BLOCK) for i in range(3))
                el = self.build_element(element, world_from, world_to, cell, uv_crop=False)
                if el:
                    self.add_piece(el, textures, cell)
                if crossed:
                    reason = "带旋转" if not can_split else "--no-split"
                    self.warn(f"元素 {element.get('name', '?')} {reason}且跨格，已放到中心格 {cell}，可能溢出")
                continue

            c0 = [cell_range(world_from[i], world_to[i])[0] for i in range(3)]
            c1 = [cell_range(world_from[i], world_to[i])[1] for i in range(3)]
            for cx in range(c0[0], c1[0] + 1):
                for cy in range(c0[1], c1[1] + 1):
                    for cz in range(c0[2], c1[2] + 1):
                        cell = (cx, cy, cz)
                        clipped_from = [
                            max(world_from[0], cx * BLOCK),
                            max(world_from[1], cy * BLOCK),
                            max(world_from[2], cz * BLOCK),
                        ]
                        clipped_to = [
                            min(world_to[0], (cx + 1) * BLOCK),
                            min(world_to[1], (cy + 1) * BLOCK),
                            min(world_to[2], (cz + 1) * BLOCK),
                        ]
                        if any(clipped_to[i] <= clipped_from[i] + EPS for i in range(3)):
                            continue
                        el = self.build_element(element, clipped_from, clipped_to, cell,
                                                uv_crop=self.args.split_uv)
                        if el:
                            self.add_piece(el, textures, cell)

        self.write_blueprint(bb_path, len(elements))
        return 0

    def add_piece(self, el, textures, cell):
        self.cell_elements.setdefault(cell, []).append(el)
        self.pieces += 1

    def write_blueprint(self, bb_path: Path, element_count: int):
        for cell in sorted(self.cell_elements):
            self.placements[cell] = self.store_model(self.cell_elements[cell], self.textures)
        cells = sorted(self.placements)
        if cells:
            mn = [min(c[i] for c in cells) for i in range(3)]
            mx = [max(c[i] for c in cells) for i in range(3)]
        else:
            mn = [0, 0, 0]
            mx = [0, 0, 0]

        unique_models = sorted(set(self.placements.values()))
        model_index = {m: i for i, m in enumerate(unique_models)}

        if self.part_block:
            if len(unique_models) > 16:
                self.warn("唯一模型超过 16 个，单个 part 整数属性可能不够；"
                          "可以拆成多个属性，或改成每个模型一个方块")
            block_name = f"{self.ns}:{self.part_block}"
            variants = {f"part={i}": {"model": f"{self.ns}:block/{m}"}
                        for m, i in model_index.items()}
            (self.state_dir / f"{self.part_block}.json").write_text(
                json.dumps({"variants": variants}, ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8")
            parts = []
            for c in cells:
                model = self.placements[c]
                idx = model_index[model]
                parts.append({
                    "pos": list(c),
                    "model": model,
                    "block": block_name,
                    "part": idx,
                    "blockstate": f"{block_name}[part={idx}]",
                })
            (self.bp_dir / f"{self.prefix}_place.mcfunction").write_text(
                "\n".join(
                    [f"# {self.prefix} placements, origin-relative"]
                    + [f"setblock ~{p['pos'][0]} ~{p['pos'][1]} ~{p['pos'][2]} {p['blockstate']}"
                       for p in parts]
                ) + "\n", encoding="utf-8")
        else:
            parts = [
                {"pos": list(c), "model": self.placements[c],
                 "blockstate": f"{self.ns}:{self.placements[c]}"}
                for c in cells
            ]

        blueprint = {
            "namespace": self.ns,
            "prefix": self.prefix,
            "origin": self.origin,
            "block_size": 16,
            "part_block": self.part_block,
            "size": [mx[i] - mn[i] + 1 for i in range(3)],
            "min": mn,
            "parts": parts,
        }
        (self.bp_dir / f"{self.prefix}_blueprint.json").write_text(
            json.dumps(blueprint, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

        with (self.bp_dir / f"{self.prefix}_blueprint.csv").open("w", encoding="utf-8", newline="") as f:
            f.write("x,y,z,model,part,blockstate\n")
            for p in parts:
                part = p.get("part", "")
                blockstate = p.get("blockstate", "")
                f.write(f"{p['pos'][0]},{p['pos'][1]},{p['pos'][2]},{p['model']},{part},{blockstate}\n")

        report = {
            "input": str(bb_path),
            "namespace": self.ns,
            "prefix": self.prefix,
            "origin": self.origin,
            "part_block": self.part_block,
            "elements": element_count,
            "pieces": self.pieces,
            "unique_models": self.models,
            "placements": len(parts),
            "size": blueprint["size"],
            "warnings": self.warnings,
        }
        (self.bp_dir / f"{self.prefix}_report.json").write_text(
            json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

        print(f"完成：{self.pieces} 个方块片段，{self.models} 个唯一模型，"
              f"{len(parts)} 个放置点，尺寸 {blueprint['size']}")
        print(f"模型：{self.model_dir}")
        print(f"方块状态：{self.state_dir}")
        print(f"蓝图：{self.bp_dir / (self.prefix + '_blueprint.json')}")
        if self.warnings:
            print(f"有 {len(self.warnings)} 条警告，见 report.json", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(description="Blockbench .bbmodel -> 逐方块 block models")
    parser.add_argument("input", help="输入 .bbmodel")
    parser.add_argument("--out", required=True, help="资源根目录，例如 src/main/resources")
    parser.add_argument("--namespace", required=True, help="模组命名空间，例如 mystery_dynamics")
    parser.add_argument("--prefix", required=True, help="输出模型名前缀，例如 big_machine")
    parser.add_argument("--origin", default="0,0,0",
                        help="模型原点偏移（像素），格式 x,y,z，默认 0,0,0")
    parser.add_argument("--block-size", type=float, default=16.0,
                        help="方块尺寸，默认 16（Minecraft 固定 16，一般不用改）")
    parser.add_argument("--render-type", default="minecraft:cutout",
                        help="block model 的 render_type，默认 minecraft:cutout")
    parser.add_argument("--blueprint-dir", default="blueprint",
                        help="蓝图输出目录（相对 --out），默认 blueprint")
    parser.add_argument("--part-block", default=None,
                        help="可选：所有部件共用一个方块时，方块注册名（不含命名空间）。"
                             "会额外生成带 part=0..N 变体的 blockstate 和 setblock 函数")
    parser.add_argument("--no-split", action="store_true",
                        help="不拆分跨格元素（只按中心格放置）")
    parser.add_argument("--no-split-uv", dest="split_uv", action="store_false",
                        help="拆分几何但不裁剪 UV（一般不要用）")
    parser.add_argument("--no-dedup", action="store_true",
                        help="不合并相同模型")
    parser.set_defaults(split_uv=True)

    args = parser.parse_args()
    global BLOCK
    BLOCK = float(args.block_size)
    bb_path = Path(args.input)
    if not bb_path.is_file():
        parser.error(f"找不到输入文件：{bb_path}")

    splitter = Splitter(args)
    return splitter.run(bb_path)


if __name__ == "__main__":
    raise SystemExit(main())
