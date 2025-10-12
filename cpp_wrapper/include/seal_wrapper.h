#ifndef SEAL_WRAPPER_H
#define SEAL_WRAPPER_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

// ============================================
// Opaque Pointers (hide C++ from Rust)
// ============================================
typedef struct SEALContextWrapper SEALContextWrapper;
typedef struct SEALEncryptor SEALEncryptor;
typedef struct SEALDecryptor SEALDecryptor;
typedef struct SEALCiphertext SEALCiphertext;
typedef struct SEALPlaintext SEALPlaintext;

// ============================================
// Context Management
// ============================================
SEALContextWrapper* seal_create_context(
    uint64_t poly_modulus_degree,
    const uint64_t* coeff_modulus, 
    size_t coeff_modulus_size,
    uint64_t plain_modulus
);
void seal_destroy_context(SEALContextWrapper* ctx);

// ============================================
// Encryption/Decryption Setup
// ============================================
SEALEncryptor* seal_create_encryptor(
    SEALContextWrapper* ctx,
    const uint8_t* public_key,
    size_t public_key_size
);
void seal_destroy_encryptor(SEALEncryptor* enc);

SEALDecryptor* seal_create_decryptor(
    SEALContextWrapper* ctx,
    const uint8_t* secret_key,
    size_t secret_key_size
);
void seal_destroy_decryptor(SEALDecryptor* dec);

// ============================================
// Plaintext Operations
// ============================================
SEALPlaintext* seal_create_plaintext(const char* hex_string);
void seal_destroy_plaintext(SEALPlaintext* plain);
const char* seal_plaintext_to_string(SEALPlaintext* plain);

// ============================================
// Encryption Operations
// ============================================
SEALCiphertext* seal_encrypt(
    SEALEncryptor* encryptor,
    SEALPlaintext* plaintext
);
void seal_destroy_ciphertext(SEALCiphertext* cipher);

// ============================================
// Decryption Operations
// ============================================
SEALPlaintext* seal_decrypt(
    SEALDecryptor* decryptor,
    SEALCiphertext* ciphertext
);

// ============================================
// Homomorphic Operations
// ============================================
SEALCiphertext* seal_add(
    SEALContextWrapper* ctx,
    SEALCiphertext* a,
    SEALCiphertext* b
);

SEALCiphertext* seal_multiply(
    SEALContextWrapper* ctx,
    SEALCiphertext* a,
    SEALCiphertext* b
);

#ifdef __cplusplus
}
#endif

#endif // SEAL_WRAPPER_H