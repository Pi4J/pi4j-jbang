
Pi4J V2 :: Java I/O Library for Raspberry Pi :: JBang example code
==================================================================

[![Chat on Slack](https://img.shields.io/badge/Chat-on%20Slack-blue)](https://join.slack.com/t/pi4j/shared_invite/zt-1ttqt8wgj-E6t69qaLrNuCMPLiYnBCsg)
[![Site](https://img.shields.io/badge/Website-pi4j.com-green)](https://pi4j.com)
[![Twitter Follow](https://img.shields.io/twitter/follow/pi4j?label=Pi4J&style=social)](https://twitter.com/pi4j)

This project contains several example applications that you can run as single files with JBang, using the Pi4J (V2) as demonstrated on Voxxed Days Brussels on May 23th 2023.

[![Watch the video](https://img.youtube.com/vi/w4AR4hWP3Qk/maxresdefault.jpg)](https://youtu.be/w4AR4hWP3Qk)

More info is available on [pi4j.com: "Running Pi4J with JBang"](https://pi4j.com/documentation/building/jbang/).

## PREREQUISITES

* A Raspberry Pi with recent Raspberry Pi OS.
* Install JBang as described on [jbang.dev/download](https://www.jbang.dev/download/).
  * JBang will install Java if it's not installed yet.
* Use [Visual Studio Code](https://code.visualstudio.com/), the free IDE, with the following extensions:
  * [Language Support for Java(TM) by Red Hat](https://marketplace.visualstudio.com/items?itemName=redhat.java)
  * [JBang](https://marketplace.visualstudio.com/items?itemName=jbangdev.jbang-vscode)
  * Install VSC with `sudo apt install code`

## SCRIPTS

This project contains several examples to demonstrate both JBang and Pi4J:

* HelloWorld.java: basic Java example
* JsonParsing.java: shows how to use dependencies, can be executed on any computer
* Pi4JMinimalExample.java: basic example with Pi4J and a LED (DigitalOutput) and button (DigitalInput)
* Pi4JTempHumPressI2C.java: reading temperature, humidity and pressure from a BME280 sensor via I2C
* Pi4JTempHumPressSpi.java: reading temperature, humidity and pressure from a BME280 sensor via SPI

Execute any of the following examples like this:

```bash
git clone https://github.com/Pi4J/pi4j-jbang
cd pi4j-jbang
jbang SCRIPT.java
// For example
jbang HelloWorld.java
```

## LICENSE

Pi4J Version 2.0 and later is licensed under the Apache License,
Version 2.0 (the "License"); you may not use this file except in
compliance with the License.  You may obtain a copy of the License at:
http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

