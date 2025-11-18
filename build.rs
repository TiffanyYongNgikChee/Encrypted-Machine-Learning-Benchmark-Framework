// build.rs - Tells Cargo how to link C++ library (macOS version)

fn main() {
    // Link to wrapper library in cpp_wrapper/build
    println!("cargo:rustc-link-search=native=cpp_wrapper/build");
    println!("cargo:rustc-link-lib=dylib=seal_wrapper");
    
    // Link to SEAL library
    println!("cargo:rustc-link-search=native=/usr/local/lib");
    println!("cargo:rustc-link-lib=dylib=seal-4.1");
    
    // Link C++ standard library
    println!("cargo:rustc-link-lib=stdc++");
    
    // Rerun if wrapper changes
    println!("cargo:rerun-if-changed=cpp_wrapper/src/seal_wrapper.cpp");
    println!("cargo:rerun-if-changed=cpp_wrapper/include/seal_wrapper.h");
}