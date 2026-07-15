# Avatar asset provenance

Five release avatars (`Maya`, `Nia`, `Leo`, `Marcus`, and `Kai`) were generated
locally with MakeHuman Community 1.3.0 from the bundled MakeHuman base mesh,
game-engine rig, hair, clothing, and texture assets. MakeHuman's bundled assets
and exported character meshes are released under CC0.

- Source: https://github.com/makehumancommunity/makehuman-assets
- Asset license: https://github.com/makehumancommunity/makehuman/blob/master/LICENSE.ASSETS.md
- Generator: MakeHuman Community 1.3.0
- Converter: FBX2glTF 0.9.7 (BSD-3-Clause)
- Generated: 2026-07-14

`Sora` uses **Alina Ip, realistic Asian woman (Animated)** by **Jungle Jim**,
redistributed under the Creative Commons Attribution 4.0 license. The original
model provides the realistic Asian adult likeness, clothing, textures, facial
rig, dark hair, and body rig. `tools/extend_sora_hair.py` lengthens the existing
skinned hair POSITION accessor without re-exporting the GLB, preserving the
source character's animation, skin bind matrices, materials, and attribution.

- Original: https://sketchfab.com/3d-models/alina-ip-realistic-asian-woman-animated-600d4d4aa71c4181b2567a3605ce8c57
- Author: https://sketchfab.com/jungle_jim
- License: https://creativecommons.org/licenses/by/4.0/

The prior unverified GLB catalog is intentionally excluded from the release
bundle. Do not add a model to `VisualTemplate` with `releaseEligible = true`
without recording its source and redistribution license here.
