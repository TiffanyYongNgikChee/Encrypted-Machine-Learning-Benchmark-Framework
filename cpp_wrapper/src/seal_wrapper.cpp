#include "../include/seal_wrapper.h"
#include "seal/seal.h"
#include <memory>
#include <stdexcept>

using namespace seal;
using namespace std;

// ============================================
// Opaque Struct Definitions
// ============================================
struct SEALContext {
    shared_ptr<seal::SEALContext> context;
    shared_ptr<KeyGenerator> keygen;
    PublicKey public_key;
    SecretKey secret_key;
};

struct SEALEncryptor {
    unique_ptr<Encryptor> encryptor;
};

struct SEALDecryptor {
    unique_ptr<Decryptor> decryptor;
};

struct SEALCiphertext {
    Ciphertext ciphertext;
};

struct SEALPlaintext {
    Plaintext plaintext;
};

// ============================================
// Context Management Implementation
// ============================================
extern "C" SEALContext* seal_create_context(
    uint64_t poly_modulus_degree,
    const uint64_t* coeff_modulus_bits, 
    size_t coeff_modulus_size,
    uint64_t plain_modulus_value
) {
    try {
        // Create encryption parameters
        EncryptionParameters parms(scheme_type::bfv);
        parms.set_poly_modulus_degree(poly_modulus_degree);
        
        // Set coefficient modulus
        vector<Modulus> coeff_modulus;
        for (size_t i = 0; i < coeff_modulus_size; i++) {
            coeff_modulus.push_back(Modulus(coeff_modulus_bits[i]));
        }
        parms.set_coeff_modulus(coeff_modulus);
        
        // Set plaintext modulus
        parms.set_plain_modulus(plain_modulus_value);
        
        // Create context
        auto context = make_shared<seal::SEALContext>(parms);
        
        // Generate keys
        KeyGenerator keygen(*context);
        
        // Allocate and populate result
        SEALContext* result = new SEALContext();
        result->context = context;
        result->keygen = make_shared<KeyGenerator>(keygen);
        result->public_key = keygen.create_public_key();
        result->secret_key = keygen.secret_key();
        
        return result;
    } catch (const exception& e) {
        // Error handling
        return nullptr;
    }
}

extern "C" void seal_destroy_context(SEALContext* ctx) {
    if (ctx) delete ctx;
}
