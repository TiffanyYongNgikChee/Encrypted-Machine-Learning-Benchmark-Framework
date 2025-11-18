// build.rs - Tells Cargo how to link C++ library (macOS version)

fn main() {
    // ============================================
    // SEAL Wrapper Linking
    // ============================================
    println!("cargo:rustc-link-search=native=cpp_wrapper/build");
    println!("cargo:rustc-link-lib=dylib=seal_wrapper");
    
    println!("cargo:rustc-link-search=native=/usr/local/lib");
    println!("cargo:rustc-link-lib=dylib=seal-4.1");
    
    // ============================================
    // HElib Wrapper Linking
    // ============================================
    println!("cargo:rustc-link-search=native=helib_wrapper/build");
    println!("cargo:rustc-link-lib=dylib=helib_wrapper");
    
    // HElib libraries (from package build)
    println!("cargo:rustc-link-search=native=/usr/local/helib_pack/lib");
    println!("cargo:rustc-link-lib=dylib=helib");
    println!("cargo:rustc-link-lib=dylib=ntl");
    println!("cargo:rustc-link-lib=dylib=gmp");
    
    // ============================================
    // Common Dependencies
    // ============================================
    println!("cargo:rustc-link-lib=stdc++");
    println!("cargo:rustc-link-lib=pthread");
    
    // Set rpath for runtime library discovery
    println!("cargo:rustc-link-arg=-Wl,-rpath,$ORIGIN/cpp_wrapper/build");
    println!("cargo:rustc-link-arg=-Wl,-rpath,$ORIGIN/helib_wrapper/build");
    println!("cargo:rustc-link-arg=-Wl,-rpath,/usr/local/lib");
    println!("cargo:rustc-link-arg=-Wl,-rpath,/usr/local/helib_pack/lib");
    
    // ============================================
    // Rerun Triggers
    // ============================================
    println!("cargo:rerun-if-changed=cpp_wrapper/src/seal_wrapper.cpp");
    println!("cargo:rerun-if-changed=cpp_wrapper/include/seal_wrapper.h");
    println!("cargo:rerun-if-changed=helib_wrapper/src/helib_wrapper.cpp");
    println!("cargo:rerun-if-changed=helib_wrapper/include/helib_wrapper.h");
}