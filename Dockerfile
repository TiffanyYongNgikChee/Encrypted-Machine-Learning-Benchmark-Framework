FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

# Install base dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    git \
    curl \
    wget \
    libgmp-dev \
    m4 \
    perl \
    pkg-config \
    sudo \
    autoconf \
    libtool \
    python3 \
    python3-pip \
    patchelf \
    && rm -rf /var/lib/apt/lists/*

# Install Rust
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
ENV PATH="/root/.cargo/bin:${PATH}"

WORKDIR /root

# Build NTL (required by HElib)
RUN echo "=== Building NTL ===" && \
    wget -q https://libntl.org/ntl-11.5.1.tar.gz && \
    tar -xzf ntl-11.5.1.tar.gz && \
    cd ntl-11.5.1/src && \
    ./configure PREFIX=/usr/local NTL_GMP_LIP=on NTL_THREADS=on CXXFLAGS="-O2 -fPIC" SHARED=on > /dev/null && \
    make -j4 > /dev/null 2>&1 && \
    make install > /dev/null && \
    ldconfig && \
    echo "NTL installed successfully" && \
    cd /root && rm -rf ntl-11.5.1*

# Build HElib 
RUN echo "=== Cloning HElib ===" && \
    git clone --depth 1 --branch v2.3.0 https://github.com/homenc/HElib.git && \
    echo "HElib cloned successfully"

RUN echo "=== Configuring HElib ===" && \
    cd /root/HElib && \
    mkdir -p build && \
    cd build && \
    cmake .. \
      -DCMAKE_BUILD_TYPE=Release \
      -DPACKAGE_BUILD=ON \
      -DENABLE_THREADS=ON \
      -DCMAKE_INSTALL_PREFIX=/usr/local/helib_pack \
      -DBUILD_SHARED=ON && \
    echo "HElib configured successfully"

RUN echo "=== Building HElib (this takes 5-10 minutes) ===" && \
    cd /root/HElib/build && \
    make -j4 && \
    echo "HElib built successfully"

RUN echo "=== Installing HElib ===" && \
    cd /root/HElib/build && \
    make install && \
    ldconfig && \
    echo "HElib installed successfully" && \
    cd /root && rm -rf HElib

# Build Microsoft SEAL
RUN echo "=== Cloning Microsoft SEAL ===" && \
    git clone --depth 1 --branch v4.1.1 https://github.com/microsoft/SEAL.git && \
    echo "SEAL cloned successfully"

RUN echo "=== Building SEAL ===" && \
    cd /root/SEAL && \
    cmake -S . -B build \
      -DCMAKE_BUILD_TYPE=Release \
      -DSEAL_USE_INTEL_HEXL=OFF \
      -DSEAL_BUILD_DEPS=ON && \
    cmake --build build -j4 && \
    echo "SEAL built successfully"

RUN echo "=== Installing SEAL ===" && \
    cd /root/SEAL && \
    cmake --install build && \
    ldconfig && \
    echo "SEAL installed successfully" && \
    cd /root && rm -rf SEAL

# Build OpenFHE
RUN echo "=== Cloning OpenFHE v1.2.2 ===" && \
    git clone https://github.com/openfheorg/openfhe-development.git && \
    cd openfhe-development && \
    git fetch --all --tags && \
    git checkout tags/v1.2.2 -b v1.2.2-branch && \
    git log -1 --oneline && \
    echo "OpenFHE v1.2.2 checked out successfully"

RUN echo "=== Building OpenFHE ===" && \
    cd /root/openfhe-development && \
    mkdir -p build && \
    cd build && \
    cmake .. \
      -DCMAKE_BUILD_TYPE=Release \
      -DCMAKE_INSTALL_PREFIX=/usr/local \
      -DBUILD_SHARED_LIBS=ON \
      -DBUILD_STATIC_LIBS=OFF \
      -DBUILD_EXAMPLES=ON \
      -DBUILD_BENCHMARKS=OFF \
      -DWITH_OPENMP=ON \
      -DWITH_TCM=OFF && \
    make -j2 && \
    echo "OpenFHE built successfully"

RUN echo "=== Installing OpenFHE ===" && \
    cd /root/openfhe-development/build && \
    make install && \
    ldconfig && \
    echo "Verifying OpenFHE version..." && \
    cat /usr/local/lib/OpenFHE/OpenFHEConfig.cmake | grep BASE_OPENFHE_VERSION && \
    echo "OpenFHE installed successfully" && \
    cd /root && rm -rf openfhe-development

# Verify library installations
RUN echo "=== Verifying HE libraries ===" && \
    ldconfig -p | grep -E "seal|helib|openfhe" || true && \
    echo "Libraries verified successfully"

# Set environment variables
ENV LD_LIBRARY_PATH=/usr/local/lib:/usr/local/helib_pack/helib_pack/lib:${LD_LIBRARY_PATH}
ENV PKG_CONFIG_PATH=/usr/local/lib/pkgconfig:${PKG_CONFIG_PATH}
ENV RUST_BACKTRACE=1

WORKDIR /app

# Copy ONLY the source files needed to build wrappers
COPY cpp_wrapper ./cpp_wrapper
COPY helib_wrapper ./helib_wrapper
COPY openfhe_cpp_wrapper ./openfhe_cpp_wrapper
COPY Cargo.toml Cargo.lock ./
COPY src ./src
COPY examples ./examples
COPY build.rs ./

# Build the wrappers
RUN echo "=== Building HElib wrapper ===" && \
    cd helib_wrapper && \
    mkdir -p build && \
    cd build && \
    cmake .. -DCMAKE_PREFIX_PATH=/usr/local/helib_pack/helib_pack && \
    make && \
    ls -lh libhelib_wrapper.so && \
    cp libhelib_wrapper.so /app/ && \
    echo "HElib wrapper built successfully"

RUN echo "=== Building SEAL wrapper ===" && \
    cd cpp_wrapper && \
    mkdir -p build && \
    cd build && \
    cmake .. && \
    make && \
    ls -lh libseal_wrapper.so && \
    cp libseal_wrapper.so /app/ && \
    echo "SEAL wrapper built successfully"

RUN echo "=== Building OpenFHE wrapper ===" && \
    cd openfhe_cpp_wrapper && \
    mkdir -p build && \
    cd build && \
    cmake .. \
      -DOpenFHE_DIR=/usr/local/lib/OpenFHE && \
    make -j2 && \
    ls -lh libopenfhe_wrapper.so && \
    cp libopenfhe_wrapper.so /app/ && \
    echo "OpenFHE wrapper built successfully"

# Store compiled libraries in a safe location
RUN mkdir -p /app/lib && \
    cp /app/cpp_wrapper/build/libseal_wrapper.so /app/lib/ && \
    cp /app/helib_wrapper/build/libhelib_wrapper.so /app/lib/ && \
    cp /app/openfhe_cpp_wrapper/build/libopenfhe_wrapper.so* /app/lib/ && \
    echo "Libraries stored in /app/lib"

# Update library search path
ENV LD_LIBRARY_PATH=/app/lib:/usr/local/lib:/usr/local/helib_pack/helib_pack/lib:${LD_LIBRARY_PATH}


# Build the Rust project
RUN echo "=== Building Rust project ===" && \
    cargo build --release && \
    echo "Rust project built successfully"

# Verify all shared libraries are accessible
RUN echo "=== Verifying wrapper libraries ===" && \
    ls -lh /app/*.so && \
    ldd /app/libseal_wrapper.so && \
    ldd /app/libhelib_wrapper.so && \
    ldd /app/libopenfhe_wrapper.so && \
    echo "All wrappers verified successfully"

CMD ["/bin/bash"]