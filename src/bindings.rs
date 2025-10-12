//! Raw FFI bindings to SEAL C wrapper

use std::os::raw::{c_char, c_uint, c_ulonglong};

// ============================================
// Opaque Types (match C header)
// ============================================
#[repr(C)]
pub struct SEALContext {
    _private: [u8; 0],
}

#[repr(C)]
pub struct SEALEncryptor {
    _private: [u8; 0],
}

#[repr(C)]
pub struct SEALDecryptor {
    _private: [u8; 0],
}

#[repr(C)]
pub struct SEALCiphertext {
    _private: [u8; 0],
}

#[repr(C)]
pub struct SEALPlaintext {
    _private: [u8; 0],
}
