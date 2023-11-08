# Nokia_project_ThermalCamera

## Project description
Android application to measure user's temperature and indicate for higher than normal temperature.<br/>
Application produced in cooperation with LeViteZer and Metropolia University of Applied Sciences.<br/>

## Development environment
Created using Android Studio (v. 4.1.1).<br/>

## Dependencies
For all dependencies, check build.gradle file in heatcam/app folder.

### Main dependencies
Face detection uses Google's MLKit (https://developers.google.com/ml-kit/vision/face-detection/android)
```
implementation 'com.google.mlkit:face-detection:16.0.2'
```
Camera api used is CameraX api (https://developer.android.com/training/camerax)
```
implementation "androidx.camera:camera-camera2:1.0.0-beta05"
implementation "androidx.camera:camera-lifecycle:1.0.0-beta05"
implementation "androidx.camera:camera-view:1.0.0-alpha12"
implementation "androidx.camera:camera-extensions:1.0.0-alpha12"
```
USB serial library by mik3y (https://github.com/mik3y/usb-serial-for-android)
```
implementation 'com.github.mik3y:usb-serial-for-android:v3.0.1'
```
MPAndroidChart library used for displaying charts by PhilJay (https://github.com/PhilJay/MPAndroidChart)
```
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
```

## Project structure

Project containts two folders Measuerement and PrivateKeyboard to seperate these. However resources such as strings etc. are stored in same files. Layouts are named **heatcam_** and **pkb_** based on wich one of the previously mentioned projects they are used.


##### 1 Heatcam Measurement App
Structure:

- BroadcastReceivers `Contains intent recievers`
- FaceDetector `Contains classes used for detecting faces`
- Fragments `UI` `Contains Fragment classes that are changed during the application`
- FrontCamera `Currently contains property class for front camera that are called once in app lifecycle.`
- Main `Contains root activity.`
- ThermalCamera
  - SerialListeners
  	- `Listeners that listens SerialPortModels IO Manager onNewData method.`
  	- `These classes handles the thermal image feed and temperature.`
  	- `Only 16Bit version is currently in use.`
  - SerialPort
    - SerialPortModel.java `Singleton class that handles IO with serial USB`
- Utils

##### 2 PrivateKeyboard
Structure:
- Data
- Helpers

Visitor Card

There are two ways to present name of host company on the Visitor Card:

- by adding or changing a logo of host company

add the picture of logo (*.png) into heatcam/app/src/main/res/drawable
go to "heatcam_intro_fragment.xml" line 115 app:srcCompat="@drawable/levitezer_logo" and change the "levitezer_logo" by the new one.

- by adding name of host company

uncomment the lines from 123 to 130 in "src/main/res/layout/pkb_activity_main.xml"
uncomment the lines 407 in "src/main/java/com/example/heatcam/PrivateKeyboard/MainActivity.java"

How to remove the logo of host company from the Visitor Card

comment the lines from 110 to 115 in "src/main/res/layout/pkb_activity_main.xml"

