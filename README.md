# SimpleOpenEyes

RGB camera tracking replacement for [SimpleOpenNI](https://github.com/tot-eng/SimpleOpenNI) — no OpenNI, no NiTE, no depth camera. Runs person/skeleton tracking on any ordinary RGB feed, built on [deep-vision-processing](https://github.com/n1ckfg/deep-vision-processing) (JavaCV/OpenCV DNN).

## Install

1. Install [Processing 4](https://processing.org/download) (Java mode).
2. Install the **DeepVision** and **Video** libraries, then install this library into `sketchbook/libraries/SimpleOpenEyes`.
   The source tree layout is already a Processing library:

```
SimpleOpenEyes/
├── library.properties
├── library/SimpleOpenEyes.jar
├── src/SimpleOpenNI/*.java
└── examples/
```

## Usage

Drop-in style, same calls as SimpleOpenNI:

```java
import SimpleOpenNI.*;

SimpleOpenEyes eyes;

void setup() {
  size(640, 480);
  eyes = new SimpleOpenEyes(this);
  eyes.enableRGB();
  eyes.enableUser();
  eyes.init();     // downloads pose model on first run
}

void draw() {
  eyes.update();
  image(eyes.rgbImage(), 0, 0);
  if (eyes.isTrackingSkeleton(1)) drawSkeleton();
}
```

Depth (monocular, relative — not metric):

```java
eyes.enableDepth();
...
PImage depth = eyes.depthImage();
```

Camera source defaults to GStreamer `pipeline:autovideosrc`. Override before `init()`:

```java
eyes.setCameraPipeline("qti-samsung-enc:h264:mipi"); // libcamera Pi example
eyes.setCameraPipeline("/dev/video0");               // v4l2 device
```

## What works vs SimpleOpenNI

| SimpleOpenNI                        | SimpleOpenEyes                              |
|-------------------------------------|---------------------------------------------|
| Hardware depth (projective mm)      | MiDaS relative depth (no metric calibration)|
| `getJointPositionSkeleton` (3D)     | 2D projected joints; z = pseudo-depth proxy |
| `convertRealWorldToProjective`      | identity 2D pass-through                    |
| Skeleton joints (OpenNI ids)        | Lightweight OpenPose LIP 17 keypoints mapped to the same `SKEL_*` constants |
| `onNewUser` / `onLostUser` callbacks| fired on tracking start/end                 |
| `userMap` / `userImage`             | person bounding box mask                    |
| IR / gestures / hands / NiTE        | not supported (dropped)                     |

## Build

```bash
javac -cp "$CORE.jar:$VIDEO.jar:DeepVision/library/*" -d build src/SimpleOpenNI/*.java
jar cf library/SimpleOpenEyes.jar -C build .
```

Processing core 4.5.x, video 3.3.7, deep-vision 0.10.0.

## Plans

- Person-proposal first-pass (YOLO/MobileNet) then pose on crops for multi-person
- Temporal smoothing and joint state propagation
- Real metric depth via camera intrinsics
- Move to lightweight OpenPose variants (432/368) as options

## License

GPL-2 — inherited from SimpleOpenNI, with Classpath exception.
