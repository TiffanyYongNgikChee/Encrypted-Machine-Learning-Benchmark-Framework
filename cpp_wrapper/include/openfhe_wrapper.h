#ifndef OPENFHE_WRAPPER_H
#define OPENFHE_WRAPPER_H

#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

// Opaque Pointers (hide C++ from Rust)
typedef struct OpenFHEContext OpenFHEContext;
typedef struct OpenFHEKeyPair OpenFHEKeyPair;
typedef struct OpenFHEPlaintext OpenFHEPlaintext;
typedef struct OpenFHECiphertext OpenFHECiphertext;

// Context Management
/// Create a new OpenFHE BFV context
/// @param plaintext_modulus: Plaintext modulus (e.g., 65537)
/// @param multiplicative_depth: Multiplicative depth (e.g., 2)
/// @return Pointer to context or NULL on failure
OpenFHEContext* openfhe_create_bfv_context(
    uint64_t plaintext_modulus,
    uint32_t multiplicative_depth
);

/// Destroy context and free memory
void openfhe_destroy_context(OpenFHEContext* ctx);

#ifdef __cplusplus
}
#endif

#endif // OPENFHE_WRAPPER_H