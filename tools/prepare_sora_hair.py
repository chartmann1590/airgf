"""Build Sora's CC0 long-hair attachment in avatar-local space.

Run with:
  blender --background --python tools/prepare_sora_hair.py -- \
    <alina.glb> <makehuman-donor.glb> <output.glb>
"""

import sys
from pathlib import Path

import bpy
from mathutils import Vector


def reset_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete()


def build_hair(target_path: Path, donor_path: Path, output_path: Path):
    reset_scene()
    bpy.ops.import_scene.gltf(filepath=str(target_path))
    bpy.context.scene.frame_set(0)
    armature = next(obj for obj in bpy.context.scene.objects if obj.type == "ARMATURE")
    head_bone = armature.data.bones.get("CC_Base_Head_038")
    if head_bone is None:
        raise RuntimeError("Target avatar is missing CC_Base_Head_038")
    head_world_inverse = (armature.matrix_world @ head_bone.matrix_local).inverted()
    reset_scene()
    bpy.ops.import_scene.gltf(filepath=str(donor_path))
    bpy.context.scene.frame_set(0)
    donor_hair = next(obj for obj in bpy.context.scene.objects if obj.name.startswith("long01Mesh"))
    evaluated_hair = donor_hair.evaluated_get(bpy.context.evaluated_depsgraph_get())
    evaluated_mesh = evaluated_hair.to_mesh()
    hair_mesh = evaluated_mesh.copy()
    source_points = [
        (evaluated_hair.matrix_world @ vertex.co).copy()
        for vertex in evaluated_mesh.vertices
    ]
    evaluated_hair.to_mesh_clear()

    source_center_y = (min(point.y for point in source_points) + max(point.y for point in source_points)) * 0.5
    source_top = max(point.z for point in source_points)
    for vertex, source in zip(hair_mesh.vertices, source_points):
        target_world = Vector(
            (
                source.x * 0.215,
                (source.y - source_center_y) * 0.21 + 0.040,
                (source.z - source_top) * 0.20 + 3.48,
            )
        )
        vertex.co = head_world_inverse @ target_world

    reset_scene()
    hair = bpy.data.objects.new("Sora_Long_Black_Hair", hair_mesh)
    bpy.context.collection.objects.link(hair)
    for material in hair_mesh.materials:
        if material is not None:
            material.name = "Sora_Long_Black_Hair_Material"
            material.diffuse_color = (0.006, 0.004, 0.004, 1.0)
            if material.use_nodes:
                principled = next(
                    (node for node in material.node_tree.nodes if node.type == "BSDF_PRINCIPLED"),
                    None,
                )
                if principled is not None:
                    principled.inputs["Base Color"].default_value = (0.006, 0.004, 0.004, 1.0)
                    principled.inputs["Roughness"].default_value = 0.38
    hair.select_set(True)
    bpy.context.view_layer.objects.active = hair
    bpy.ops.export_scene.gltf(
        filepath=str(output_path),
        export_format="GLB",
        use_selection=True,
        export_animations=False,
        export_skins=False,
        export_yup=True,
    )


def main():
    if "--" not in sys.argv:
        raise SystemExit(
            "Usage: blender --background --python tools/prepare_sora_hair.py -- "
            "<target.glb> <donor.glb> <output.glb>"
        )
    args = sys.argv[sys.argv.index("--") + 1 :]
    if len(args) != 3:
        raise SystemExit("Expected target, donor, and output GLB paths")
    build_hair(Path(args[0]).resolve(), Path(args[1]).resolve(), Path(args[2]).resolve())


if __name__ == "__main__":
    main()
