# -*- coding: utf-8 -*-
"""把方块模型 JSON 渲染成预览图，用来检查 UV/贴图是否对得上。"""
import json
import math
import os

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL = os.path.join(ROOT, "src", "main", "resources", "assets", "golem_power",
                     "models", "block", "golem_dynamo.json")
TEX_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "golem_power", "textures", "block")
OUT = os.path.join(ROOT, "preview_block.png")

FACES = ["north", "east", "south", "west", "up", "down"]


def face_quad(frm, to, name):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    if name == "north":
        return [((x1, y1, z0), (0, 0)), ((x0, y1, z0), (1, 0)),
                ((x0, y0, z0), (1, 1)), ((x1, y0, z0), (0, 1))]
    if name == "south":
        return [((x0, y1, z1), (0, 0)), ((x1, y1, z1), (1, 0)),
                ((x1, y0, z1), (1, 1)), ((x0, y0, z1), (0, 1))]
    if name == "east":
        return [((x1, y1, z1), (0, 0)), ((x1, y1, z0), (1, 0)),
                ((x1, y0, z0), (1, 1)), ((x1, y0, z1), (0, 1))]
    if name == "west":
        return [((x0, y1, z0), (0, 0)), ((x0, y1, z1), (1, 0)),
                ((x0, y0, z1), (1, 1)), ((x0, y0, z0), (0, 1))]
    if name == "up":
        return [((x0, y1, z0), (0, 0)), ((x1, y1, z0), (1, 0)),
                ((x1, y1, z1), (1, 1)), ((x0, y1, z1), (0, 1))]
    if name == "down":
        return [((x0, y0, z1), (0, 0)), ((x1, y0, z1), (1, 0)),
                ((x1, y0, z0), (1, 1)), ((x0, y0, z0), (0, 1))]
    raise KeyError(name)


def load():
    model = json.load(open(MODEL, encoding="utf-8"))
    tex_cache = {}
    quads = []
    for el in model["elements"]:
        for name in FACES:
            face = el["faces"][name]
            mat = face["texture"].lstrip("#")
            path = model["textures"][mat].split(":")[-1].split("/")[-1] + ".png"
            if path not in tex_cache:
                tex_cache[path] = Image.open(os.path.join(TEX_DIR, path)).convert("RGBA")
            tex = tex_cache[path]
            u1, v1, u2, v2 = face["uv"]
            corners = []
            for (pos, (tu, tv)) in face_quad(el["from"], el["to"], name):
                u = u1 + (u2 - u1) * tu
                v = v1 + (v2 - v1) * tv
                corners.append(((pos[0] - 8, pos[1] - 8, pos[2] - 8), (u / 16.0, v / 16.0)))
            quads.append((corners, tex))
    return quads


def render(quads, out_png, size=720, yaw=210, pitch=18, zoom=3.2):
    pts = np.array([p for q in quads for p, _ in q[0]], dtype=float)
    lo, hi = pts.min(0), pts.max(0)
    center = (lo + hi) / 2
    radius = float(np.linalg.norm(hi - lo)) / 2

    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    eye = center + radius * zoom * np.array([sy * cp, sp, cy * cp])
    forward = center - eye
    forward /= np.linalg.norm(forward)
    right = np.cross(forward, [0, 1, 0])
    right /= np.linalg.norm(right)
    up = np.cross(right, forward)

    d = pts - eye
    cam = np.stack([d @ right, d @ up, d @ forward], axis=1)
    sx, sy2 = cam[:, 0] / cam[:, 2], -cam[:, 1] / cam[:, 2]
    half = max(np.abs(sx).max(), np.abs(sy2).max())
    f = size * 0.5 * 0.88 / half
    off_x = -float(sx.min() + sx.max()) * 0.5 * f
    off_y = -float(sy2.min() + sy2.max()) * 0.5 * f

    color = np.zeros((size, size, 3))
    alpha = np.zeros((size, size))
    depth = np.full((size, size), 1e9)
    light = np.array([0.45, 0.85, 0.5])
    light /= np.linalg.norm(light)

    for corners, tex in quads:
        tarr = np.asarray(tex, dtype=float) / 255.0
        th, tw = tarr.shape[:2]

        def project(p):
            dd = np.array(p, dtype=float) - eye
            z = float(dd @ forward)
            return (float(dd @ right) / z * f + size / 2 + off_x,
                    -float(dd @ up) / z * f + size / 2 + off_y, z)

        poly = [project(p) for p, _ in corners]
        a = np.array(corners[0][0], float)
        b = np.array(corners[1][0], float) - a
        c = np.array(corners[2][0], float) - a
        n = np.cross(b, c)
        n /= max(np.linalg.norm(n), 1e-9)
        if np.dot(n, -forward) < 0 and np.dot(n, forward) > 0:
            n = -n
        shade = 0.62 + 0.38 * max(0.0, float(np.dot(n, light)))

        for tri in ((0, 1, 2), (0, 2, 3)):
            p0, p1, p2 = (poly[i] for i in tri)
            if min(p0[2], p1[2], p2[2]) <= 0.01:
                continue
            xs = [p0[0], p1[0], p2[0]]
            ys = [p0[1], p1[1], p2[1]]
            xmin, xmax = int(max(0, math.floor(min(xs)))), int(min(size - 1, math.ceil(max(xs))))
            ymin, ymax = int(max(0, math.floor(min(ys)))), int(min(size - 1, math.ceil(max(ys))))
            if xmin > xmax or ymin > ymax:
                continue
            xs_g, ys_g = np.meshgrid(np.arange(xmin, xmax + 1) + 0.5,
                                     np.arange(ymin, ymax + 1) + 0.5)
            det = (p1[0] - p0[0]) * (p2[1] - p0[1]) - (p2[0] - p0[0]) * (p1[1] - p0[1])
            if abs(det) < 1e-9:
                continue
            w0 = ((p1[0] - xs_g) * (p2[1] - ys_g) - (p2[0] - xs_g) * (p1[1] - ys_g)) / det
            w1 = ((p2[0] - xs_g) * (p0[1] - ys_g) - (p0[0] - xs_g) * (p2[1] - ys_g)) / det
            w2 = 1.0 - w0 - w1
            mask = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
            if not mask.any():
                continue
            iz = w0 / p0[2] + w1 / p1[2] + w2 / p2[2]
            z = 1.0 / iz
            uvx = (w0 * corners[tri[0]][1][0] / p0[2] + w1 * corners[tri[1]][1][0] / p1[2]
                   + w2 * corners[tri[2]][1][0] / p2[2]) * z
            uvy = (w0 * corners[tri[0]][1][1] / p0[2] + w1 * corners[tri[1]][1][1] / p1[2]
                   + w2 * corners[tri[2]][1][1] / p2[2]) * z
            tx = np.clip((uvx * tw).astype(int) % tw, 0, tw - 1)
            ty = np.clip((uvy * th).astype(int) % th, 0, th - 1)
            sub = depth[ymin:ymax + 1, xmin:xmax + 1]
            m = mask & (z < sub) & (tarr[ty, tx, 3] > 0.35)
            if not m.any():
                continue
            sub[m] = z[m]
            color[ymin:ymax + 1, xmin:xmax + 1][m] = tarr[ty, tx, :3][m] * shade
            alpha[ymin:ymax + 1, xmin:xmax + 1][m] = 1.0

    bg = np.zeros((size, size, 3))
    bg[:] = np.linspace(0.94, 0.80, size)[:, None, None]
    out = bg * (1 - alpha[..., None]) + color * alpha[..., None]
    Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8)).save(out_png)
    print("preview ->", out_png)


if __name__ == "__main__":
    render(load(), OUT)
