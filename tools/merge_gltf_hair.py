"""Merge a donor hair mesh into an existing GLB without re-exporting its body.

This preserves the target character's normals, materials, skeleton, and bind
poses exactly. Both inputs must use the same MakeHuman joint order.
"""

from copy import deepcopy
import json
from pathlib import Path
import struct
import sys


JSON_CHUNK = 0x4E4F534A
BIN_CHUNK = 0x004E4942


def read_glb(path: Path) -> tuple[dict, bytes]:
    data = path.read_bytes()
    magic, version, total_length = struct.unpack_from("<III", data, 0)
    if magic != 0x46546C67 or version != 2 or total_length != len(data):
        raise ValueError(f"Invalid GLB: {path}")
    offset = 12
    document = None
    binary = b""
    while offset < len(data):
        length, chunk_type = struct.unpack_from("<II", data, offset)
        offset += 8
        chunk = data[offset : offset + length]
        offset += length
        if chunk_type == JSON_CHUNK:
            document = json.loads(chunk.decode("utf-8").rstrip(" \0"))
        elif chunk_type == BIN_CHUNK:
            binary = chunk
    if document is None:
        raise ValueError(f"GLB has no JSON chunk: {path}")
    return document, binary


def write_glb(path: Path, document: dict, binary: bytes) -> None:
    json_bytes = json.dumps(document, separators=(",", ":")).encode("utf-8")
    json_bytes += b" " * ((4 - len(json_bytes) % 4) % 4)
    binary += b"\0" * ((4 - len(binary) % 4) % 4)
    total = 12 + 8 + len(json_bytes) + 8 + len(binary)
    output = bytearray(struct.pack("<III", 0x46546C67, 2, total))
    output.extend(struct.pack("<II", len(json_bytes), JSON_CHUNK))
    output.extend(json_bytes)
    output.extend(struct.pack("<II", len(binary), BIN_CHUNK))
    output.extend(binary)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(output)


def remap_texture_info(value: dict, texture_offset: int) -> None:
    if "index" in value:
        value["index"] += texture_offset


def find_node(document: dict, token: str) -> tuple[int, dict]:
    return next(
        (index, node)
        for index, node in enumerate(document["nodes"])
        if token.lower() in node.get("name", "").lower()
    )


if len(sys.argv) != 4:
    raise SystemExit("Usage: merge_gltf_hair.py <target.glb> <donor.glb> <output.glb>")

target_path, donor_path, output_path = map(lambda value: Path(value).resolve(), sys.argv[1:])
target, target_binary = read_glb(target_path)
donor, donor_binary = read_glb(donor_path)

target_binary += b"\0" * ((4 - len(target_binary) % 4) % 4)
donor_binary_offset = len(target_binary)
target_binary += donor_binary

buffer_view_offset = len(target.get("bufferViews", []))
accessor_offset = len(target.get("accessors", []))
image_offset = len(target.get("images", []))
sampler_offset = len(target.get("samplers", []))
texture_offset = len(target.get("textures", []))
material_offset = len(target.get("materials", []))
mesh_offset = len(target.get("meshes", []))

for view in deepcopy(donor.get("bufferViews", [])):
    view["buffer"] = 0
    view["byteOffset"] = view.get("byteOffset", 0) + donor_binary_offset
    target.setdefault("bufferViews", []).append(view)

for accessor in deepcopy(donor.get("accessors", [])):
    if "bufferView" in accessor:
        accessor["bufferView"] += buffer_view_offset
    target.setdefault("accessors", []).append(accessor)

for image in deepcopy(donor.get("images", [])):
    if "bufferView" in image:
        image["bufferView"] += buffer_view_offset
    target.setdefault("images", []).append(image)

target.setdefault("samplers", []).extend(deepcopy(donor.get("samplers", [])))
for texture in deepcopy(donor.get("textures", [])):
    if "source" in texture:
        texture["source"] += image_offset
    if "sampler" in texture:
        texture["sampler"] += sampler_offset
    target.setdefault("textures", []).append(texture)

for material in deepcopy(donor.get("materials", [])):
    pbr = material.get("pbrMetallicRoughness", {})
    for key in ("baseColorTexture", "metallicRoughnessTexture"):
        if key in pbr:
            remap_texture_info(pbr[key], texture_offset)
    for key in ("normalTexture", "occlusionTexture", "emissiveTexture"):
        if key in material:
            remap_texture_info(material[key], texture_offset)
    target.setdefault("materials", []).append(material)

for mesh in deepcopy(donor.get("meshes", [])):
    for primitive in mesh.get("primitives", []):
        primitive["attributes"] = {
            name: accessor + accessor_offset
            for name, accessor in primitive.get("attributes", {}).items()
        }
        if "indices" in primitive:
            primitive["indices"] += accessor_offset
        if "material" in primitive:
            primitive["material"] += material_offset
        for morph_target in primitive.get("targets", []):
            for name in list(morph_target):
                morph_target[name] += accessor_offset
    target.setdefault("meshes", []).append(mesh)

donor_hair_index, donor_hair_node = find_node(donor, "long01")
target_body_index, target_body_node = find_node(target, "soraMesh")
_, target_cap_node = find_node(target, "bob02")

hair_node = {
    key: deepcopy(value)
    for key, value in donor_hair_node.items()
    if key in {"matrix", "rotation", "scale", "translation", "weights"}
}
hair_node.update(
    name="longBlackHairMesh",
    mesh=donor_hair_node["mesh"] + mesh_offset,
    skin=target_body_node["skin"],
)
hair_node_index = len(target["nodes"])
target["nodes"].append(hair_node)

parent = next(
    node for node in target["nodes"]
    if target_body_index in node.get("children", [])
)
parent.setdefault("children", []).append(hair_node_index)

# Tint both fitted cap and long strands black while retaining texture detail.
black_tint = [0.035, 0.04, 0.05, 1.0]
for node in (target_cap_node, hair_node):
    mesh = target["meshes"][node["mesh"]]
    for primitive in mesh.get("primitives", []):
        material = target["materials"][primitive["material"]]
        pbr = material.setdefault("pbrMetallicRoughness", {})
        pbr["baseColorFactor"] = black_tint
        pbr["metallicFactor"] = 0.0
        pbr["roughnessFactor"] = 0.48

target.setdefault("buffers", [{"byteLength": 0}])[0]["byteLength"] = len(target_binary)
target["extensionsUsed"] = sorted(
    set(target.get("extensionsUsed", [])) | set(donor.get("extensionsUsed", []))
)
write_glb(output_path, target, target_binary)
print(f"Merged {donor['nodes'][donor_hair_index].get('name')} into {output_path}")
print(f"Output bytes: {output_path.stat().st_size}")
