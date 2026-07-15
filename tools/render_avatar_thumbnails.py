import math
import sys
from pathlib import Path

import bpy
from mathutils import Vector


THUMBNAILS = {
    "ariadna.glb": "thumb_ariadna.webp",
    "gracy_lee.glb": "thumb_gracy_lee.webp",
    "catwoman.glb": "thumb_catwoman.webp",
    "indian_woman_in_saree.glb": "thumb_priya.webp",
    "alina_ip_realistic_asian_woman_animated.glb": "thumb_alina.webp",
    "realistic_woman_walking_animated.glb": "thumb_natasha.webp",
    "short-hair-red-head-american.glb": "thumb_ruby.webp",
    "sophia_animated_003_-_animated_3d_woman.glb": "thumb_sophia.webp",
    "bouncy_beach_babe_walking_in_bikini_animated.glb": "thumb_beach_babe.webp",
    "indian_office_woman.glb": "thumb_nisha_office.webp",
    "indian_teenager__woman_in__hoodie.glb": "thumb_indian_hoodie.webp",
    "hayley_smith.glb": "thumb_hayley_smith.webp",
    "woman_rumba_dancing_-_mulher_dancando_rumba.glb": "thumb_rumba.webp",
    "realistic_female_character__game-ready_3d_model.glb": "thumb_elena.webp",
}


def reset_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete()


def mesh_bounds():
    points = []
    for obj in bpy.context.scene.objects:
        if obj.type == "MESH":
            points.extend(obj.matrix_world @ Vector(corner) for corner in obj.bound_box)
    if not points:
        raise RuntimeError("No mesh bounds found")
    min_corner = Vector((min(p.x for p in points), min(p.y for p in points), min(p.z for p in points)))
    max_corner = Vector((max(p.x for p in points), max(p.y for p in points), max(p.z for p in points)))
    return min_corner, max_corner


def look_at(obj, target):
    direction = target - obj.location
    obj.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()


def setup_scene(min_corner, max_corner, full_body=False):
    center = (min_corner + max_corner) * 0.5
    size = max_corner - min_corner
    height = max(size.z, 0.01)
    depth = max(size.y, 0.01)

    target = center if full_body else Vector((center.x, center.y, min_corner.z + height * 0.79))
    camera_distance = max(height * 1.8, depth * 2.6, 2.0)
    camera_location = Vector((center.x, min_corner.y - camera_distance, target.z))

    camera_data = bpy.data.cameras.new("AvatarThumbnailCamera")
    camera = bpy.data.objects.new("AvatarThumbnailCamera", camera_data)
    bpy.context.collection.objects.link(camera)
    camera.location = camera_location
    look_at(camera, target)
    camera_data.type = "ORTHO"
    camera_data.ortho_scale = height * (1.08 if full_body else 0.42)
    bpy.context.scene.camera = camera

    key_data = bpy.data.lights.new("KeyLight", "AREA")
    key = bpy.data.objects.new("KeyLight", key_data)
    bpy.context.collection.objects.link(key)
    key.location = Vector((center.x - height * 0.25, min_corner.y - camera_distance * 0.7, max_corner.z + height * 0.12))
    key_data.energy = 1800
    key_data.size = max(height * 1.0, 1.0)

    fill_data = bpy.data.lights.new("FillLight", "POINT")
    fill = bpy.data.objects.new("FillLight", fill_data)
    bpy.context.collection.objects.link(fill)
    fill.location = Vector((center.x + height * 0.55, min_corner.y - camera_distance * 0.8, target.z))
    fill_data.energy = 500

    sun_data = bpy.data.lights.new("FrontSun", "SUN")
    sun = bpy.data.objects.new("FrontSun", sun_data)
    bpy.context.collection.objects.link(sun)
    sun.location = Vector((center.x, min_corner.y - camera_distance, max_corner.z))
    look_at(sun, target)
    sun_data.energy = 1.2


def render_thumbnail(model_path, output_path, full_body=False):
    reset_scene()
    bpy.ops.import_scene.gltf(filepath=str(model_path))
    bpy.context.scene.frame_set(0)
    min_corner, max_corner = mesh_bounds()
    setup_scene(min_corner, max_corner, full_body)

    scene = bpy.context.scene
    scene.render.engine = "BLENDER_EEVEE"
    scene.render.resolution_x = 512
    scene.render.resolution_y = 512
    scene.render.film_transparent = False
    scene.world = scene.world or bpy.data.worlds.new("World")
    scene.world.color = (0.12, 0.12, 0.14)
    scene.view_settings.view_transform = "AgX"
    scene.view_settings.look = "AgX - Medium High Contrast"
    scene.view_settings.exposure = 0
    scene.view_settings.gamma = 1
    scene.render.image_settings.file_format = "WEBP"
    scene.render.image_settings.color_mode = "RGB"
    scene.render.image_settings.quality = 86
    scene.render.filepath = str(output_path)
    bpy.ops.render.render(write_still=True)


def main():
    if "--" not in sys.argv:
        raise SystemExit(
            "Usage: blender --background --python tools/render_avatar_thumbnails.py -- "
            "<models_dir> OR <model.glb> <output.webp>"
        )
    args = sys.argv[sys.argv.index("--") + 1 :]
    if len(args) in (2, 3):
        render_thumbnail(Path(args[0]).resolve(), Path(args[1]).resolve(), len(args) == 3 and args[2] == "full")
        return
    models_dir = Path(args[0]).resolve()

    for model_name, thumbnail_name in THUMBNAILS.items():
        model_path = models_dir / model_name
        output_path = models_dir / thumbnail_name
        if not model_path.exists():
            raise FileNotFoundError(model_path)
        print(f"Rendering {thumbnail_name} from {model_name}")
        render_thumbnail(model_path, output_path)


if __name__ == "__main__":
    main()
