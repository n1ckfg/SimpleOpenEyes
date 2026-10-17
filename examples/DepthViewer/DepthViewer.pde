/**
 * DepthViewer — SimpleOpenEyes MiDaS monocular depth from an RGB camera.
 * Replaces SimpleOpenNI hardware depth with estimated relative depth.
 */
import SimpleOpenEyes.*;

SimpleOpenEyes eyes;

void setup() {
  size(640, 480);

  eyes = new SimpleOpenEyes(this);
  eyes.enableRGB();
  eyes.enableDepth();   // MiDaS estimated depth
  eyes.init();
}

void draw() {
  background(0);
  eyes.update();

  PImage depth = eyes.depthImage();
  if (depth != null) {
    image(depth, 0, 0, width, height);
  }

  surface.setTitle("SimpleOpenEyes depth — FPS " + nf(frameRate, 2, 1));
}
