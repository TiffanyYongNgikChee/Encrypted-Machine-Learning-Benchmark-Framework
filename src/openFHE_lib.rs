//! Safe Rust wrapper for OpenFHE homomorphic encryption library
//! 
//! This module provides a safe, idiomatic Rust interface to OpenFHE.

mod ffi;

use std::ffi::CStr;
use std::ptr::NonNull;

// Error Types
#[derive(Debug)]
pub enum OpenFHEError {
    NullPointer,
    InvalidParameter,
    EncryptionFailed,
    DecryptionFailed,
    OperationFailed,
    Unknown(String),
}

impl std::fmt::Display for OpenFHEError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::NullPointer => write!(f, "Null pointer returned from OpenFHE"),
            Self::InvalidParameter => write!(f, "Invalid parameter"),
            Self::EncryptionFailed => write!(f, "Encryption failed"),
            Self::DecryptionFailed => write!(f, "Decryption failed"),
            Self::OperationFailed => write!(f, "Operation failed"),
            Self::Unknown(msg) => write!(f, "Unknown error: {}", msg),
        }
    }
}

impl std::error::Error for OpenFHEError {}

pub type Result<T> = std::result::Result<T, OpenFHEError>;

/// Get last error from OpenFHE
fn get_last_error() -> String {
    unsafe {
        let err_ptr = ffi::openfhe_get_last_error();
        if err_ptr.is_null() {
            return String::from("Unknown error");
        }
        CStr::from_ptr(err_ptr)
            .to_string_lossy()
            .into_owned()
    }
}