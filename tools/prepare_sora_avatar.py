"""Prepare the licensed Alina source model as AirGF's long-haired Sora avatar.

Run with:
  blender --background --python tools/prepare_sora_avatar.py -- <input.glb> <output.glb>
"""

import sys
from pathlib import Path

import bpy
from mathutils import Matrix, Vector


HAIR_MATERIAL_NAMES = {
    "Hair_MAT_Transparency",
    "Skullcap_MAT_Transparency",
    "Hair_Transparency",
}


def reset_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete()


def extend_weighted_hair():
    for obj in bpy.context.scene.objects:
        if obj.type != "MESH":
            continue
        material_names = {material.name for material in obj.data.materials if material}
        if "Hair_MAT_Transparency" not in material_names:
            continue

        inverse_world = obj.matrix_world.inverted()
        for vertex in obj.data.vertices:
            world = obj.matrix_world @ vertex.co
            if world.z < 3.18:
                # Preserve the crown and bangs while extending the existing
                # skinned side/back layers smoothly to the upper waist.
                extension = (3.18 - world.z) * 1.72
                world.z = 3.18 - extension
                falloff = min(extension / 1.05, 1.0)
                world.x *= 1.0 - 0.05 * falloff
                vertex.co = inverse_world @ world

        obj.data.update()

    for material in bpy.data.materials:
        if material.name not in HAIR_MATERIAL_NAMES:
            continue
        material.diffuse_color = (0.012, 0.009, 0.008, 1.0)
        if material.use_nodes:
            shader = material.node_tree.nodes.get("Principled BSDF")
            if shader is not None:
                shader.inputs["Base Color"].default_value = (0.012, 0.009, 0.008, 1.0)


def attach_long_hair(hair_source_path: Path, armature):
    existing_objects = set(bpy.context.scene.objects)
    bpy.ops.import_scene.gltf(filepath=str(hair_source_path))
    imported_objects = set(bpy.context.scene.objects) - existing_objects
    hair = next(
        obj for obj in imported_objects
        if obj.type == "MESH" and obj.name.startswith("long01Mesh")
    )

    bpy.context.scene.frame_set(0)
    evaluated_hair = hair.evaluated_get(bpy.context.evaluated_depsgraph_get())
    evaluated_mesh = evaluated_hair.to_mesh()
    source_points = [
        (evaluated_hair.matrix_world @ vertex.co).copy()
        for vertex in evaluated_mesh.vertices
    ]
    evaluated_hair.to_mesh_clear()
    source_center_y = (min(point.y for point in source_points) + max(point.y for point in source_points)) * 0.5
    source_top = max(point.z for point in source_points)
    for vertex, source in zip(hair.data.vertices, source_points):
        vertex.co = Vector(
            (
                source.x * 0.215,
                (source.y - source_center_y) * 0.21 + 0.040,
                (source.z - source_top) * 0.20 + 3.48,
            )
        )
    hair.matrix_world = Matrix.Identity(4)
    hair.name = "Sora_Long_Black_Hair"
    hair.data.name = "Sora_Long_Black_Hair_Mesh"

    for modifier in list(hair.modifiers):
        hair.modifiers.remove(modifier)
    for group in list(hair.vertex_groups):
        hair.vertex_groups.remove(group)
    head_group = hair.vertex_groups.new(name="CC_Base_Head_038")
    chest_group = hair.vertex_groups.new(name="CC_Base_Spine02_035")
    waist_group = hair.vertex_groups.new(name="CC_Base_Spine01_034")
    for vertex in hair.data.vertices:
        z = vertex.co.z
        if z >= 2.90:
            head_group.add([vertex.index], 1.0, "REPLACE")
        elif z >= 2.55:
            head_weight = (z - 2.55) / 0.35
            head_group.add([vertex.index], head_weight, "REPLACE")
            chest_group.add([vertex.index], 1.0 - head_weight, "REPLACE")
        else:
            chest_weight = max(0.0, min(1.0, (z - 2.35) / 0.20))
            chest_group.add([vertex.index], chest_weight, "REPLACE")
            waist_group.add([vertex.index], 1.0 - chest_weight, "REPLACE")
    modifier = hair.modifiers.new(name="Sora Hair Rig", type="ARMATURE")
    modifier.object = armature
    world_matrix = hair.matrix_world.copy()
    hair.parent = armature
    hair.matrix_world = world_matrix

    for material in hair.data.materials:
        if material is not None:
            material.name = "Sora_Long_Black_Hair_Material"
            material.diffuse_color = (0.006, 0.004, 0.004, 1.0)

    for obj in imported_objects - {hair}:
        bpy.data.objects.remove(obj, do_unlink=True)


def export_avatar(input_path: Path, hair_source_path: Path, output_path: Path):
    reset_scene()
    bpy.ops.import_scene.gltf(filepath=str(input_path))
    for obj in list(bpy.context.scene.objects):
        if obj.type == "MESH" and not obj.data.materials and obj.parent is None:
            bpy.data.objects.remove(obj, do_unlink=True)
    extend_weighted_hair()
    armature = next(obj for obj in bpy.context.scene.objects if obj.type == "ARMATURE")
    attach_long_hair(hair_source_path, armature)
    bpy.ops.export_scene.gltf(
        filepath=str(output_path),
        export_format="GLB",
        export_animations=True,
        export_skins=True,
        export_morph=True,
        export_yup=True,
    )


def main():
    if "--" not in sys.argv:
        raise SystemExit(
            "Usage: blender --background --python tools/prepare_sora_avatar.py -- "
            "<input.glb> <hair-source.glb> <output.glb>"
        )
    args = sys.argv[sys.argv.index("--") + 1 :]
    if len(args) != 3:
        raise SystemExit("Expected base, hair-source, and output GLB paths")
    export_avatar(
        Path(args[0]).resolve(),
        Path(args[1]).resolve(),
        Path(args[2]).resolve(),
    )


if __name__ == "__main__":
    main()
