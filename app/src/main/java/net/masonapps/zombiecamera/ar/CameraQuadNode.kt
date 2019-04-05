package net.masonapps.zombiecamera.ar

import android.util.Log
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.RenderableDefinition
import com.google.ar.sceneform.rendering.Vertex
import java.util.concurrent.ExecutionException

class CameraQuadNode : Node() {


    private val quadNode: Node = Node()
    private val vertices: ArrayList<Vertex> = ArrayList(3)
    //    private val triangleIndices: ArrayList<Int> = arrayListOf(0, 1, 2)
    private val triangleIndices: ArrayList<Int> = arrayListOf(2, 1, 0)
    private val submeshes: ArrayList<RenderableDefinition.Submesh> = ArrayList()
    private val quadMeshDefinition: RenderableDefinition
    private val view = Matrix()
    private val projection = Matrix()
    private val projectionView = Matrix()
    private val inverseProjectionView = Matrix()
    private val tmpVec = FloatArray(4)
    private val resultVec = FloatArray(4)
    private val vertexVectors = arrayOf(
        Vector3(-1.0f, 1.0f, 1.0f),
        Vector3(-1.0f, -3.0f, 1.0f),
        Vector3(3.0f, 1.0f, 1.0f)
    )
    private val uvCoords = floatArrayOf(
        0.0f, 0.0f,
        0.0f, 2.0f,
        2.0f, 0.0f
    )

    private val transformedUvCoords = uvCoords.copyOf()

    private var quadRenderable: ModelRenderable? = null
    var material: Material? = null
        set(value) {
            field = value
            updateSubmeshes()
        }

    init {
        quadNode.setParent(this)
        quadMeshDefinition = RenderableDefinition.builder().setVertices(vertices).setSubmeshes(submeshes).build()
        worldPosition = Vector3.zero()
    }

    fun updateFrame(frame: Frame) {
        // Wait until the material is loaded.
        if (material == null) return

        val near = 0.1f
        val far = 5f
        frame.camera.getProjectionMatrix(projection.data, 0, near, far)
        frame.camera.getViewMatrix(view.data, 0)
        android.opengl.Matrix.multiplyMM(projectionView.data, 0, projection.data, 0, view.data, 0)
        android.opengl.Matrix.invertM(inverseProjectionView.data, 0, projectionView.data, 0)

        frame.transformCoordinates2d(
            Coordinates2d.VIEW_NORMALIZED,
            uvCoords,
            Coordinates2d.TEXTURE_NORMALIZED,
            transformedUvCoords
        )

        for (i in 1 until 6 step 2) {
            transformedUvCoords[i] = 1f - transformedUvCoords[i]
        }

        val z = 0.75f
        val p0 = inverseProjectionView.transformPoint(vertexVectors[0])
        val uv0 = Vertex.UvCoordinate(transformedUvCoords[0], transformedUvCoords[1])

        val p1 = inverseProjectionView.transformPoint(vertexVectors[1])
        val uv1 = Vertex.UvCoordinate(transformedUvCoords[2], transformedUvCoords[3])

        val p2 = inverseProjectionView.transformPoint(vertexVectors[2])
        val uv2 = Vertex.UvCoordinate(transformedUvCoords[4], transformedUvCoords[5])

//        val p3 = vertexVectors[2]
//        val uv3 = Vertex.UvCoordinate(transformedUvCoords[6], transformedUvCoords[7])


//        Log.d(javaClass.simpleName, "p0$p0, p1$p1, p2$p2, p3$p3")
//        Log.d(javaClass.simpleName, "uv0$uv0, uv1$uv1, uv2$uv2, uv3$uv3")

        vertices.clear()
        vertices.add(Vertex.builder().setPosition(p0).setUvCoordinate(uv0).build())
        vertices.add(Vertex.builder().setPosition(p1).setUvCoordinate(uv1).build())
        vertices.add(Vertex.builder().setPosition(p2).setUvCoordinate(uv2).build())
//        vertices.add(Vertex.builder().setPosition(p3).setUvCoordinate(uv3).build())

        updateMesh()
    }

    private fun updateSubmeshes() {
        if (this.material == null) return

        submeshes.clear()

        createSubMesh(material!!)
    }

    private fun createSubMesh(material: Material): Boolean {
        val submesh = RenderableDefinition.Submesh.builder()
            .setTriangleIndices(triangleIndices)
            .setMaterial(material)
            .build()
        return submeshes.add(submesh)
    }

    private fun updateMesh() {

        if (quadRenderable == null) {
            try {
                quadRenderable = ModelRenderable.builder().setSource(quadMeshDefinition).build().get()
                quadRenderable!!.renderPriority = ModelRenderable.RENDER_PRIORITY_DEFAULT
            } catch (ex: InterruptedException) {
                Log.e(javaClass.simpleName, "Failed to build quadRenderable from definition", ex)
            } catch (ex: ExecutionException) {
                Log.e(javaClass.simpleName, "Failed to build quadRenderable from definition", ex)
            }

            checkNotNull(quadRenderable).isShadowReceiver = false
            checkNotNull(quadRenderable).isShadowCaster = false

            quadNode.renderable = quadRenderable
        } else {
            // Face mesh renderable already exists, so update it to match the face mesh definition.
            quadRenderable?.updateFromDefinition(quadMeshDefinition)
        }
    }
}