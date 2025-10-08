// build.rs - Tells Cargo how to link C++ library (macOS version)

fn main() {
    // Tell Cargo to link against seal_wrapper
    println!("cargo:rustc-link-search=native=cpp_wrapper/build");
    println!("cargo:rustc-link-lib=dylib=seal_wrapper");
    
    // Tell Cargo to link against SEAL (macOS location)
    println!("cargo:rustc-link-search=native=/usr/local/lib");
    println!("cargo:rustc-link-lib=dylib=seal");
    
    // macOS specific: Add rpath for dynamic libraries
    println!("cargo:rustc-link-arg=-Wl,-rpath,@loader_path");
    println!("cargo:rustc-link-arg=-Wl,-rpath,/usr/local/lib");
    
    // Rerun if wrapper changes
    println!("cargo:rerun-if-changed=cpp_wrapper/src/seal_wrapper.cpp");
    println!("cargo:rerun-if-changed=cpp_wrapper/include/seal_wrapper.h");
}