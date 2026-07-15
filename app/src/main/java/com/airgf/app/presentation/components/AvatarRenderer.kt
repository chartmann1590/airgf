package com.airgf.app.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.airgf.app.animation.AvatarExpressionDriver
import com.airgf.app.animation.AvatarFacePose
import com.airgf.app.animation.BlendShapeLipSync
import com.airgf.app.animation.CharacterAnimationController
import com.airgf.app.data.model.AvatarModelSource
import com.airgf.app.data.model.DownloadState
import com.airgf.app.data.repository.AvatarAssetRepository
import com.airgf.app.domain.model.EmotionState
import com.airgf.app.domain.model.VisualTemplate
import com.airgf.app.presentation.theme.OnSurface
import com.airgf.app.presentation.theme.OnSurfaceVariant
import com.airgf.app.presentation.theme.Primary
import com.airgf.app.tts.LipSyncBridge
import com.airgf.app.R
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberModelInstance
import kotlinx.coroutines.launch

@Composable
fun AvatarRenderer(
    template: VisualTemplate,
    emotion: EmotionState,
    mouthShape: LipSyncBridge.MouthShape,
    assetRepository: AvatarAssetRepository,
    modifier: Modifier = Modifier,
    environmentAssetPath: String? = null,
    action: CharacterAnimationController.AvatarAction = CharacterAnimationController.AvatarAction.IDLE,
) {
    val scope = rememberCoroutineScope()
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(0f, 0.05f, 4.2f)
        lookAt(Position(0f, 0.05f, 0f))
    }
    val mainLightNode = rememberMainLightNode(engine) {
        intensity = 100_000f
    }
    val lipSync = remember { BlendShapeLipSync() }
    val expressionDriver = remember { AvatarExpressionDriver() }
    val facePose = remember(template, emotion, mouthShape) {
        expressionDriver.poseFor(emotion) + lipSync.poseFor(mouthShape)
    }

    var downloadState by remember(template) { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var availabilityVersion by remember(template) { mutableIntStateOf(0) }
    val availability = remember(template, availabilityVersion) {
        assetRepository.resolveAvailability(template)
    }
    val sourcePath = when (val source = availability.source) {
        is AvatarModelSource.BundledAsset -> source.assetPath
        is AvatarModelSource.DownloadedFile -> source.absolutePath
        is AvatarModelSource.ProceduralFallback -> null
    }
    val modelInstance = sourcePath?.let { rememberModelInstance(modelLoader, it) }
    val envModelInstance = if (environmentAssetPath != null) {
        rememberModelInstance(modelLoader, environmentAssetPath)
    } else {
        null
    }

    val idleTransition = rememberInfiniteTransition(label = "avatarIdle")
    val idleLift by idleTransition.animateFloat(
        initialValue = -0.02f,
        targetValue = 0.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "idleLift",
    )
    var avatarNode by remember(template) { mutableStateOf<io.github.sceneview.node.ModelNode?>(null) }
    var rigNodes by remember(template) { mutableStateOf<Map<String, Node>>(emptyMap()) }
    var rigBaseRotations by remember(template) { mutableStateOf<Map<String, Float3>>(emptyMap()) }
    var rigBasePositions by remember(template) { mutableStateOf<Map<String, Float3>>(emptyMap()) }
    var avatarBaseScale by remember(template) { mutableStateOf(Float3(1f, 1f, 1f)) }
    val rigMotionState = remember(template) { RigMotionState() }

    LaunchedEffect(avatarNode, facePose) {
        val node = avatarNode ?: return@LaunchedEffect
        val weights = facePose.toMorphWeights(template.featureSet.blendShapeOrder)
        if (weights.isNotEmpty()) {
            node.renderableNodes.indices.forEach { renderableIndex ->
                runCatching { node.setMorphWeights(weights, renderableIndex) }
            }
        }
    }

    LaunchedEffect(avatarNode, action) {
        val node = avatarNode ?: return@LaunchedEffect
        val names = (0 until node.animationCount).map(node.animator::getAnimationName)
        val hints = when (action) {
            CharacterAnimationController.AvatarAction.WALKING -> listOf("walk", "locomotion")
            CharacterAnimationController.AvatarAction.TALKING -> listOf("talk", "speak", "idle")
            CharacterAnimationController.AvatarAction.WAVING -> listOf("wave", "greet")
            CharacterAnimationController.AvatarAction.DANCING -> listOf("dance", "rumba")
            CharacterAnimationController.AvatarAction.REACTING -> listOf("react", "laugh", "happy", "idle")
            CharacterAnimationController.AvatarAction.IDLE -> listOf("idle", "breath")
        }
        val selected = names.indexOfFirst { name -> hints.any { name.contains(it, ignoreCase = true) } }
            .takeIf { it >= 0 } ?: 0
        if (node.animationCount > 0 && !template.proceduralRigAnimation) {
            (0 until node.animationCount).forEach(node::stopAnimation)
            node.playAnimation(selected, if (action == CharacterAnimationController.AvatarAction.TALKING) 1.12f else 1f, true)
        }
    }

    val backdrop = template.fallbackPalette

    val bgBrush = if (envModelInstance != null) {
        Brush.verticalGradient(listOf(Color.Black, Color.Black))
    } else {
        Brush.verticalGradient(
            listOf(
                Color(backdrop.backdropStartColor),
                Color(backdrop.backdropEndColor),
            ),
        )
    }

    Box(
        modifier = modifier
            .background(bgBrush),
    ) {
        Image(
            painter = painterResource(R.drawable.companion_bedroom),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.12f)),
        )
        if (modelInstance != null) {
            SceneView(
                modifier = Modifier.fillMaxSize(),
                surfaceType = SurfaceType.TextureSurface,
                isOpaque = false,
                engine = engine,
                modelLoader = modelLoader,
                cameraNode = cameraNode,
                mainLightNode = mainLightNode,
                onFrame = { frameTimeNanos ->
                    if (template.proceduralRigAnimation || (avatarNode?.animationCount ?: 0) == 0) {
                        val motion = animateProceduralRig(
                            nodes = rigNodes,
                            baseRotations = rigBaseRotations,
                            basePositions = rigBasePositions,
                            action = action,
                            jawOpen = facePose.jawOpen,
                            frameTimeNanos = frameTimeNanos,
                            motionState = rigMotionState,
                        )
                        applyAuthoredRigPose(
                            nodes = rigNodes,
                            baseRotations = rigBaseRotations,
                            motion = motion,
                        )
                        avatarNode?.position = Position(
                            motion.rootX,
                            template.stageVerticalOffset + motion.rootY,
                            motion.rootZ,
                        )
                        avatarNode?.rotation = Float3(0f, motion.facingYaw, 0f)
                        avatarNode?.scale = avatarBaseScale.scaledBy(motion.scale)
                    } else {
                        val motion = rigMotionState.advance(action, frameTimeNanos)
                        applyAuthoredRigPose(
                            nodes = rigNodes,
                            baseRotations = rigBaseRotations,
                            motion = motion,
                        )
                        avatarNode?.position = Position(
                            motion.rootX,
                            template.stageVerticalOffset + motion.rootY,
                            motion.rootZ,
                        )
                        avatarNode?.rotation = Float3(0f, motion.facingYaw, 0f)
                        avatarNode?.scale = avatarBaseScale.scaledBy(motion.scale)
                    }
                },
            ) {
                if (envModelInstance != null) {
                    ModelNode(
                        modelInstance = envModelInstance,
                        scaleToUnits = 8f,
                        centerOrigin = Position(0f, 0f, 0f),
                    )
                }
                ModelNode(
                    modelInstance = modelInstance,
                    scaleToUnits = if (envModelInstance != null) 1.9f else 2.2f,
                    centerOrigin = if (envModelInstance != null) {
                        Position(0f, 0f, 1.5f)
                    } else Position(0f, 0f, 0f),
                    position = Position(
                        0f,
                        template.stageVerticalOffset,
                        0f,
                    ),
                    autoAnimate = false,
                    apply = {
                        avatarNode = this
                        avatarBaseScale = scale
                        if (rigNodes.isEmpty()) {
                            rigNodes = nodes.mapNotNull { node ->
                                node.name?.let { name -> name to node }
                            }.toMap()
                            rigBaseRotations = rigNodes.mapValues { (_, node) -> node.rotation }
                            rigBasePositions = rigNodes.mapValues { (_, node) -> node.position }
                        }
                    },
                )
            }
        } else {
            FallbackAvatarStage(
                template = template,
                facePose = facePose,
                idleLift = idleLift,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(rigMotionState) {
                    detectTapGestures { offset ->
                        if (size.width > 0 && size.height > 0) {
                            rigMotionState.navigateTo(
                                normalizedX = offset.x / size.width,
                                normalizedY = offset.y / size.height,
                            )
                        }
                    }
                },
        )

        AvatarStatusOverlay(
            template = template,
            downloadState = downloadState,
            statusText = availability.statusMessage,
            showDownloadAction = availability.canDownload && downloadState !is DownloadState.Progress,
            onDownload = {
                scope.launch {
                    assetRepository.downloadModel(template).collect { state ->
                        downloadState = state
                        if (state is DownloadState.Complete) {
                            availabilityVersion++
                        }
                    }
                }
            },
        )
    }
}

private fun animateProceduralRig(
    nodes: Map<String, Node>,
    baseRotations: Map<String, Float3>,
    basePositions: Map<String, Float3>,
    action: CharacterAnimationController.AvatarAction,
    jawOpen: Float,
    frameTimeNanos: Long,
    motionState: RigMotionState,
) : RigMotionFrame {
    if (nodes.isEmpty()) return RigMotionFrame()
    val seconds = frameTimeNanos / 1_000_000_000f
    val motion = motionState.advance(action, frameTimeNanos)
    val weights = motion.weights
    fun weight(value: CharacterAnimationController.AvatarAction) = weights[value.ordinal]

    val idle = weight(CharacterAnimationController.AvatarAction.IDLE)
    val walking = weight(CharacterAnimationController.AvatarAction.WALKING)
    val talking = weight(CharacterAnimationController.AvatarAction.TALKING)
    val waving = weight(CharacterAnimationController.AvatarAction.WAVING)
    val dancing = weight(CharacterAnimationController.AvatarAction.DANCING)
    val reacting = weight(CharacterAnimationController.AvatarAction.REACTING)

    val breath = kotlin.math.sin(seconds * 1.35f)
    val weightShift = kotlin.math.sin(seconds * 0.72f)
    val walkPhase = motion.walkPhase
    val leftStep = kotlin.math.sin(walkPhase)
    val rightStep = kotlin.math.sin(walkPhase + kotlin.math.PI.toFloat())
    val leftKnee = kotlin.math.max(0f, -leftStep)
    val rightKnee = kotlin.math.max(0f, -rightStep)
    val talkPrimary = kotlin.math.sin(seconds * 2.15f)
    val talkSecondary = kotlin.math.sin(seconds * 1.37f + 1.2f)
    val wave = kotlin.math.sin(seconds * 7.8f)
    val danceBeat = kotlin.math.sin(seconds * 4.8f)
    val danceHalf = kotlin.math.sin(seconds * 2.4f + 0.4f)

    fun resolveName(name: String): String? = when {
        nodes.containsKey(name) -> name
        else -> RIG_BONE_ALIASES[name]?.firstOrNull(nodes::containsKey)
    }

    fun pose(name: String, x: Float = 0f, y: Float = 0f, z: Float = 0f) {
        val resolvedName = resolveName(name) ?: return
        val node = nodes[resolvedName] ?: return
        val base = baseRotations[resolvedName] ?: return
        node.rotation = Float3(base.x + x, base.y + y, base.z + z)
    }

    val isValveBiped = nodes.keys.any { it.startsWith("ValveBiped.") }
    val relaxedLeftArm = if (isValveBiped) 0f else -54f
    val relaxedRightArm = if (isValveBiped) 0f else 54f
    val relaxedLeftForearm = if (isValveBiped) 0f else 8f
    val relaxedRightForearm = if (isValveBiped) 0f else -8f

    // All action layers start from the same relaxed stance. Cross-faded weights
    // prevent the skeleton snapping when speech or a user action begins.
    val leftArmSwing = walking * (-leftStep * 25f)
    val rightArmSwing = walking * (-rightStep * 25f)
    val conversationalLeft = talking * (talkPrimary * 7f + talkSecondary * 4f)
    val conversationalRight = talking * (talkPrimary * -4f + talkSecondary * 6f)
    pose(
        "upperarm01.L",
        x = leftArmSwing + conversationalLeft + dancing * danceBeat * 15f,
        y = talking * talkSecondary * 3f,
        z = relaxedLeftArm + talking * 7f + dancing * danceHalf * 14f,
    )
    pose(
        "upperarm01.R",
        x = rightArmSwing + conversationalRight + dancing * -danceBeat * 15f + waving * 18f,
        y = talking * -talkSecondary * 3f + waving * 4f,
        z = relaxedRightArm - talking * 8f - dancing * danceHalf * 14f - waving * 42f,
    )
    pose("lowerarm01.L", x = relaxedLeftForearm + talking * (12f + talkSecondary * 8f) + dancing * 18f)
    pose("lowerarm01.R", x = relaxedRightForearm - talking * (10f + talkPrimary * 7f) + waving * (68f + wave * 12f) - dancing * 18f)
    pose("wrist.L", y = talking * talkPrimary * 5f + dancing * danceBeat * 8f)
    pose("wrist.R", y = talking * -talkSecondary * 5f + waving * wave * 18f - dancing * danceBeat * 8f)

    pose("upperleg01.L", x = walking * leftStep * 28f + dancing * danceBeat * 9f)
    pose("upperleg01.R", x = walking * rightStep * 28f - dancing * danceBeat * 9f)
    pose("lowerleg01.L", x = walking * leftKnee * 34f + dancing * kotlin.math.max(0f, -danceBeat) * 12f)
    pose("lowerleg01.R", x = walking * rightKnee * 34f + dancing * kotlin.math.max(0f, danceBeat) * 12f)
    pose("foot.L", x = walking * (-leftStep * 8f - leftKnee * 10f))
    pose("foot.R", x = walking * (-rightStep * 8f - rightKnee * 10f))

    val relaxedBreathing = (idle + talking + waving + reacting) * breath
    pose("pelvis.L", y = weightShift * 2.1f + dancing * danceHalf * 8f, z = walking * -leftStep * 2.5f)
    pose("pelvis.R", y = weightShift * 2.1f + dancing * danceHalf * 8f, z = walking * -rightStep * 2.5f)
    pose("pelvis", y = weightShift * 2.1f + dancing * danceHalf * 8f, z = walking * -leftStep * 2.5f)
    pose("spine03", x = relaxedBreathing * 1.1f, y = walking * -leftStep * 3.5f + dancing * danceHalf * 11f)
    pose("spine02", x = talking * talkSecondary * 1.6f + reacting * -4f, y = dancing * -danceHalf * 6f)
    pose("spine01", y = talking * talkPrimary * 2.3f + reacting * 3f)
    pose("clavicle.L", x = relaxedBreathing * 0.7f + talking * talkSecondary * 1.8f)
    pose("clavicle.R", x = relaxedBreathing * 0.7f - talking * talkSecondary * 1.8f + waving * -4f)
    pose(
        "head",
        x = talking * talkSecondary * 1.8f + reacting * -3f,
        y = weightShift * 1.5f + talking * talkPrimary * 2.2f,
        z = reacting * (4f + breath * 2f) + dancing * -danceHalf * 3f,
    )
    pose("jaw", x = jawOpen.coerceIn(0f, 1f) * 13f)
    if (resolveName("jaw") == null) {
        val mouthName = nodes.keys.firstOrNull { it.contains("hhp227_mouth", ignoreCase = true) }
        val mouth = mouthName?.let(nodes::get)
        val base = mouthName?.let(basePositions::get)
        if (mouth != null && base != null) {
            mouth.position = Position(base.x, base.y - jawOpen.coerceIn(0f, 1f) * 0.012f, base.z)
        }
    }
    return motion
}

private val RIG_BONE_ALIASES = mapOf(
    "pelvis" to listOf("ValveBiped.Bip01_Pelvis_040", "CC_Base_Pelvis_03"),
    "spine01" to listOf("ValveBiped.Bip01_Spine1_078", "ValveBiped.Bip01_Spine_077", "CC_Base_Waist_033"),
    "spine02" to listOf("ValveBiped.Bip01_Spine2_079", "CC_Base_Spine01_034"),
    "spine03" to listOf("ValveBiped.Bip01_Spine4_080", "CC_Base_Spine02_035"),
    "clavicle.L" to listOf("ValveBiped.Bip01_L_Clavicle_06", "CC_Base_L_Clavicle_049"),
    "clavicle.R" to listOf("ValveBiped.Bip01_R_Clavicle_044", "CC_Base_R_Clavicle_077"),
    "upperarm01.L" to listOf("ValveBiped.Bip01_L_UpperArm_036", "CC_Base_L_Upperarm_050"),
    "upperarm01.R" to listOf("ValveBiped.Bip01_R_UpperArm_074", "CC_Base_R_Upperarm_078"),
    "lowerarm01.L" to listOf("ValveBiped.Bip01_L_Forearm_024", "CC_Base_L_Forearm_051"),
    "lowerarm01.R" to listOf("ValveBiped.Bip01_R_Forearm_062", "CC_Base_R_Forearm_079"),
    "wrist.L" to listOf("ValveBiped.Bip01_L_Hand_025", "ValveBiped.Bip01_L_Wrist_037", "CC_Base_L_Hand_055"),
    "wrist.R" to listOf("ValveBiped.Bip01_R_Hand_063", "ValveBiped.Bip01_R_Wrist_075", "CC_Base_R_Hand_083"),
    "upperleg01.L" to listOf("ValveBiped.Bip01_L_Thigh_032", "CC_Base_L_Thigh_04"),
    "upperleg01.R" to listOf("ValveBiped.Bip01_R_Thigh_070", "CC_Base_R_Thigh_018"),
    "lowerleg01.L" to listOf("ValveBiped.Bip01_L_Calf_05", "CC_Base_L_Calf_05"),
    "lowerleg01.R" to listOf("ValveBiped.Bip01_R_Calf_043", "CC_Base_R_Calf_019"),
    "foot.L" to listOf("ValveBiped.Bip01_L_Foot_023", "CC_Base_L_Foot_06"),
    "foot.R" to listOf("ValveBiped.Bip01_R_Foot_061", "CC_Base_R_Foot_021"),
    "head" to listOf("ValveBiped.Bip01_Head1_02", "CC_Base_Head_038"),
    "jaw" to listOf("jaw", "Jaw", "mixamorig:Jaw", "CC_Base_JawRoot_040"),
)

private data class RigMotionFrame(
    val weights: FloatArray = FloatArray(CharacterAnimationController.AvatarAction.entries.size),
    val rootX: Float = 0f,
    val rootY: Float = 0f,
    val rootZ: Float = 0f,
    val scale: Float = 1f,
    val facingYaw: Float = 0f,
    val walkPhase: Float = 0f,
    val isNavigating: Boolean = false,
    val interaction: SceneInteraction = SceneInteraction.FLOOR,
)

private fun applyAuthoredRigPose(
    nodes: Map<String, Node>,
    baseRotations: Map<String, Float3>,
    motion: RigMotionFrame,
) {
    fun resolveName(name: String): String? = when {
        nodes.containsKey(name) -> name
        else -> RIG_BONE_ALIASES[name]?.firstOrNull(nodes::containsKey)
    }

    fun pose(name: String, x: Float = 0f, y: Float = 0f, z: Float = 0f) {
        val resolved = resolveName(name) ?: return
        val node = nodes[resolved] ?: return
        val base = baseRotations[resolved] ?: return
        node.rotation = Float3(base.x + x, base.y + y, base.z + z)
    }

    if (motion.isNavigating) {
        val leftStep = kotlin.math.sin(motion.walkPhase)
        val rightStep = -leftStep
        val leftKnee = kotlin.math.max(0f, -leftStep)
        val rightKnee = kotlin.math.max(0f, -rightStep)
        pose("upperarm01.L", x = -leftStep * 22f, z = -8f)
        pose("upperarm01.R", x = -rightStep * 22f, z = 8f)
        pose("lowerarm01.L", x = 7f + leftKnee * 5f)
        pose("lowerarm01.R", x = -7f - rightKnee * 5f)
        pose("upperleg01.L", x = leftStep * 27f)
        pose("upperleg01.R", x = rightStep * 27f)
        pose("lowerleg01.L", x = leftKnee * 38f)
        pose("lowerleg01.R", x = rightKnee * 38f)
        pose("foot.L", x = -leftStep * 7f - leftKnee * 9f)
        pose("foot.R", x = -rightStep * 7f - rightKnee * 9f)
        pose("spine03", y = -leftStep * 2.8f)
        return
    }

    when (motion.interaction) {
        SceneInteraction.BED -> {
            pose("spine03", x = -7f, y = 6f)
            pose("upperarm01.L", x = 34f, y = 8f, z = -22f)
            pose("lowerarm01.L", x = 38f)
            pose("wrist.L", y = 12f)
        }
        SceneInteraction.CHAIR -> {
            pose("spine03", x = -4f, y = -5f)
            pose("upperarm01.R", x = 28f, y = -6f, z = 22f)
            pose("lowerarm01.R", x = 34f)
            pose("wrist.R", y = -10f)
        }
        SceneInteraction.DRESSER -> {
            pose("spine03", x = -4f, y = -7f)
            pose("upperarm01.R", x = 42f, y = 8f, z = -34f)
            pose("lowerarm01.R", x = 54f)
            pose("wrist.R", y = -12f)
        }
        SceneInteraction.FLOOR -> Unit
    }
}

private class RigMotionState {
    private val weights = FloatArray(CharacterAnimationController.AvatarAction.entries.size)
    private var lastFrameNanos = 0L
    private var rootX = 0f
    private var walkDirection = 1f
    private var walkVelocity = 0f
    private var walkPhase = 0f
    private var rootDepth = 0.56f
    private var navigationSpeed = 0f
    private var destination: StageDestination? = null
    private var settledInteraction = SceneInteraction.FLOOR
    private var manuallyPlaced = false

    fun navigateTo(normalizedX: Float, normalizedY: Float) {
        val x = normalizedX.coerceIn(0f, 1f)
        val y = normalizedY.coerceIn(0f, 1f)
        manuallyPlaced = true
        settledInteraction = SceneInteraction.FLOOR
        val interaction = when {
            x < 0.60f && y in 0.30f..0.60f -> SceneInteraction.BED
            x in 0.66f..0.82f && y in 0.30f..0.57f -> SceneInteraction.CHAIR
            x > 0.82f && y in 0.34f..0.70f -> SceneInteraction.DRESSER
            else -> SceneInteraction.FLOOR
        }
        val depth = when (interaction) {
            SceneInteraction.BED -> 0.38f
            SceneInteraction.CHAIR -> 0.40f
            SceneInteraction.DRESSER -> 0.48f
            SceneInteraction.FLOOR -> ((y - 0.24f) / 0.70f).coerceIn(0.06f, 0.90f)
        }
        val perspectiveWidth = 0.48f + depth * 0.52f
        val stageX = when (interaction) {
            SceneInteraction.BED -> -0.30f
            SceneInteraction.CHAIR -> 0.32f
            SceneInteraction.DRESSER -> 0.45f
            SceneInteraction.FLOOR -> ((x - 0.5f) * 1.25f * perspectiveWidth).coerceIn(-0.62f, 0.62f)
        }
        destination = StageDestination(stageX, depth, interaction)
    }

    fun advance(
        action: CharacterAnimationController.AvatarAction,
        frameTimeNanos: Long,
    ): RigMotionFrame {
        val isFirstFrame = lastFrameNanos == 0L
        val deltaSeconds = if (isFirstFrame) {
            0f
        } else {
            ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
        }
        lastFrameNanos = frameTimeNanos
        val activeDestination = destination
        val destinationDistance = activeDestination?.let {
            kotlin.math.sqrt(
                (it.x - rootX) * (it.x - rootX) +
                    (it.depth - rootDepth) * (it.depth - rootDepth),
            )
        } ?: 0f
        val navigating = activeDestination != null && destinationDistance > DESTINATION_EPSILON
        val effectiveAction = if (navigating) CharacterAnimationController.AvatarAction.WALKING else action
        if (isFirstFrame) weights[effectiveAction.ordinal] = 1f
        val blend = 1f - kotlin.math.exp(-deltaSeconds * 7.5f)
        weights.indices.forEach { index ->
            val target = if (index == effectiveAction.ordinal) 1f else 0f
            weights[index] += (target - weights[index]) * blend
        }

        val walking = weights[CharacterAnimationController.AvatarAction.WALKING.ordinal]
        val velocityBlend = 1f - kotlin.math.exp(-deltaSeconds * 5.5f)
        val distance = if (navigating) {
            val target = checkNotNull(activeDestination)
            val targetSpeed = kotlin.math.min(NAVIGATION_SPEED, destinationDistance * 2.6f)
            navigationSpeed += (targetSpeed - navigationSpeed) * velocityBlend
            val step = kotlin.math.min(destinationDistance, navigationSpeed * deltaSeconds)
            val directionX = (target.x - rootX) / destinationDistance
            val directionDepth = (target.depth - rootDepth) / destinationDistance
            rootX += directionX * step
            rootDepth += directionDepth * step
            walkVelocity = directionX * navigationSpeed
            if (step >= destinationDistance - DESTINATION_EPSILON) {
                rootX = target.x
                rootDepth = target.depth
                settledInteraction = target.interaction
                destination = null
                navigationSpeed = 0f
            }
            step
        } else {
            navigationSpeed += (0f - navigationSpeed) * velocityBlend
            val desiredVelocity = if (manuallyPlaced) 0f else walkDirection * WALK_SPEED * walking
            walkVelocity += (desiredVelocity - walkVelocity) * velocityBlend
            rootX += walkVelocity * deltaSeconds
            if (rootX >= WALK_BOUNDARY) {
                rootX = WALK_BOUNDARY
                walkDirection = -1f
            } else if (rootX <= -WALK_BOUNDARY) {
                rootX = -WALK_BOUNDARY
                walkDirection = 1f
            }
            kotlin.math.abs(walkVelocity) * deltaSeconds
        }
        walkPhase = (walkPhase + distance / STRIDE_LENGTH * TWO_PI) % TWO_PI

        val gaitBounce = kotlin.math.abs(kotlin.math.sin(walkPhase)) * 0.027f * walking
        val breathing = kotlin.math.sin(frameTimeNanos / 1_000_000_000f * 1.35f) * 0.009f
        val dancing = weights[CharacterAnimationController.AvatarAction.DANCING.ordinal]
        val dancePhase = kotlin.math.sin(frameTimeNanos / 1_000_000_000f * 4.8f)
        val reacting = weights[CharacterAnimationController.AvatarAction.REACTING.ordinal]
        val perspectiveY = 0.34f - rootDepth * 0.40f
        val interactionLowering = when (settledInteraction) {
            SceneInteraction.BED -> 0.04f
            SceneInteraction.CHAIR -> 0.04f
            SceneInteraction.DRESSER, SceneInteraction.FLOOR -> 0f
        }
        val rootY = perspectiveY - interactionLowering + breathing + gaitBounce +
            kotlin.math.abs(dancePhase) * 0.045f * dancing + reacting * 0.025f
        val danceSway = kotlin.math.sin(frameTimeNanos / 1_000_000_000f * 2.4f) * 0.10f * dancing
        return RigMotionFrame(
            weights = weights,
            rootX = rootX + danceSway,
            rootY = rootY,
            rootZ = -1.25f + rootDepth * 1.25f,
            scale = 0.50f + rootDepth * 0.50f,
            facingYaw = 0f,
            walkPhase = walkPhase,
            isNavigating = navigating,
            interaction = if (navigating) SceneInteraction.FLOOR else settledInteraction,
        )
    }

    private companion object {
        const val WALK_BOUNDARY = 0.48f
        const val WALK_SPEED = 0.23f
        const val STRIDE_LENGTH = 0.22f
        const val TWO_PI = 6.2831855f
        const val NAVIGATION_SPEED = 0.52f
        const val DESTINATION_EPSILON = 0.008f
    }
}

private data class StageDestination(
    val x: Float,
    val depth: Float,
    val interaction: SceneInteraction,
)

private fun Float3.scaledBy(value: Float) = Float3(x * value, y * value, z * value)

private enum class SceneInteraction {
    FLOOR,
    BED,
    CHAIR,
    DRESSER,
}

private fun AvatarFacePose.toMorphWeights(order: List<String>): FloatArray =
    order.map { name ->
        when (name) {
            "jawOpen" -> jawOpen
            "viseme_sil" -> visemeSil
            "viseme_aa" -> visemeAa
            "viseme_E" -> visemeE
            "viseme_O" -> visemeO
            "viseme_U" -> visemeU
            "viseme_FF" -> visemeFf
            "viseme_PP" -> visemePp
            "viseme_TH" -> visemeTh
            "mouthOpen" -> mouthOpen
            "mouthSmile" -> mouthSmile.coerceAtLeast(0f)
            "mouthFrown" -> (-mouthSmile).coerceAtLeast(0f)
            "mouthPucker" -> mouthPucker
            "mouthPress" -> mouthPress
            "browInnerUp" -> browInnerUp
            "browDownLeft", "browDownRight" -> browDown
            "eyeBlinkLeft", "eyeBlinkRight" -> eyeBlink
            "eyeWideLeft", "eyeWideRight" -> eyeWide
            "eyeSquintLeft", "eyeSquintRight" -> eyeSquint
            "cheekSquintLeft", "cheekSquintRight" -> cheekSquint
            else -> 0f
        }.coerceIn(0f, 1f)
    }.toFloatArray()

@Composable
private fun FallbackAvatarStage(
    template: VisualTemplate,
    facePose: AvatarFacePose,
    idleLift: Float,
) {
    val tilt by animateFloatAsState(
        targetValue = facePose.headTiltDegrees,
        animationSpec = tween(350),
        label = "fallbackTilt",
    )
    val scale by animateFloatAsState(
        targetValue = 1f + (facePose.jawOpen * 0.03f) + (facePose.speakingBob * 0.02f),
        animationSpec = tween(120),
        label = "fallbackScale",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = 0.2f + (facePose.mouthSmile.coerceAtLeast(0f) * 0.2f) + (facePose.eyeWide * 0.1f),
        animationSpec = tween(250),
        label = "fallbackGlow",
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .scale(1f + facePose.speakingBob * 0.08f)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(template.fallbackPalette.accentColor).copy(alpha = glowAlpha)),
        )
        ProceduralAvatarPreview(
            template = template,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .graphicsLayer {
                    rotationZ = tilt
                    translationY = idleLift * 120f
                }
                .scale(scale)
                .clip(RoundedCornerShape(28.dp)),
        )
    }
}

@Composable
private fun BoxScope.AvatarStatusOverlay(
    template: VisualTemplate,
    downloadState: DownloadState,
    statusText: String?,
    showDownloadAction: Boolean,
    onDownload: () -> Unit,
) {
    val message = when (downloadState) {
        DownloadState.Idle -> statusText
        is DownloadState.Progress -> "Downloading ${template.displayName} ${(downloadState.percent * 100).toInt()}%"
        is DownloadState.Complete -> "Downloaded ${template.displayName}. The full-detail avatar is ready."
        is DownloadState.Error -> downloadState.message
    } ?: return

    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
        color = Color.Black.copy(alpha = 0.48f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurface,
            )
            when (downloadState) {
                is DownloadState.Progress -> {
                    CircularProgressIndicator(
                        progress = { downloadState.percent },
                        color = Primary,
                    )
                }

                is DownloadState.Error, DownloadState.Idle -> {
                    if (showDownloadAction) {
                        Button(onClick = onDownload) {
                            Text(text = "Download Full Avatar")
                        }
                    } else {
                        Text(
                            text = "Rendering the built-in 3D fallback until a GLB is available.",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant,
                        )
                    }
                }

                is DownloadState.Complete -> {
                    Text(
                        text = "Re-open this screen anytime to view the downloaded model.",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                    )
                }
            }
        }
    }
}
