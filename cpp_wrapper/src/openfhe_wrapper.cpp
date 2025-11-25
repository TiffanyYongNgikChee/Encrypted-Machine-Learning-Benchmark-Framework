#include "../include/openfhe_wrapper.h"

// OpenFHE headers - include lat-backend.h which defines DCRTPoly
#include "openfhe/core/lattice/hal/lat-backend.h"
#include "openfhe/pke/encoding/plaintext-fwd.h"
#include "openfhe/pke/openfhe.h"

// Standard headers
#include <string>
#include <memory>
#include <vector>
#include <cstring>

using namespace lbcrypto;

// Internal Structures
// ============================================

struct OpenFHEContext {
    CryptoContext<DCRTPoly> cryptoContext;
};

struct OpenFHEKeyPair {
    KeyPair<DCRTPoly> keyPair;
    OpenFHEContext* ctx;  // Reference to parent context
};

struct OpenFHEPlaintext {
    Plaintext plaintext;
};

struct OpenFHECiphertext {
    Ciphertext<DCRTPoly> ciphertext;
};
