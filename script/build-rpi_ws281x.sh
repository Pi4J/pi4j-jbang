#!/bin/bash
sudo apt update
sudo apt install scons git build-essential
sudo apt install raspberrypi-kernel-headers

# Clone the repository, using a community fork with RPi 5 support
git clone https://github.com/richardghirst/rpi_ws281x.git
cd rpi_ws281x

# Build the library
sudo scons

# Install the library system-wide
sudo cp libws2811.so.1 /usr/local/lib/
sudo ln -s /usr/local/lib/libws2811.so.1 /usr/local/lib/libws2811.so
sudo ldconfig

# Copy header files
sudo mkdir -p /usr/local/include/ws2811
sudo cp *.h /usr/local/include/ws2811/

# Check if library is accessible
ldconfig -p | grep ws2811

# Test with the C example
cd rpi_ws281x
sudo ./test
