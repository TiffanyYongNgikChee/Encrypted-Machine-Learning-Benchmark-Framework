#include "../include/helib_wrapper.h"
#include <helib/helib.h>
#include <NTL/ZZX.h>
#include <memory>
#include <iostream>

using namespace helib;
using namespace std;
using namespace NTL;


// Opaque Struct Implementations
struct HElibContext {
    unique_ptr<Context> context;
};

struct HElibSecretKey {
    unique_ptr<SecKey> secretKey;
    HElibContext* ctx; // Keep reference to context
};

struct HElibPublicKey {
    const PubKey* publicKey; // Non-owning pointer
};

struct HElibCiphertext {
    unique_ptr<Ctxt> ctxt;
};

struct HElibPlaintext {
    long value;
};

// Context Management Implementation
extern "C" HElibContext* helib_create_context(
    unsigned long m,
    unsigned long p,
    unsigned long r
) {
    try {
        HElibContext* result = new HElibContext();
        
        // Use reset() to take ownership of raw pointer
        result->context.reset(
            ContextBuilder<BGV>()
                .m(m)
                .p(p)
                .r(r)
                .bits(300)
                .c(2)
                .buildModChain(true)
                .buildPtr()
        );
        
        return result;
        
    } catch (const exception& e) {
        cerr << "Context creation failed: " << e.what() << endl;
        return nullptr;
    }
}

extern "C" void helib_destroy_context(HElibContext* ctx) {
    if (ctx) delete ctx;
}

// Key Management Implementation
extern "C" HElibSecretKey* helib_generate_secret_key(HElibContext* ctx) {
    try {
        if (!ctx || !ctx->context) return nullptr;
        
        HElibSecretKey* sk = new HElibSecretKey();
        sk->ctx = ctx;
        
        // Create secret key
        sk->secretKey = make_unique<SecKey>(*ctx->context);
        sk->secretKey->GenSecKey(); // Generate secret key polynomial
        
        // Add key-switching matrices (required for multiplication)
        addSome1DMatrices(*sk->secretKey);
        
        return sk;
        
    } catch (const exception& e) {
        cerr << "Secret key generation failed: " << e.what() << endl;
        return nullptr;
    }
}

extern "C" void helib_destroy_secret_key(HElibSecretKey* sk) {
    if (sk) delete sk;
}

extern "C" HElibPublicKey* helib_get_public_key(HElibSecretKey* sk) {
    try {
        if (!sk || !sk->secretKey) return nullptr;
        
        HElibPublicKey* pk = new HElibPublicKey();
        pk->publicKey = sk->secretKey.get(); // SecKey inherits from PubKey
        
        return pk;
        
    } catch (...) {
        return nullptr;
    }
}

extern "C" void helib_destroy_public_key(HElibPublicKey* pk) {
    if (pk) delete pk;
}

// Plaintext Operations Implementation
extern "C" HElibPlaintext* helib_create_plaintext(
    HElibContext* ctx,
    long value
) {
    try {
        HElibPlaintext* plain = new HElibPlaintext();
        plain->value = value;
        return plain;
        
    } catch (...) {
        return nullptr;
    }
}

extern "C" long helib_plaintext_to_long(HElibPlaintext* plain) {
    if (!plain) return 0;
    return plain->value;
}

extern "C" void helib_destroy_plaintext(HElibPlaintext* plain) {
    if (plain) delete plain;
}

// Encryption/Decryption Implementation
extern "C" HElibCiphertext* helib_encrypt(
    HElibPublicKey* pk,
    HElibPlaintext* plain
) {
    try {
        if (!pk || !pk->publicKey || !plain) return nullptr;
        
        HElibCiphertext* cipher = new HElibCiphertext();
        cipher->ctxt = make_unique<Ctxt>(*pk->publicKey);
        
        // Encrypt the value
        pk->publicKey->Encrypt(*cipher->ctxt, to_ZZX(plain->value));
        
        return cipher;
        
    } catch (const exception& e) {
        cerr << "Encryption failed: " << e.what() << endl;
        return nullptr;
    }
}

extern "C" HElibPlaintext* helib_decrypt(
    HElibSecretKey* sk,
    HElibCiphertext* cipher
) {
    try {
        if (!sk || !sk->secretKey || !cipher || !cipher->ctxt) {
            return nullptr;
        }
        
        // Decrypt to polynomial
        ZZX poly;
        sk->secretKey->Decrypt(poly, *cipher->ctxt);
        
        // Convert to integer
        long value = to_long(coeff(poly, 0));
        
        // Create plaintext result
        HElibPlaintext* plain = new HElibPlaintext();
        plain->value = value;
        
        return plain;
        
    } catch (const exception& e) {
        cerr << "Decryption failed: " << e.what() << endl;
        return nullptr;
    }
}

extern "C" void helib_destroy_ciphertext(HElibCiphertext* cipher) {
    if (cipher) delete cipher;
}

// Homomorphic Operations Implementation
extern "C" HElibCiphertext* helib_add(
    HElibCiphertext* a,
    HElibCiphertext* b
) {
    try {
        if (!a || !a->ctxt || !b || !b->ctxt) return nullptr;
        
        HElibCiphertext* result = new HElibCiphertext();
        
        // Copy first ciphertext
        result->ctxt = make_unique<Ctxt>(*a->ctxt);
        
        // Add second ciphertext
        *result->ctxt += *b->ctxt;
        
        return result;
        
    } catch (const exception& e) {
        cerr << "Addition failed: " << e.what() << endl;
        return nullptr;
    }
}

extern "C" HElibCiphertext* helib_multiply(
    HElibCiphertext* a,
    HElibCiphertext* b
) {
    try {
        if (!a || !a->ctxt || !b || !b->ctxt) return nullptr;
        
        HElibCiphertext* result = new HElibCiphertext();
        
        // Copy first ciphertext
        result->ctxt = make_unique<Ctxt>(*a->ctxt);
        
        // Multiply by second ciphertext
        *result->ctxt *= *b->ctxt;
        
        return result;
        
    } catch (const exception& e) {
        cerr << "Multiplication failed: " << e.what() << endl;
        return nullptr;
    }
}

extern "C" HElibCiphertext* helib_subtract(
    HElibCiphertext* a,
    HElibCiphertext* b
) {
    try {
        if (!a || !a->ctxt || !b || !b->ctxt) return nullptr;
        
        HElibCiphertext* result = new HElibCiphertext();
        
        // Copy first ciphertext
        result->ctxt = make_unique<Ctxt>(*a->ctxt);
        
        // Subtract second ciphertext
        *result->ctxt -= *b->ctxt;
        
        return result;
        
    } catch (const exception& e) {
        cerr << "Subtraction failed: " << e.what() << endl;
        return nullptr;
    }
}

// Utility Functions Implementation
extern "C" int helib_noise_budget(
    HElibSecretKey* sk,
    HElibCiphertext* cipher
) {
    try {
        if (!sk || !sk->secretKey || !cipher || !cipher->ctxt) {
            return -1;
        }
        
        // Get capacity (similar to SEAL's noise budget)
        return cipher->ctxt->capacity();
        
    } catch (...) {
        return -1;
    }
}