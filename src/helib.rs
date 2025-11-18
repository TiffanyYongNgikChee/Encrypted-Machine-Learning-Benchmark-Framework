//! Safe Rust wrapper for HElib

use crate::helib_bindings;
use std::ptr::NonNull;

// Error Types
#[derive(Debug)]
pub enum HElibError {
    NullPointer,
    InvalidParameter,
    EncryptionFailed,
    DecryptionFailed,
    OperationFailed,
}

// Implement Display for HElibError
impl std::fmt::Display for HElibError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            HElibError::NullPointer => write!(f, "Null pointer returned from HElib"),
            HElibError::InvalidParameter => write!(f, "Invalid parameter provided"),
            HElibError::EncryptionFailed => write!(f, "Encryption operation failed"),
            HElibError::DecryptionFailed => write!(f, "Decryption operation failed"),
            HElibError::OperationFailed => write!(f, "HElib operation failed"),
        }
    }
}

// Implement Error trait for HElibError
impl std::error::Error for HElibError {}

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

// Secret Key
pub struct HESecretKey {
    ptr: NonNull<helib_bindings::HElibSecretKey>,
}

impl HESecretKey {
    pub fn generate(context: &HEContext) -> Result<Self> {
        let ptr = unsafe {
            helib_bindings::helib_generate_secret_key(context.ptr.as_ptr())
        };
        
        NonNull::new(ptr)
            .map(|ptr| HESecretKey { ptr })
            .ok_or(HElibError::NullPointer)
    }
    
    pub fn public_key(&self) -> Result<HEPublicKey> {
        let ptr = unsafe {
            helib_bindings::helib_get_public_key(self.ptr.as_ptr())
        };
        
        NonNull::new(ptr)
            .map(|ptr| HEPublicKey { ptr })
            .ok_or(HElibError::NullPointer)
    }
    
    pub fn decrypt(&self, ciphertext: &HECiphertext) -> Result<HEPlaintext> {
        let ptr = unsafe {
            helib_bindings::helib_decrypt(
                self.ptr.as_ptr(),
                ciphertext.ptr.as_ptr(),
            )
        };
        
        NonNull::new(ptr)
            .map(|ptr| HEPlaintext { ptr })
            .ok_or(HElibError::DecryptionFailed)
    }
    
    pub fn noise_budget(&self, ciphertext: &HECiphertext) -> i32 {
        unsafe {
            helib_bindings::helib_noise_budget(
                self.ptr.as_ptr(),
                ciphertext.ptr.as_ptr(),
            )
        }
    }
}

impl Drop for HESecretKey {
    fn drop(&mut self) {
        unsafe {
            helib_bindings::helib_destroy_secret_key(self.ptr.as_ptr());
        }
    }
}

// Public Key
pub struct HEPublicKey {
    ptr: NonNull<helib_bindings::HElibPublicKey>,
}

impl HEPublicKey {
    pub fn encrypt(&self, plaintext: &HEPlaintext) -> Result<HECiphertext> {
        let ptr = unsafe {
            helib_bindings::helib_encrypt(
                self.ptr.as_ptr(),
                plaintext.ptr.as_ptr(),
            )
        };
        
        NonNull::new(ptr)
            .map(|ptr| HECiphertext { ptr })
            .ok_or(HElibError::EncryptionFailed)
    }
}

impl Drop for HEPublicKey {
    fn drop(&mut self) {
        unsafe {
            helib_bindings::helib_destroy_public_key(self.ptr.as_ptr());
        }
    }
}

// Plaintext
pub struct HEPlaintext {
    ptr: NonNull<helib_bindings::HElibPlaintext>,
}

impl HEPlaintext {
    pub fn new(context: &HEContext, value: i64) -> Result<Self> {
        let ptr = unsafe {
            helib_bindings::helib_create_plaintext(
                context.ptr.as_ptr(),
                value,
            )
        };
        
        NonNull::new(ptr)
            .map(|ptr| HEPlaintext { ptr })
            .ok_or(HElibError::NullPointer)
    }
    
    pub fn value(&self) -> i64 {
        unsafe {
            helib_bindings::helib_plaintext_to_long(self.ptr.as_ptr())
        }
    }
}

impl Drop for HEPlaintext {
    fn drop(&mut self) {
        unsafe {
            helib_bindings::helib_destroy_plaintext(self.ptr.as_ptr());
        }
    }
}

// Ciphertext
pub struct HECiphertext {
    ptr: NonNull<helib_bindings::HElibCiphertext>,
}

impl HECiphertext {
    /// Homomorphic addition
    pub fn add(&self, other: &HECiphertext) -> Result<HECiphertext> {
        let ptr = unsafe {
            helib_bindings::helib_add(
                self.ptr.as_ptr(),
                other.ptr.as_ptr(),
            )
        };
        
        NonNull::new(ptr)
            .map(|ptr| HECiphertext { ptr })
            .ok_or(HElibError::OperationFailed)
    }
    
    /// Homomorphic multiplication
    pub fn multiply(&self, other: &HECiphertext) -> Result<HECiphertext> {
        let ptr = unsafe {
            helib_bindings::helib_multiply(
                self.ptr.as_ptr(),
                other.ptr.as_ptr(),
            )
        };
        
        NonNull::new(ptr)
            .map(|ptr| HECiphertext { ptr })
            .ok_or(HElibError::OperationFailed)
    }
    
    /// Homomorphic subtraction
    pub fn subtract(&self, other: &HECiphertext) -> Result<HECiphertext> {
        let ptr = unsafe {
            helib_bindings::helib_subtract(
                self.ptr.as_ptr(),
                other.ptr.as_ptr(),
            )
        };
        
        NonNull::new(ptr)
            .map(|ptr| HECiphertext { ptr })
            .ok_or(HElibError::OperationFailed)
    }
}

impl Drop for HECiphertext {
    fn drop(&mut self) {
        unsafe {
            helib_bindings::helib_destroy_ciphertext(self.ptr.as_ptr());
        }
    }
}