<p align="center">
  <img src="./ash-logo.png">
</p>

## Install/Run
As of now, there are no prebuilt distributions of ASH available. So clone the repository and start the server.
```
sbt ashJVM/run
```

This will compile the Scala.js client code as well as boot up a Play! server. To use ASH, navigate your browser to http://localhost:9000/fast.

## Built in Services
+ Spotify
  + When ASH is first loaded, it will attempt to open Spotify
  + If ASH acquires a connection to a desktop Spotify client on the computer it is running on, it will show current song details and control buttons
+ Remote Motor
  + If ASH detects a computer on the network that is running the motor-remote project (in the motor-remote SBT subproject), it will connect to it and allow control of the motor through the web interface
