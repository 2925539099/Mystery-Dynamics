# -*- coding: utf-8 -*-
"""
把 Blockbench 的 java_block 工程转成 Minecraft 方块模型 JSON。

用法：
    python tools/convert_model.py

要点：
- 工程分辨率是 128x128，方块模型的 UV 是 0..16，所以统一除以 8；
- 傀儡身体用 golem_iron 贴图，机械部分用 andesite_belt_casing 贴图；
- 只处理静态几何（本模型没有元素旋转）。
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_BBMODEL = r"C:\Users\LENOVO\Desktop\傀儡动力系统\（傀儡动力系统）golem_body - Converted.bbmodel"
SRC_TEX_DIR = r"C:\Users\LENOVO\Desktop\傀儡动力系统"

RES = os.path.join(ROOT, "src", "main", "resources")
MODEL_OUT = os.path.join(RES, "assets", "golem_power", "models", "block", "golem_dynamo.json")
TEX_OUT_DIR = os.path.join(RES, "assets", "golem_power", "textures", "block")

# 模型里的贴图索引 -> 方块模型材质
TEXTURE_MATERIALS = {
    0: "golem",   # golem_iron.png
    8: "casing",  # andesite_belt_casing.png
}

PROJECT_SIZE = 128  # Blockbench 工程分辨率

FACES = ["north", "east", "south", "west", "up", "down"]


def uv16(v):
    """UV 是 128 像素坐标系，方块模型是 0..16 -> 除以 8。"""
    v = round(float(v) * 16.0 / PROJECT_SIZE, 4)
    return 0.0 if v == 0 else v


def pos(v):
    """from / to 已经是方块空间的 0..16，只需要去掉 -0.0。"""
    v = round(float(v), 4)
    return 0.0 if v == 0 else v


def main():
    data = json.load(open(SRC_BBMODEL, encoding="utf-8"))
    elements = []
    for el in data["elements"]:
        # Blockbench 的 inflate：正数外扩、负数内缩；方块模型没有 inflate 字段，
        # 导出时直接折算进 from/to（这一点决定了傀儡在机壳里的实际大小）。
        inflate = float(el.get("inflate", 0))
        frm = [pos(el["from"][i] - inflate) for i in range(3)]
        to = [pos(el["to"][i] + inflate) for i in range(3)]

        faces = {}
        for name in FACES:
            face = el["faces"][name]
            tex = face["texture"]
            material = TEXTURE_MATERIALS.get(tex)
            if material is None:
                raise RuntimeError(f"未映射的贴图索引 {tex}（元素 {el['name']}）")
            faces[name] = {
                "uv": [uv16(v) for v in face["uv"]],
                "texture": f"#{material}",
            }
        elements.append({
            "from": frm,
            "to": to,
            "faces": faces,
        })

    model = {
        "credit": "Converted from Blockbench project (傀儡动力系统)",
        "ambientocclusion": bool(data.get("ambientocclusion", True)),
        "textures": {
            "particle": "golem_power:block/golem_iron",
            "golem": "golem_power:block/golem_iron",
            "casing": "golem_power:block/andesite_belt_casing",
        },
        "elements": elements,
    }
    # 手持 / 物品栏的显示变换（Blockbench 里已经调好了）
    if data.get("display"):
        model["display"] = data["display"]

    os.makedirs(os.path.dirname(MODEL_OUT), exist_ok=True)
    os.makedirs(TEX_OUT_DIR, exist_ok=True)
    with open(MODEL_OUT, "w", encoding="utf-8") as f:
        json.dump(model, f, ensure_ascii=False, indent=1)

    # 复制用到的贴图
    import shutil
    for src, dst in [
        ("golem_iron.png", "golem_iron.png"),
        ("andesite_belt_casing.png", "andesite_belt_casing.png"),
    ]:
        shutil.copyfile(os.path.join(SRC_TEX_DIR, src), os.path.join(TEX_OUT_DIR, dst))

    print("model ->", MODEL_OUT)
    print("elements:", len(elements))


if __name__ == "__main__":
    main()


