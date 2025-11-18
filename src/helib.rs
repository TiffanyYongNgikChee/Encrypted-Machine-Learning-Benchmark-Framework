//! Safe Rust wrapper for HElib

use crate::helib_bindings;
use std::ptr::NonNull;

// Error Types
#[derive(Debug)]
pub enum HElibError {
    NullPointer,
    EncryptionFailed,
    DecryptionFailed,
    OperationFailed,
}

pub type Result<T> = std::result::Result<T, HElibError>;

// Context
pub struct HEContext {
    ptr: NonNull<helib_bindings::HElibContext>,
}

impl HEContext {
    /// Create new HElib context
    /// 
    /// # Parameters
    /// - m: Cyclotomic polynomial (e.g., 4095)
    /// - p: Plaintext modulus (e.g., 2 for binary, 257 for integers)
    /// - r: Lifting (typically 1)
    pub fn new(m: u64, p: u64, r: u64) -> Result<Self> {
        let ptr = unsafe {
            helib_bindings::helib_create_context(m, p, r)
        };
        
        NonNull::new(ptr)
            .map(|ptr| HEContext { ptr })
            .ok_or(HElibError::NullPointer)
    }
}

impl Drop for HEContext {
    fn drop(&mut self) {
        unsafe {
            helib_bindings::helib_destroy_context(self.ptr.as_ptr());
        }
    }
}
