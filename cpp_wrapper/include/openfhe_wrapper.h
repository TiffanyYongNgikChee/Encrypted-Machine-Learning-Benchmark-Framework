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


#ifdef __cplusplus
}
#endif

#endif // OPENFHE_WRAPPER_H