/*
 * Copyright 2019 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.masonapps.zombiecamera.ar;

import androidx.appcompat.app.AppCompatActivity;

/**
 * This is an example activity that uses the Sceneform UX package to make common Augmented Faces
 * tasks easier.
 */
public class AugmentedFacesActivity extends AppCompatActivity {

//  private FaceArFragment arFragment;
//
//  private Texture faceMeshTexture;
//
//  private final HashMap<AugmentedFace, AugmentedFaceNode> faceNodeMap = new HashMap<>();
//
//  @Override
//  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
//  // CompletableFuture requires api level 24
//  // FutureReturnValueIgnored is not valid
//  protected void onCreate(Bundle savedInstanceState) {
//    super.onCreate(savedInstanceState);
//
//    if (!checkIsSupportedDeviceOrFinish(this)) {
//      return;
//    }
//
//    setContentView(R.layout.activity_face_mesh);
//    arFragment = (FaceArFragment) getSupportFragmentManager().findFragmentById(R.id.face_fragment);
//
//
//    // Load the face mesh texture.
//    Texture.builder()
//        .setSource(this, R.drawable.fox_face_mesh_texture)
//        .build()
//        .thenAccept(texture -> faceMeshTexture = texture);
//
//    ArSceneView sceneView = arFragment.getArSceneView();
//
//    // This is important to make sure that the camera stream renders first so that
//    // the face mesh occlusion works correctly.
//    sceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);
//
//    Scene scene = sceneView.getScene();
//
//    scene.addOnUpdateListener(
//        (FrameTime frameTime) -> {
//          if (faceMeshTexture == null) {
//            return;
//          }
//
//          Collection<AugmentedFace> faceList =
//              sceneView.getSession().getAllTrackables(AugmentedFace.class);
//
//          // Make new AugmentedFaceNodes for any new faces.
//          for (AugmentedFace face : faceList) {
//            if (!faceNodeMap.containsKey(face)) {
//              AugmentedFaceNode faceNode = new AugmentedFaceNode(face);
//              faceNode.setParent(scene);
//              faceNode.setFaceMeshTexture(faceMeshTexture);
//              faceNodeMap.put(face, faceNode);
//            }
//          }
//
//          // Remove any AugmentedFaceNodes associated with an AugmentedFace that stopped tracking.
//          Iterator<Map.Entry<AugmentedFace, AugmentedFaceNode>> iter =
//              faceNodeMap.entrySet().iterator();
//          while (iter.hasNext()) {
//            Map.Entry<AugmentedFace, AugmentedFaceNode> entry = iter.next();
//            AugmentedFace face = entry.getKey();
//            if (face.getTrackingState() == TrackingState.STOPPED) {
//              AugmentedFaceNode faceNode = entry.getValue();
//              faceNode.setParent(null);
//              iter.remove();
//            }
//          }
//        });
//  }
}
