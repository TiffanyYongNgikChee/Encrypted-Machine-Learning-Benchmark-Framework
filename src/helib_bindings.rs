//! Raw FFI bindings to HElib C wrapper

use std::os::raw::c_int;

// Opaque Types
#[repr(C)]
pub struct HElibContext {
    _private: [u8; 0],
}

#[repr(C)]
pub struct HElibSecretKey {
    _private: [u8; 0],
}

#[repr(C)]
pub struct HElibPublicKey {
    _private: [u8; 0],
}

#[repr(C)]
pub struct HElibCiphertext {
    _private: [u8; 0],
}

#[repr(C)]
pub struct HElibPlaintext {
    _private: [u8; 0],
}

// FFI Function Declarations
extern "C" {
    // Context management
    pub fn helib_create_context(
        m: std::os::raw::c_ulong,
        p: std::os::raw::c_ulong,
        r: std::os::raw::c_ulong,
    ) -> *mut HElibContext;
    
    pub fn helib_destroy_context(ctx: *mut HElibContext);
    
    // Key management
    pub fn helib_generate_secret_key(ctx: *mut HElibContext) -> *mut HElibSecretKey;
    pub fn helib_destroy_secret_key(sk: *mut HElibSecretKey);
    
    pub fn helib_get_public_key(sk: *mut HElibSecretKey) -> *mut HElibPublicKey;
    pub fn helib_destroy_public_key(pk: *mut HElibPublicKey);
    
    // Plaintext operations
    pub fn helib_create_plaintext(
        ctx: *mut HElibContext,
        value: std::os::raw::c_long,
    ) -> *mut HElibPlaintext;
    
    pub fn helib_plaintext_to_long(plain: *mut HElibPlaintext) -> std::os::raw::c_long;
    pub fn helib_destroy_plaintext(plain: *mut HElibPlaintext);
    
    // Encryption/Decryption
    pub fn helib_encrypt(
        pk: *mut HElibPublicKey,
        plain: *mut HElibPlaintext,
    ) -> *mut HElibCiphertext;
    
    pub fn helib_decrypt(
        sk: *mut HElibSecretKey,
        cipher: *mut HElibCiphertext,
    ) -> *mut HElibPlaintext;
    
    pub fn helib_destroy_ciphertext(cipher: *mut HElibCiphertext);
    
    // Homomorphic operations
    pub fn helib_add(
        a: *mut HElibCiphertext,
        b: *mut HElibCiphertext,
    ) -> *mut HElibCiphertext;
    
    pub fn helib_multiply(
        a: *mut HElibCiphertext,
        b: *mut HElibCiphertext,
    ) -> *mut HElibCiphertext;
    
    pub fn helib_subtract(
        a: *mut HElibCiphertext,
        b: *mut HElibCiphertext,
    ) -> *mut HElibCiphertext;
    
    // Utilities
    pub fn helib_noise_budget(
        sk: *mut HElibSecretKey,
        cipher: *mut HElibCiphertext,
    ) -> c_int;
}