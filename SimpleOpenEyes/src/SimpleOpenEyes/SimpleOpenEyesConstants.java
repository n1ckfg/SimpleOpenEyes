package SimpleOpenEyes;

/**
 * Constants matching SimpleOpenNI's joint/node ids so sketches port unchanged.
 * Values follow OpenNI XnSkeletonJoint numbering.
 */
public interface SimpleOpenEyesConstants {
    public static final int SKEL_HEAD = 1;
    public static final int SKEL_NECK = 2;
    public static final int SKEL_TORSO = 3;
    public static final int SKEL_LEFT_SHOULDER = 6;
    public static final int SKEL_LEFT_ELBOW = 7;
    public static final int SKEL_LEFT_HAND = 9;
    public static final int SKEL_LEFT_FINGERTIP = 10;
    public static final int SKEL_RIGHT_SHOULDER = 12;
    public static final int SKEL_RIGHT_ELBOW = 13;
    public static final int SKEL_RIGHT_HAND = 15;
    public static final int SKEL_RIGHT_FINGERTIP = 16;
    public static final int SKEL_LEFT_HIP = 17;
    public static final int SKEL_LEFT_KNEE = 18;
    public static final int SKEL_LEFT_FOOT = 20;
    public static final int SKEL_RIGHT_HIP = 21;
    public static final int SKEL_RIGHT_KNEE = 22;
    public static final int SKEL_RIGHT_FOOT = 24;
    public static final int SKEL_COUNT = 25;

    public static final int GESTURE_WAVE = 0;
    public static final int GESTURE_CLICK = 1;
    public static final int GESTURE_HAND_RAISE = 2;

    public static final int NODE_NONE = 0;
    public static final int NODE_DEPTH = 1;
    public static final int NODE_IMAGE = 2;
    public static final int NODE_IR = 3;
    public static final int NODE_SCENE = 4;
    public static final int NODE_USER = 5;
    public static final int NODE_HANDS = 6;
    public static final int NODE_GESTURE = 7;
    public static final int NODE_RECORDER = 8;
    public static final int NODE_PLAYER = 9;

    public static final int IMG_MODE_DEFAULT = 0;
    public static final int IMG_MODE_RGB_FADE = 1;

    public static final int RUN_MODE_DEFAULT = 0;
    public static final int RUN_MODE_SINGLE_THREADED = 1;
    public static final int RUN_MODE_MULTI_THREADED = 2;
}
