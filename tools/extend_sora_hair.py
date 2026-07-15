"""Extend Sora's existing skinned hair without re-exporting the avatar.

This edits only the POSITION accessor for the authored hair mesh. Keeping the
original GLB structure intact avoids changing its skin bind matrices.
"""

import json
from pathlib import Path
import struct
import sys


JSON_CHUNK = 0x4E4F534A
BIN_CHUNK = 0x004E4942
HAIR_MESH = "TMDesign_Casual_Hair_55194_Shape_Hair_MAT_Transparency_0"
EXTENSION_START = 173.0
EXTENSION_FACTOR = 1.30


def read_glb(path: Path) -> tuple[dict, bytearray]:
    data = path.read_bytes()
    magic, version, total_length = struct.unpack_from("<III", data, 0)
    if magic != 0x46546C67 or version != 2 or total_length != len(data):
        raise ValueError(f"Invalid GLB: {path}")
    document = None
    binary = None
    offset = 12
    while offset < len(data):
        length, chunk_type = struct.unpack_from("<II", data, offset)
        offset += 8
        chunk = data[offset : offset + length]
        offset += length
        if chunk_type == JSON_CHUNK:
            document = json.loads(chunk.decode("utf-8").rstrip(" \0"))
        elif chunk_type == BIN_CHUNK:
            binary = bytearray(chunk)
    if document is None or binary is None:
        raise ValueError(f"GLB is missing a required chunk: {path}")
    return document, binary


def write_glb(path: Path, document: dict, binary: bytearray) -> None:
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


def extend_hair(input_path: Path, output_path: Path) -> None:
    document, binary = read_glb(input_path)
    mesh = next(mesh for mesh in document["meshes"] if mesh.get("name") == HAIR_MESH)
    primitive = mesh["primitives"][0]
    accessor = document["accessors"][primitive["attributes"]["POSITION"]]
    if accessor["componentType"] != 5126 or accessor["type"] != "VEC3":
        raise ValueError("Hair POSITION accessor must contain float VEC3 values")
    view = document["bufferViews"][accessor["bufferView"]]
    stride = view.get("byteStride", 12)
    start = view.get("byteOffset", 0) + accessor.get("byteOffset", 0)
    points = []
    for index in range(accessor["count"]):
        offset = start + index * stride
        x, y, z = struct.unpack_from("<fff", binary, offset)
        if y < EXTENSION_START:
            distance = EXTENSION_START - y
            y -= distance * EXTENSION_FACTOR
            x *= 0.97
        struct.pack_into("<fff", binary, offset, x, y, z)
        points.append((x, y, z))
    accessor["min"] = [min(point[axis] for point in points) for axis in range(3)]
    accessor["max"] = [max(point[axis] for point in points) for axis in range(3)]

    material = document["materials"][primitive["material"]]
    pbr = material.setdefault("pbrMetallicRoughness", {})
    pbr["baseColorFactor"] = [0.035, 0.03, 0.028, 1.0]
    pbr["roughnessFactor"] = 0.42
    write_glb(output_path, document, binary)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: extend_sora_hair.py <input.glb> <output.glb>")
    extend_hair(Path(sys.argv[1]).resolve(), Path(sys.argv[2]).resolve())
