package SimpleOpenNI;

import processing.core.*;
import ch.bildspur.vision.DeepVision;
import ch.bildspur.vision.SingleHumanPoseNetwork;
import ch.bildspur.vision.MidasNetwork;
import ch.bildspur.vision.result.HumanPoseResult;
import ch.bildspur.vision.result.KeyPointResult;
import ch.bildspur.vision.result.ImageResult;
import processing.video.Capture;

import java.lang.reflect.Method;


/**
 * SimpleOpenEyes — RGB camera tracking replacement for SimpleOpenNI.
 *
 * Drops all OpenNI/NiTE dependencies. Depth comes from MiDaS monocular
 * estimation, person/skeleton tracking from deep-vision-processing's
 * single human pose network (lightweight OpenPose). The API mirrors
 * SimpleOpenNI so existing sketches migrate with minimal changes.
 */
public class SimpleOpenEyes implements SimpleOpenNIConstants {
    public PApplet parent;

    private DeepVision vision;
    private SingleHumanPoseNetwork poseNet;
    private MidasNetwork depthNet;

    private Capture cam;
    private String cameraDevice = "pipeline:autovideosrc";

    private boolean depthEnabled = false;
    private boolean rgbEnabled = false;
    private boolean userEnabled = false;
    private boolean irEnabled = false;

    private boolean mirror = true;

    // per-frame state
    private PVector[] joints;                    // SKEL_* indexed
    private float[] jointConfidence;
    private int trackingUserId = -1;

    private PImage rgbImage;
    private PImage depthImage;
    private float[] depthMap;                    // normalized 0..1 midas depth per pixel

    // callbacks
    private Method onNewUserCb, onLostUserCb, onStartCalibrationCb, onEndCalibrationCb;
    private Method onStartPoseCb, onEndPoseCb;

    public SimpleOpenEyes(PApplet parent) {
        this.parent = parent;
        parent.registerMethod("dispose", this);
        this.vision = new DeepVision(parent);

        try { onNewUserCb = findCallback("onNewUser"); } catch (Exception e) {}
        try { onLostUserCb = findCallback("onLostUser"); } catch (Exception e) {}
        try { onStartCalibrationCb = findCallback("onStartCalibration"); } catch (Exception e) {}
        try { onEndCalibrationCb = findCallback("onEndCalibration"); } catch (Exception e) {}
        try { onStartPoseCb = findCallback("onStartPose"); } catch (Exception e) {}
        try { onEndPoseCb = findCallback("onEndPose"); } catch (Exception e) {}
    }

    private Method findCallback(String name) throws NoSuchMethodException {
        for (Method m : parent.getClass().getMethods()) {
            if (m.getName().equals(name)) return m;
        }
        return null;
    }

    /** Set GStreamer pipeline for the camera source. Call before init(). */
    public void setCameraPipeline(String pipeline) {
        this.cameraDevice = pipeline;
    }

    public void enableDepth() { depthEnabled = true; }
    public void enableDepth(int w, int h) { depthEnabled = true; }

    public void enableRGB() { rgbEnabled = true; }
    public void enableRGB(int w, int h) { rgbEnabled = true; }

    public void enableIR() { /* IR unavailable on RGB cameras — no-op */ }
    public boolean enableIR(int w, int h) { return false; }

    public void enableUser() { userEnabled = true; }

    /** Initialize networks and camera. Call after enabling nodes. */
    public boolean init() {
        if (userEnabled || true) { // pose network is the core of this lib
            poseNet = vision.createSingleHumanPoseEstimation();
            poseNet.setup();
        }
        if (depthEnabled) {
            depthNet = vision.createMidasNetwork();
            depthNet.setup();
        }
        if (rgbEnabled || depthEnabled || userEnabled) {
            cam = new Capture(parent, cameraDevice);
            cam.start();
        }
        joints = new PVector[SKEL_COUNT];
        jointConfidence = new float[SKEL_COUNT];
        for (int i = 0; i < SKEL_COUNT; i++) joints[i] = new PVector();
        return true;
    }

    public void dispose() {
        if (cam != null) cam.stop();
    }

    /** Update: read camera frame and re-run tracking. */
    public boolean update() {
        if (cam != null && cam.available()) {
            cam.read();
            if (mirror) {
                cam.loadPixels();
                mirrorPixels(cam);
            }
        }
        if (cam == null || cam.width == 0) return false;
        rgbImage = cam;

        if (userEnabled) {
            try {
                // downscale for speed: pose input is 288x384 anyway; heatmap resize scales with frame size
                PImage poseInput = rgbImage.copy();
                poseInput.resize(rgbImage.width / 2, rgbImage.height / 2);
                HumanPoseResult result = poseNet.run(poseInput);
                boolean hadUser = (trackingUserId != -1);
                boolean hasUser = result != null && hasConfidentPerson(result);
                if (hasUser && !hadUser) fireUserCallback(onNewUserCb, 1);
                if (!hasUser && hadUser) fireUserCallback(onLostUserCb, 1);
                trackingUserId = hasUser ? 1 : -1;
                if (hasUser) extractJoints(result);
                return true;
            } catch (Exception e) {
                System.err.println("SimpleOpenEyes pose run: " + e.getMessage());
            }
        }
        return true;
    }

    private boolean hasConfidentPerson(HumanPoseResult r) {
        float sum = 0;
        for (KeyPointResult kp : r.getKeyPoints()) sum += kp.getProbability();
        return sum / r.getKeyPoints().size() > 0.15f; // rough likelihood a person is visible
    }

    private void extractJoints(HumanPoseResult r) {
        // LIP/COCO keypoint order (matches HumanPoseResult getters):
        // 0 nose, 1/2 eyes, 3/4 ears, 5 left shoulder, 6 right shoulder,
        // 7 left elbow, 8 right elbow, 9 left wrist, 10 right wrist,
        // 11 left hip, 12 right hip, 13 left knee, 14 right knee,
        // 15 left ankle, 16 right ankle
        KeyPointResult[] kps = new KeyPointResult[17];
        for (int i = 0; i < 17; i++) kps[i] = r.getKeyPoints().get(i);

        setJoint(SKEL_HEAD, kps[0]);   setJoint(SKEL_NECK, mid(kps[3], kps[4], kps[0]));
        setJoint(SKEL_TORSO, mid(kps[11], kps[12], kps[5], kps[6]));
        setJoint(SKEL_LEFT_SHOULDER, kps[5]);  setJoint(SKEL_RIGHT_SHOULDER, kps[6]);
        setJoint(SKEL_LEFT_ELBOW, kps[7]);     setJoint(SKEL_RIGHT_ELBOW, kps[8]);
        setJoint(SKEL_LEFT_HAND, kps[9]);      setJoint(SKEL_RIGHT_HAND, kps[10]);
        setJoint(SKEL_LEFT_HIP, kps[11]);      setJoint(SKEL_RIGHT_HIP, kps[12]);
        setJoint(SKEL_LEFT_KNEE, kps[13]);     setJoint(SKEL_RIGHT_KNEE, kps[14]);
        setJoint(SKEL_LEFT_FOOT, kps[15]);     setJoint(SKEL_RIGHT_FOOT, kps[16]);
    }

    private void setJoint(int joint, KeyPointResult kp) {
        if (joint < 0 || joint >= SKEL_COUNT) return;
        if (kp == null) { jointConfidence[joint] = 0; return; }
        joints[joint].x = kp.getX() * 2;   // pose input was downscaled 2x; map back to frame coords
        joints[joint].y = kp.getY() * 2;
        joints[joint].z = (float)kp.getProbability() * 1000f; // pseudo-depth
        jointConfidence[joint] = kp.getProbability();
    }

    private KeyPointResult mid(KeyPointResult a, KeyPointResult... rest) {
        if (a == null) return null;
        float x = a.getX(), y = a.getY(), p = a.getProbability(), n = 1;
        for (KeyPointResult k : rest) {
            if (k == null) continue;
            x += k.getX(); y += k.getY(); p += k.getProbability(); n++;
        }
        KeyPointResult out = new KeyPointResult(0, (int)(x / n), (int)(y / n), p / n);
        return out;
    }

    private void fireUserCallback(Method cb, int userId) {
        if (cb == null) return;
        try {
            if (cb.getParameterCount() == 2) cb.invoke(parent, userId, 0);
            else cb.invoke(parent, userId);
        } catch (Exception e) { /* ignore */ }
    }

    // ---- image accessors ----

    public PImage rgbImage() { return rgbImage; }

    public int[] rgbMap() { return toIntMap(rgbImage); }

    public PImage depthImage() {
        if (depthNet == null || rgbImage == null) return depthImage;
        try {
            ImageResult depth = depthNet.run(rgbImage);
            PImage out = depth.getImage();
            mirrorPixels(out);
            depthImage = out;
        } catch (Exception e) {
            System.err.println("SimpleOpenEyes depth run: " + e.getMessage());
        }
        return depthImage;
    }

    public int[] depthMap() {
        PImage d = depthImage();
        return d == null ? null : toIntMap(d);
    }

    /** Rough depths, no calibration. Values in arbitrary MiDaS units. */
    public int[] depthMapRealWorld() { return depthMap(); }

    public int userWidth() { return rgbImage == null ? 0 : rgbImage.width; }
    public int userHeight() { return rgbImage == null ? 0 : rgbImage.height; }
    public int rgbWidth() { return cam == null ? 0 : cam.width; }
    public int rgbHeight() { return cam == null ? 0 : cam.height; }
    public int depthWidth() { return rgbImage == null ? 0 : rgbImage.width; }
    public int depthHeight() { return rgbImage == null ? 0 : rgbImage.height; }
    public int irWidth() { return 0; }
    public int irHeight() { return 0; }

    // ---- user / skeleton API (mirrors SimpleOpenNI) ----

    public int getNumberOfUsers() { return trackingUserId != -1 ? 1 : 0; }

    public int[] getUsers() {
        return trackingUserId != -1 ? new int[]{trackingUserId} : new int[0];
    }

    public void getCoM(int userId, PVector com) {
        PVector c = centerOfMass();
        if (com != null && c != null) com.set(c);
    }

    public PVector getCoM(int userId) { return centerOfMass(); }

    private PVector centerOfMass() {
        if (trackingUserId == -1 || joints == null) return null;
        PVector sum = new PVector(); int n = 0;
        for (int i = 0; i < SKEL_COUNT; i++) {
            if (jointConfidence[i] > 0.2f) { sum.add(joints[i]); n++; }
        }
        return n > 0 ? sum.div(n) : null;
    }

    public boolean getBoundingBox(int userId, PVector min, PVector max) {
        if (trackingUserId == -1) return false;
        PVector lo = new PVector(Float.MAX_VALUE, Float.MAX_VALUE);
        PVector hi = new PVector(-Float.MAX_VALUE, -Float.MAX_VALUE);
        for (int i = 0; i < SKEL_COUNT; i++) {
            if (jointConfidence[i] < 0.2f) continue;
            lo.x = Math.min(lo.x, joints[i].x); lo.y = Math.min(lo.y, joints[i].y);
            hi.x = Math.max(hi.x, joints[i].x); hi.y = Math.max(hi.y, joints[i].y);
        }
        if (min != null) min.set(lo);
        if (max != null) max.set(hi);
        return true;
    }

    public int[] userMap() { PImage u = userImage(); return toIntMap(u); }

    public PImage userImage() {
        // Simple stand-in: mask = bounding box of tracked person, white where inside
        PImage out = parent.createImage(userWidth(), userHeight(), PConstants.RGB);
        PVector lo = new PVector(), hi = new PVector();
        boolean ok = trackingUserId != -1 && getBoundingBox(trackingUserId, lo, hi);
        out.loadPixels();
        for (int i = 0; i < out.pixels.length; i++) {
            int x = i % out.width, y = i / out.width;
            boolean box = ok && x >= lo.x && x <= hi.x && y >= lo.y && y <= hi.y;
            out.pixels[i] = box ? 0xFFFFFFFF : 0;
        }
        out.updatePixels();
        return out;
    }

    // ---- user / skeleton API (mirrors SimpleOpenNI) ----

    public boolean isTrackingSkeleton(int userId) { return trackingUserId == userId; }

    public boolean startTrackingSkeleton(int userId) { return trackingUserId != -1; }

    public void stopTrackingSkeleton(int userId) {}

    public void requestCalibrationSkeleton(int userId, boolean force) {}

    public void getJointPositionSkeleton(int userId, int joint, PVector out) {
        if (joints != null && joint >= 0 && joint < SKEL_COUNT) out.set(joints[joint]);
    }

    public PVector getJointPositionSkeleton(int userId, int joint) {
        return (joints != null && joint >= 0 && joint < SKEL_COUNT) ? joints[joint] : new PVector();
    }

    public void getJointOrientationSkeleton(int userId, int joint, PMatrix3D out) {
        // orientation not supported by single-image pose; leaves matrix unchanged
    }

    public float getConfidence(int joint) {
        return (joints != null && joint >= 0 && joint < SKEL_COUNT) ? jointConfidence[joint] : 0;
    }

    // ---- coordinate conversion (2D identity in RGB space; z stays as-is) ----

    public void convertRealWorldToProjective(PVector realWorld, PVector projective) {
        projective.set(realWorld);
    }

    public void convertProjectiveToRealWorld(PVector projective, PVector realWorld) {
        realWorld.set(projective);
    }

    public boolean setMirror(boolean mirror) { this.mirror = mirror; return true; }

    public boolean mirror() { return mirror; }

    public void close() { dispose(); }

    public int framesPlayer() { return 0; }
    public int curFramePlayer() { return 0; }
    public boolean isInit() { return cam != null && cam.width > 0; }

    public int deviceCount() {
        try {
            return Capture.list().length;
        } catch (Exception e) {
            return 0;
        }
    }

    public String[] deviceNames() {
        try {
            return Capture.list();
        } catch (Exception e) {
            return new String[0];
        }
    }

    // ---- helpers ----

    private int[] toIntMap(PImage img) {
        if (img == null) return new int[0];
        int[] out = new int[img.width * img.height];
        img.loadPixels();
        System.arraycopy(img.pixels, 0, out, 0, Math.min(out.length, img.pixels.length));
        return out;
    }

    private void mirrorPixels(PImage img) {
        img.loadPixels();
        int w = img.width, h = img.height;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w / 2; x++) {
                int i = y * w + x;
                int j = y * w + (w - 1 - x);
                int t = img.pixels[i]; img.pixels[i] = img.pixels[j]; img.pixels[j] = t;
            }
        }
        img.updatePixels();
    }

    boolean inside;
}
