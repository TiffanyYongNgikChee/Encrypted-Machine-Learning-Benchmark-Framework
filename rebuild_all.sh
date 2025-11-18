#!/bin/bash
set -e  # Exit on error

echo "=== Rebuilding NTL with -fPIC ==="
cd ~/ntl-11.5.1/src
make clean

./configure \
    PREFIX=/usr/local \
    NTL_GMP_LIP=on \
    NTL_THREADS=on \
    CXXFLAGS="-O2 -fPIC" \
    SHARED=on
echo ""
echo "Building NTL..."
make -j$(nproc)

echo ""
echo "Installing NTL..."
sudo make install
sudo ldconfig

echo ""
echo "NTL installed with -fPIC"
ldconfig -p | grep ntl

echo ""
echo "=== Rebuilding HElib ==="
cd ~/HElib/build
rm -rf *

cmake .. \
    -DCMAKE_BUILD_TYPE=Release \
    -DPACKAGE_BUILD=ON \
    -DENABLE_THREADS=ON

echo ""
echo "Building HElib..."
make -j$(nproc)

echo ""
echo "Installing HElib..."
sudo make install
sudo ldconfig

echo ""
echo "HElib installed"
ldconfig -p | grep helib

echo ""
echo "=== Rebuilding HElib Wrapper ==="
cd /workspaces/he-benchmark-spike/helib_wrapper/build
rm -rf *

cmake ..

echo ""
echo "Building wrapper..."
make

if [ -f libhelib_wrapper.so ]; then
    echo ""
    echo "Wrapper built successfully!"
    ls -lh libhelib_wrapper.so
    
    cp libhelib_wrapper.so ../..
    echo ""
    echo "Copied to project root"
    ls -lh /workspaces/he-benchmark-spike/libhelib_wrapper.so
    
    echo ""
    echo "Checking exports:"
    nm -D libhelib_wrapper.so | grep helib_ | head -5
else
    echo ""
    echo "Wrapper build failed"
    exit 1
fi

echo ""
echo "All done! NTL, HElib, and wrapper rebuilt successfully."
echo ""

