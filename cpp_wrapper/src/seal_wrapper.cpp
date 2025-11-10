#include "../include/seal_wrapper.h"
#include "seal/seal.h"
#include <memory>
#include <stdexcept>
#include <cstring>

using namespace seal;
using namespace std;

// ============================================
// Opaque Struct Definitions
// ============================================
// These structs act as containers to hold SEAL objects, like the encryption context, keys, encryptor, and decryptor.
// They make sure all the C++ objects stay alive and can be safely accessed from Rust without breaking memory.
struct SEALContextWrapper {
    shared_ptr<seal::SEALContext> seal_context;
    shared_ptr<KeyGenerator> keygen;
    PublicKey public_key;
    SecretKey secret_key;
};

// SEALEncryptor: Wrapper for SEAL's Encryptor object
struct SEALEncryptor {
    unique_ptr<Encryptor> encryptor; // unique_ptr: Only one owner, auto-cleanup
};

// SEALDecryptor: Wrapper for SEAL's Decryptor object
struct SEALDecryptor {
    unique_ptr<Decryptor> decryptor; // unique_ptr: Exclusive ownership
};

// SEALCiphertext: Wrapper for encrypted data
struct SEALCiphertext {
    Ciphertext ciphertext; // not pointer: Ciphertext has proper copy/move semantics
};

// SEALPlaintext: Wrapper for plaintext data (before encryption)
struct SEALPlaintext {
    Plaintext plaintext; // not pointer: Plaintext is lightweight
};

// SEALBatchEncoder: Wrapper for encoding vectors of integers
struct SEALBatchEncoder {
    unique_ptr<BatchEncoder> encoder; // unique_ptr: One encoder per context
};

// SEALGaloisKeys: Wrapper for rotation keys
struct SEALGaloisKeys {
    GaloisKeys keys; // not pointer: GaloisKeys has proper move semantics
};

// ============================================
// Context Management Implementation
// ============================================
// This function creates and sets up the SEAL encryption context.
// It defines the encryption parameters and generates public/secret keys.
extern "C" SEALContextWrapper* seal_create_context(
    
    // poly_modulus_degree: defines polynomial size (affects security and speed)
    uint64_t poly_modulus_degree,
    // coeff_modulus_bits: list of bit sizes for coefficient modulus primes
    const uint64_t* coeff_modulus_bits, 
    // coeff_modulus_size: how many primes are used
    size_t coeff_modulus_size,
    // plain_modulus_value: modulus for plaintext (controls noise level)
    uint64_t plain_modulus_value
) {
    try {
        // Create encryption parameters for BFV(A type of homomorphic encryption that 
        // enables computations on encrypted data, like adding or multiplying numbers while they are still encrypted) scheme
        EncryptionParameters parms(scheme_type::bfv);

        // Set polynomial modulus degree (size of the ciphertext)
        parms.set_poly_modulus_degree(poly_modulus_degree);
        
        // Convert array of coeff_modulus bit sizes into a vector<int>
        // This will be used to generate proper primes
        vector<int> bit_sizes;
        for (size_t i = 0; i < coeff_modulus_size; i++) {
            bit_sizes.push_back(static_cast<int>(coeff_modulus_bits[i]));
        }

        // Create coefficient modulus primes based on bit sizes
        auto coeff_modulus = CoeffModulus::Create(poly_modulus_degree, bit_sizes);
        // Set coefficient modulus into parameters
        parms.set_coeff_modulus(coeff_modulus);
        
        // Set plaintext modulus
        parms.set_plain_modulus(plain_modulus_value);
        
        // Create SEAL context using these parameters
        auto seal_ctx = make_shared<seal::SEALContext>(parms);
        
        // Check if the parameters are valid
        if (!seal_ctx->parameters_set()) {
            // Return null if context creation failed
            return nullptr;
        }
        
        // Generate keys (public + secret)
        KeyGenerator keygen(*seal_ctx);
        
        // Allocate memory for our wrapper struct
        SEALContextWrapper* result = new SEALContextWrapper();

        // Store the context and key generator in the wrapper
        result->seal_context = seal_ctx;
        result->keygen = make_shared<KeyGenerator>(*seal_ctx);

        // Create public key and store in wrapper
        keygen.create_public_key(result->public_key);

        // Store secret key in wrapper
        result->secret_key = keygen.secret_key();
        
        // Return pointer to this wrapper (so Rust can use it)
        return result;
    } catch (const exception& e) {
        // Error handling - could log error here
        return nullptr;
    }
}
// Frees the memory used by the context
extern "C" void seal_destroy_context(SEALContextWrapper* ctx) {
    if (ctx) delete ctx;
}

// ============================================
// Encryptor Implementation
// ============================================

// Creates an encryptor object that can perform encryption
extern "C" SEALEncryptor* seal_create_encryptor(
    SEALContextWrapper* ctx, // existing SEAL context
    const uint8_t* public_key, // not used directly (here for future use)
    size_t public_key_size // same as above
) {
    try {
        // If context is missing, stop
        if (!ctx) return nullptr;
        
        // Create a new encryptor struct
        SEALEncryptor* enc = new SEALEncryptor();
        // Create SEAL encryptor using context and public key
        enc->encryptor = make_unique<Encryptor>(
            *ctx->seal_context, 
            ctx->public_key
        );
        // Return it to Rust
        return enc;
    } catch (...) {
        return nullptr;
    }
}
// Destroys encryptor to free memory
extern "C" void seal_destroy_encryptor(SEALEncryptor* enc) {
    if (enc) delete enc;
}

// ============================================
// Decryptor Implementation
// ============================================

// Creates a decryptor object that can decrypt ciphertexts
extern "C" SEALDecryptor* seal_create_decryptor(
    SEALContextWrapper* ctx, // context created earlier
    const uint8_t* secret_key, // for compatibility, not used directly
    size_t secret_key_size
) {
    try {
        if (!ctx) return nullptr;
        // Create decryptor struct
        SEALDecryptor* dec = new SEALDecryptor();
        // Create SEAL decryptor using context and secret key
        dec->decryptor = make_unique<Decryptor>(
            *ctx->seal_context,
            ctx->secret_key
        );
        
        return dec;
    } catch (...) {
        return nullptr;
    }
}
// Frees decryptor memory
extern "C" void seal_destroy_decryptor(SEALDecryptor* dec) {
    if (dec) delete dec;
}

// Batch Encoder
// PURPOSE: Create encoder for packing multiple integers into one ciphertext
// WHY BATCH ENCODING: More efficient than encrypting one value at a time
// EXAMPLE: Instead of 10 ciphertexts for 10 numbers, use 1 ciphertext for all 10
extern "C" SEALBatchEncoder* seal_create_batch_encoder(SEALContextWrapper* ctx) {
    try {
        if (!ctx) return nullptr;
        
        // Allocate encoder wrapper
        SEALBatchEncoder* encoder = new SEALBatchEncoder();

        // Create SEAL BatchEncoder
        encoder->encoder = unique_ptr<BatchEncoder>(
            new BatchEncoder(*ctx->seal_context) 
        );
        
        return encoder;
    } catch (...) {
        return nullptr;
    }
}
// Free batch encoder memory
extern "C" void seal_destroy_batch_encoder(SEALBatchEncoder* encoder) {
    if (encoder) delete encoder;
}

// PURPOSE: Encode vector of integers into plaintext
// WHAT IT DOES: [1, 2, 3, 4, ...] → Plaintext polynomial
extern "C" SEALPlaintext* seal_batch_encode(
    SEALBatchEncoder* encoder,
    const int64_t* values, // Array of integers from Rust
    size_t values_size // How many integers
) {
    try {
        if (!encoder || !values) return nullptr;
        
        // Convert C array to C++ vector
        // SEAL's encode() function expects std::vector
        vector<int64_t> vec(values, values + values_size);
        
        // Create plaintext wrapper
        SEALPlaintext* plain = new SEALPlaintext();

        // Encode vector into plaintext
        // Integers are packed into polynomial coefficients
        encoder->encoder->encode(vec, plain->plaintext);
        
        return plain;
    } catch (...) {
        return nullptr;
    }
}

// PURPOSE: Decode plaintext back into vector of integers
// Plaintext polynomial → [1, 2, 3, 4, ...]
extern "C" void seal_batch_decode(
    SEALBatchEncoder* encoder,
    SEALPlaintext* plain,
    int64_t* output, // Buffer allocated by Rust
    size_t* output_size // IN: buffer size, OUT: actual data size
) {
    try {
        if (!encoder || !plain || !output || !output_size) return;
        
        // Decode plaintext into vector
        vector<int64_t> vec;
        encoder->encoder->decode(plain->plaintext, vec);
        
        // Copy to Rust's buffer (only copy what fits)
        // min(): Prevent buffer overflow if vec is larger than output buffer
        size_t copy_size = min(vec.size(), *output_size);
        memcpy(output, vec.data(), copy_size * sizeof(int64_t));

        // Tell Rust how much data we actually wrote
        *output_size = copy_size;
        
    } catch (...) {
        // On error, set output_size to 0
        *output_size = 0;
    }
}

// PURPOSE: Get how many integers can fit in one ciphertext
// Rust needs to know buffer size for encoding/decoding
extern "C" size_t seal_get_slot_count(SEALBatchEncoder* encoder) {
    if (!encoder) return 0;
    return encoder->encoder->slot_count();
    // TYPICAL VALUES: poly_degree=8192 → slot_count=4096
}

// ============================================
// Galois Keys
// ============================================
extern "C" SEALGaloisKeys* seal_generate_galois_keys(SEALContextWrapper* ctx) {
    try {
        if (!ctx) return nullptr;
        
        SEALGaloisKeys* gal_keys = new SEALGaloisKeys();
        // Create galois keys and move into our struct
        ctx->keygen->create_galois_keys(gal_keys->keys);
        
        return gal_keys;
    } catch (...) {
        return nullptr;
    }
}

extern "C" void seal_destroy_galois_keys(SEALGaloisKeys* keys) {
    if (keys) delete keys;
}

extern "C" SEALCiphertext* seal_rotate_rows(
    SEALContextWrapper* ctx,
    SEALCiphertext* cipher,
    int steps,
    SEALGaloisKeys* galois_keys
) {
    try {
        if (!ctx || !cipher || !galois_keys) return nullptr;
        
        Evaluator evaluator(*ctx->seal_context);  // ⭐ Dereference seal_context
        SEALCiphertext* result = new SEALCiphertext();
        
        evaluator.rotate_rows(
            cipher->ciphertext,
            steps,
            galois_keys->keys,
            result->ciphertext
        );
        
        return result;
    } catch (...) {
        return nullptr;
    }
}

// ============================================
// Plaintext Operations
// ============================================
// Create plaintext object from a hexadecimal string
extern "C" SEALPlaintext* seal_create_plaintext(const char* hex_string) {
    try {
        SEALPlaintext* plain = new SEALPlaintext();
        // Convert hex string (e.g. "42") into SEAL plaintext
        plain->plaintext = Plaintext(hex_string);
        return plain;
    } catch (...) {
        return nullptr;
    }
}
// Delete plaintext object
extern "C" void seal_destroy_plaintext(SEALPlaintext* plain) {
    if (plain) delete plain;
}
// Convert plaintext to readable string (for debugging)
extern "C" const char* seal_plaintext_to_string(SEALPlaintext* plain) {
    if (!plain) return nullptr;
    // Note: This leaks memory - for demo only
    // In production, need better string management
    // Convert plaintext to string (e.g. "42")
    string str = plain->plaintext.to_string();

    // Allocate new C-style string
    char* result = new char[str.length() + 1];

    // Copy content into new string
    strcpy(result, str.c_str());

    // Return pointer to string
    return result;
}

// ============================================
// Encryption Implementation
// ============================================

// Encrypt a plaintext and return a ciphertext
extern "C" SEALCiphertext* seal_encrypt(
    SEALEncryptor* encryptor, // encryptor object
    SEALPlaintext* plaintext // input plaintext
) {
    try {
        if (!encryptor || !plaintext) return nullptr;
        
        // Create ciphertext object
        SEALCiphertext* cipher = new SEALCiphertext();

        // Use SEAL encryptor to encrypt the plaintext
        encryptor->encryptor->encrypt(
            plaintext->plaintext,
            cipher->ciphertext
        );
        // Return ciphertext to Rust
        return cipher;
    } catch (...) {
        return nullptr;
    }
}
// Delete ciphertext object
extern "C" void seal_destroy_ciphertext(SEALCiphertext* cipher) {
    if (cipher) delete cipher;
}

// ============================================
// Ciphertext Inspection
// ============================================
extern "C" size_t seal_ciphertext_size(SEALCiphertext* cipher) {
    if (!cipher) return 0;
    return cipher->ciphertext.size();
}

extern "C" uint64_t seal_ciphertext_coeff_count(SEALCiphertext* cipher) {
    if (!cipher) return 0;
    return cipher->ciphertext.poly_modulus_degree();
}

extern "C" size_t seal_ciphertext_byte_count(SEALCiphertext* cipher) {
    if (!cipher) return 0;
    return cipher->ciphertext.save_size();
}

extern "C" const char* seal_ciphertext_info(SEALCiphertext* cipher) {
    if (!cipher) return nullptr;
    try {
        stringstream ss;
        ss << "Ciphertext["
           << "size=" << cipher->ciphertext.size()
           << ", poly_degree=" << cipher->ciphertext.poly_modulus_degree()
           << ", coeff_mod_count=" << cipher->ciphertext.coeff_modulus_size()
           << ", bytes=" << cipher->ciphertext.save_size()
           << "]";
        string str = ss.str();
        char* result = new char[str.length() + 1];
        strcpy(result, str.c_str());
        return result;
    } catch (...) {
        return nullptr;
    }
}

// ============================================
// Decryption Implementation
// ============================================

// Decrypt ciphertext and return plaintext
extern "C" SEALPlaintext* seal_decrypt(
    SEALDecryptor* decryptor, // decryptor object
    SEALCiphertext* ciphertext // input ciphertext
) {
    try {
        if (!decryptor || !ciphertext) return nullptr;
        
        // Create new plaintext object
        SEALPlaintext* plain = new SEALPlaintext();

        // Use SEAL decryptor to decrypt the ciphertext
        decryptor->decryptor->decrypt(
            ciphertext->ciphertext,
            plain->plaintext
        );
        
        return plain;
    } catch (...) {
        return nullptr;
    }
}

// ============================================
// Homomorphic Operations
// ============================================

// Add two encrypted numbers without decrypting
extern "C" SEALCiphertext* seal_add(
    SEALContextWrapper* ctx, // context
    SEALCiphertext* a, // first ciphertext
    SEALCiphertext* b // second ciphertext
) {
    try {
        if (!ctx || !a || !b) return nullptr;
        
        // Evaluator is SEAL's tool for math on encrypted data
        Evaluator evaluator(*ctx->seal_context);
        // Create new ciphertext for the result
        SEALCiphertext* result = new SEALCiphertext();

        // Perform encrypted addition
        evaluator.add(
            a->ciphertext,
            b->ciphertext,
            result->ciphertext
        );
        
        return result;
    } catch (...) {
        return nullptr;
    }
}

// Multiply two encrypted numbers without decrypting
extern "C" SEALCiphertext* seal_multiply(
    SEALContextWrapper* ctx,
    SEALCiphertext* a,
    SEALCiphertext* b
) {
    try {
        if (!ctx || !a || !b) return nullptr;
        // Create evaluator for encrypted math
        Evaluator evaluator(*ctx->seal_context);
        // Create ciphertext for result
        SEALCiphertext* result = new SEALCiphertext();
        // Perform encrypted multiplication
        evaluator.multiply(
            a->ciphertext,
            b->ciphertext,
            result->ciphertext
        );
        
        return result;
    } catch (...) {
        return nullptr;
    }
}