package net.masonapps.zombiecamera.ar

import android.util.Log
import com.google.ar.core.AugmentedFace
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import java.util.concurrent.ExecutionException

class FaceOverlayNode(var augmentedFace: AugmentedFace? = null) : Node() {

    companion object {
        private val TAG = FaceOverlayNode::class.java.simpleName
        private val FACE_MESH_RENDER_PRIORITY =
            Math.max(Renderable.RENDER_PRIORITY_FIRST, Renderable.RENDER_PRIORITY_DEFAULT - 1)
    }

    private val faceMeshNode: Node = Node()
    private val vertices: ArrayList<Vertex> = ArrayList()
    private val triangleIndices: ArrayList<Int> = ArrayList()
    private val submeshes: ArrayList<RenderableDefinition.Submesh> = ArrayList()
    private val faceMeshDefinition: RenderableDefinition
    
    private var faceMeshRenderable: ModelRenderable? = null
    var faceMeshMaterial: Material? = null
    set(value){
        field = value
        updateSubmeshes()
    }

    init {
        faceMeshNode.setParent(this)
        faceMeshDefinition = RenderableDefinition.builder().setVertices(vertices).setSubmeshes(submeshes).build()
    }

//    override fun onActivate() {
//        val scene = checkNotNull(scene)
//        val context = scene.view.context
//
//        ModelRenderable.builder()
//            .setSource(context){
//                context.assets.open(Assets.BLENDED_MATERIAL)
//            }
//            .build()
//            .handle { renderable, throwable ->
//                if (throwable != null) {
//                    Log.e(TAG, "Unable to load face mesh material.", throwable)
//                    return@handle false
//                }
//
//                faceMeshMaterial = renderable.material
//                updateSubmeshes()
//                true
//            }
//    }

    override fun onUpdate(frameTime: FrameTime) {
        val isTracking = isTracking()

        // Only render the visual effects when the augmented face is tracking.
        faceMeshNode.isEnabled = isTracking

        if (isTracking) {
            updateTransform()
            updateFaceMesh()
        }
    }

    override fun onDeactivate() {
        super.onDeactivate()
    }

    private fun updateSubmeshes() {
        if (this.faceMeshMaterial == null) return

        val faceMeshMaterial = this.faceMeshMaterial!!

        submeshes.clear()
        
        faceMeshMaterial?.let {
            createSubMesh(it)
        }
    }

    private fun createSubMesh(material: Material): Boolean {
        val submesh = RenderableDefinition.Submesh.builder()
            .setTriangleIndices(triangleIndices)
            .setMaterial(material)
            .build()
        return submeshes.add(submesh)
    }

    private fun updateTransform() {
        // Update this node to be positioned at the center pose of the face.
        augmentedFace?.centerPose?.let { pose ->
            worldPosition = Vector3(pose.tx(), pose.ty(), pose.tz())
            worldRotation = Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw())
        }
    }

    private fun updateFaceMesh() {
        // Wait until the material is loaded.
        if (faceMeshMaterial == null) return

        updateFaceMeshVerticesAndTriangles()

        if (faceMeshRenderable == null) {
            try {
                faceMeshRenderable = ModelRenderable.builder().setSource(faceMeshDefinition).build().get()
                faceMeshRenderable!!.renderPriority = FACE_MESH_RENDER_PRIORITY
            } catch (ex: InterruptedException) {
                Log.e(TAG, "Failed to build faceMeshRenderable from definition", ex)
            } catch (ex: ExecutionException) {
                Log.e(TAG, "Failed to build faceMeshRenderable from definition", ex)
            }

            checkNotNull(faceMeshRenderable).isShadowReceiver = false
            checkNotNull(faceMeshRenderable).isShadowCaster = false

            faceMeshNode.renderable = faceMeshRenderable
        } else {
            // Face mesh renderable already exists, so update it to match the face mesh definition.
            faceMeshRenderable?.updateFromDefinition(faceMeshDefinition)
        }
    }

    private fun updateFaceMeshVerticesAndTriangles() {
        if(augmentedFace == null) return
        
        val augmentedFace = this.augmentedFace!!

        val verticesBuffer = augmentedFace.meshVertices
        verticesBuffer.rewind()
        // Vertices in x, y, z packing.
        val numVertices = verticesBuffer.limit() / 3

        val textureCoordsBuffer = augmentedFace.meshTextureCoordinates
        textureCoordsBuffer.rewind()
        // Texture coordinates in u, v packing.
        val numTextureCoords = textureCoordsBuffer.limit() / 2

        val normalsBuffer = augmentedFace.meshNormals
        normalsBuffer.rewind()
        // Normals in x, y, z packing.
        val numNormals = normalsBuffer.limit() / 3

        if (numVertices != numTextureCoords || numVertices != numNormals) {
            throw IllegalStateException(
                "AugmentedFace must have the same number of vertices, normals, and texture coordinates."
            )
        }

        vertices.ensureCapacity(numVertices)

        for (i in 0 until numVertices) {
            // position.
            val vX = verticesBuffer.get()
            val vY = verticesBuffer.get()
            val vZ = verticesBuffer.get()

            // Normal.
            val nX = normalsBuffer.get()
            val nY = normalsBuffer.get()
            val nZ = normalsBuffer.get()

            // Uv coordinate.
            //TODO fix u flip
            val u = 1f - textureCoordsBuffer.get()
            val v = textureCoordsBuffer.get()

            if (i < vertices.size) {
                // Re-use existing vertex.
                val vertex = vertices[i]

                val vertexPos = checkNotNull(vertex.position)
                vertexPos.set(vX, vY, vZ)

                val normal = checkNotNull(vertex.normal)
                normal.set(nX, nY, nZ)

                val uvCoord = checkNotNull(vertex.uvCoordinate)
                uvCoord.x = u
                uvCoord.y = v
            } else {
                // Create new vertex.
                val vertex = Vertex.builder()
                    .setPosition(Vector3(vX, vY, vZ))
                    .setNormal(Vector3(nX, nY, nZ))
                    .setUvCoordinate(Vertex.UvCoordinate(u, v))
                    .build()

                vertices.add(vertex)
            }
        }

        // Remove any extra vertices. In practice, this shouldn't happen.
        // The number of vertices remains the same each frame.
        while (vertices.size > numVertices) {
            vertices.removeAt(vertices.size - 1)
        }

        val indicesBuffer = augmentedFace.meshTriangleIndices
        indicesBuffer.rewind()

        // Only do this if the size doesn't match.
        // The triangle indices of the face mesh don't change from frame to frame.
        if (triangleIndices.size != indicesBuffer.limit()) {
            triangleIndices.clear()
            triangleIndices.ensureCapacity(indicesBuffer.limit())

            while (indicesBuffer.hasRemaining()) {
                triangleIndices.add(indicesBuffer.get().toInt())
            }
        }
    }

    private fun isTracking(): Boolean = augmentedFace?.trackingState == TrackingState.TRACKING
}