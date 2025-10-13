//! Safe Rust wrapper for SEAL homomorphic encryption library
//! 
//! This module provides a safe, idiomatic Rust interface to Microsoft SEAL.

mod bindings;

use std::ffi::{CStr, CString};
use std::ptr::NonNull;

// ============================================
// Error Types
// ============================================
#[derive(Debug)]
pub enum SealError {
    NullPointer,
    InvalidParameter,
    EncryptionFailed,
    DecryptionFailed,
    OperationFailed,
}

pub type Result<T> = std::result::Result<T, SealError>;

// ============================================
// Context (owns SEAL context and keys)
// ============================================
pub struct Context {
    ptr: NonNull<bindings::SEALContext>,
}

impl Context {
    /// Create a new SEAL context with BFV scheme
    /// 
    /// # Parameters
    /// - poly_modulus_degree: Polynomial modulus degree (e.g., 4096, 8192)
    /// - plain_modulus: Plaintext modulus for BFV
    pub fn new(poly_modulus_degree: u64, plain_modulus: u64) -> Result<Self> {
        // Standard coefficient modulus for given poly degree
        let coeff_modulus = vec![60, 40, 40, 60]; // bits per prime
        
        let ptr = unsafe {
            bindings::seal_create_context(
                poly_modulus_degree,
                coeff_modulus.as_ptr(),
                coeff_modulus.len(),
                plain_modulus,
            )
        };
        
        NonNull::new(ptr)
            .map(|ptr| Context { ptr })
            .ok_or(SealError::NullPointer)
    }
}

impl Drop for Context {
    fn drop(&mut self) {
        unsafe {
            bindings::seal_destroy_context(self.ptr.as_ptr());
        }
    }
}