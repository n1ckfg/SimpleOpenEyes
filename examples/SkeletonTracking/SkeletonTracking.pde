/**
 * SkeletonTracking — SimpleOpenEyes port of a SimpleOpenNI skeleton sketch.
 * Uses a live RGB camera (GStreamer autovideosrc) and the lightweight
 * OpenPose network from deep-vision-processing. No depth camera required.
 */
import SimpleOpenNI.*;

SimpleOpenEyes eyes;

void setup() {
  size(640, 480);

  eyes = new SimpleOpenEyes(this);
  eyes.enableRGB();
  // eyes.enableUser(); // TEMP offline
  eyes.init();
  println("SETUP_DONE");
}

void draw() {
  background(20);
  int t0=millis(); eyes.update(); if (frameCount<8||frameCount%50==0) println("FC"+frameCount+" update_ms="+(millis()-t0));

  if (frameCount == 2) println("FRAME2 cam=" + eyes.rgbWidth() + "x" + eyes.rgbHeight());
  if (frameCount % 10 == 0) println("FC" + frameCount + " users=" + eyes.getNumberOfUsers());

  if (eyes.rgbImage() != null) {
    image(eyes.rgbImage(), 0, 0, width, height);
  }

  if (eyes.isTrackingSkeleton(1)) {
    drawSkeleton(1);
  }

  surface.setTitle("SimpleOpenEyes skeleton — FPS " + nf(frameRate, 2, 1));
}

void drawSkeleton(int userId) {
  stroke(255, 200, 50);
  strokeWeight(4);

  connect(SimpleOpenEyes.SKEL_HEAD, SimpleOpenEyes.SKEL_NECK);
  connect(SimpleOpenEyes.SKEL_NECK, SimpleOpenEyes.SKEL_TORSO);
  connect(SimpleOpenEyes.SKEL_NECK, SimpleOpenEyes.SKEL_LEFT_SHOULDER);
  connect(SimpleOpenEyes.SKEL_NECK, SimpleOpenEyes.SKEL_RIGHT_SHOULDER);
  connect(SimpleOpenEyes.SKEL_LEFT_SHOULDER, SimpleOpenEyes.SKEL_LEFT_ELBOW);
  connect(SimpleOpenEyes.SKEL_LEFT_ELBOW, SimpleOpenEyes.SKEL_LEFT_HAND);
  connect(SimpleOpenEyes.SKEL_RIGHT_SHOULDER, SimpleOpenEyes.SKEL_RIGHT_ELBOW);
  connect(SimpleOpenEyes.SKEL_RIGHT_ELBOW, SimpleOpenEyes.SKEL_RIGHT_HAND);
  connect(SimpleOpenEyes.SKEL_TORSO, SimpleOpenEyes.SKEL_LEFT_HIP);
  connect(SimpleOpenEyes.SKEL_TORSO, SimpleOpenEyes.SKEL_RIGHT_HIP);
  connect(SimpleOpenEyes.SKEL_LEFT_HIP, SimpleOpenEyes.SKEL_LEFT_KNEE);
  connect(SimpleOpenEyes.SKEL_LEFT_KNEE, SimpleOpenEyes.SKEL_LEFT_FOOT);
  connect(SimpleOpenEyes.SKEL_RIGHT_HIP, SimpleOpenEyes.SKEL_RIGHT_KNEE);
  connect(SimpleOpenEyes.SKEL_RIGHT_KNEE, SimpleOpenEyes.SKEL_RIGHT_FOOT);
}

void connect(int a, int b) {
  PVector pa = eyes.getJointPositionSkeleton(1, a);
  PVector pb = eyes.getJointPositionSkeleton(1, b);
  if (eyes.getConfidence(a) < 0.3 || eyes.getConfidence(b) < 0.3) return;
  line(pa.x, pa.y, pb.x, pb.y);
}
