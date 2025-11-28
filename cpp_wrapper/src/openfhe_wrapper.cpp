#include "../include/openfhe_wrapper.h"

// OpenFHE headers - include lat-backend.h which defines DCRTPoly
#include "openfhe/core/lattice/hal/lat-backend.h"
#include "openfhe/pke/constants-defs.h"
#include "openfhe/pke/encoding/plaintext-fwd.h"
#include "openfhe/pke/scheme/gen-cryptocontext-params.h"
#include "openfhe/pke/openfhe.h"
#include "openfhe/pke/scheme/bfvrns/gen-cryptocontext-bfvrns-params.h"
#include "openfhe/pke/scheme/bfvrns/gen-cryptocontext-bfvrns.h"
#include "openfhe/pke/encoding/plaintext.h"  
#include "openfhe/pke/ciphertext.h"                  

// Standard headers
#include <string>
#include <memory>
#include <vector>
#include <cstring>

using namespace lbcrypto;

// Internal Structures
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

// Error Handling
static thread_local std::string last_error;

extern "C" const char* openfhe_get_last_error() {
    return last_error.c_str();
}

static void set_error(const std::string& error) {
    last_error = error;
}

// Context Management Implementation
extern "C" OpenFHEContext* openfhe_create_bfv_context(
    uint64_t plaintext_modulus,
    uint32_t multiplicative_depth
) {
    try {
        // Create encryption parameters for BFV
        CCParams<CryptoContextBFVRNS> parameters;
        parameters.SetPlaintextModulus(plaintext_modulus);
        parameters.SetMultiplicativeDepth(multiplicative_depth);
        
        // Generate crypto context
        CryptoContext<DCRTPoly> cryptoContext = GenCryptoContext(parameters);
        
        // Enable features
        cryptoContext->Enable(PKE);
        cryptoContext->Enable(KEYSWITCH);
        cryptoContext->Enable(LEVELEDSHE);
        
        // Allocate and return
        OpenFHEContext* ctx = new OpenFHEContext();
        ctx->cryptoContext = cryptoContext;
        
        set_error("");
        return ctx;
        
    } catch (const std::exception& e) {
        set_error(std::string("Failed to create context: ") + e.what());
        return nullptr;
    }
}

extern "C" void openfhe_destroy_context(OpenFHEContext* ctx) {
    if (ctx) {
        delete ctx;
    }
}

// Key Management Implementation
extern "C" OpenFHEKeyPair* openfhe_generate_keypair(OpenFHEContext* ctx) {
    if (!ctx) {
        set_error("Invalid context");
        return nullptr;
    }
    
    try {
        // Generate key pair
        KeyPair<DCRTPoly> keyPair = ctx->cryptoContext->KeyGen();
        
        // Generate evaluation key for multiplication
        ctx->cryptoContext->EvalMultKeyGen(keyPair.secretKey);
        
        // Allocate and return
        OpenFHEKeyPair* kp = new OpenFHEKeyPair();
        kp->keyPair = keyPair;
        kp->ctx = ctx;
        
        set_error("");
        return kp;
        
    } catch (const std::exception& e) {
        set_error(std::string("Failed to generate keys: ") + e.what());
        return nullptr;
    }
}

extern "C" void openfhe_destroy_keypair(OpenFHEKeyPair* keypair) {
    if (keypair) {
        delete keypair;
    }
}

// Plaintext Operations Implementation
// ============================================

extern "C" OpenFHEPlaintext* openfhe_create_plaintext(
    OpenFHEContext* ctx,
    const int64_t* values,
    size_t length
) {
    if (!ctx || !values) {
        set_error("Invalid parameters");
        return nullptr;
    }
    
    try {
        // Convert to vector
        std::vector<int64_t> vec(values, values + length);
        
        // Create packed plaintext
        Plaintext plaintext = ctx->cryptoContext->MakePackedPlaintext(vec);
        
        // Allocate and return
        OpenFHEPlaintext* plain = new OpenFHEPlaintext();
        plain->plaintext = plaintext;
        
        set_error("");
        return plain;
        
    } catch (const std::exception& e) {
        set_error(std::string("Failed to create plaintext: ") + e.what());
        return nullptr;
    }
}

extern "C" void openfhe_destroy_plaintext(OpenFHEPlaintext* plain) {
    if (plain) {
        delete plain;
    }
}

extern "C" bool openfhe_get_plaintext_values(
    OpenFHEPlaintext* plain,
    int64_t* out_values,
    size_t* out_length
) {
    if (!plain || !out_values || !out_length) {
        set_error("Invalid parameters");
        return false;
    }
    
    try {
        // plain->plaintext is already a Plaintext (shared_ptr)
        // Just call GetPackedValue() directly
        const std::vector<int64_t>& vec = plain->plaintext->GetPackedValue();
        
        // Copy to output buffer
        size_t copy_length = std::min(*out_length, vec.size());
        std::memcpy(out_values, vec.data(), copy_length * sizeof(int64_t));
        *out_length = copy_length;
        
        set_error("");
        return true;
        
    } catch (const std::exception& e) {
        set_error(std::string("Failed to get plaintext values: ") + e.what());
        return false;
    }
}