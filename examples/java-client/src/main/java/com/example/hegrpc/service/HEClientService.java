package com.example.hegrpc.service;

import com.google.protobuf.ByteString;
import he_service.HeService.*;
import he_service.HEServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service class wrapping the gRPC stub with convenient methods.
 */
@Service
public class HEClientService {

    private static final Logger log = LoggerFactory.getLogger(HEClientService.class);
    
    // The blocking stub makes synchronous calls to the server
    private final HEServiceGrpc.HEServiceBlockingStub blockingStub;

    /**
     * Constructor injection - Spring injects the ManagedChannel bean.
     */
    public HEClientService(ManagedChannel channel) {
        this.blockingStub = HEServiceGrpc.newBlockingStub(channel);
    }

    // ==================== KEY GENERATION ====================

    /**
     * Generate encryption keys for the specified HE library.
     *
     * @param library "SEAL", "HELib", or "OpenFHE"
     * @param polyModulusDegree Security parameter (e.g., 8192)
     * @return Response with session_id and public_key
     */
    public GenerateKeysResponse generateKeys(String library, int polyModulusDegree) {
        log.info("Generating keys for {} with poly_modulus_degree={}", 
                 library, polyModulusDegree);
        
        GenerateKeysRequest request = GenerateKeysRequest.newBuilder()
                .setLibrary(library)
                .setPolyModulusDegree(polyModulusDegree)
                .build();

        try {
            GenerateKeysResponse response = blockingStub.generateKeys(request);
            log.info("Keys generated. Session ID: {}", response.getSessionId());
            return response;
        } catch (StatusRuntimeException e) {
            log.error("Failed to generate keys: {}", e.getStatus());
            throw e;
        }
    }

    /**
     * Generate keys with default parameters.
     */
    public GenerateKeysResponse generateKeys(String library) {
        return generateKeys(library, 8192);
    }

    // ==================== ENCRYPTION ====================

    /**
     * Encrypt a list of integer values.
     */
    public EncryptResponse encrypt(String sessionId, List<Long> values) {
        log.info("Encrypting {} values for session {}", values.size(), sessionId);
        
        EncryptRequest request = EncryptRequest.newBuilder()
                .setSessionId(sessionId)
                .addAllValues(values)
                .build();

        try {
            EncryptResponse response = blockingStub.encrypt(request);
            log.info("Encrypted. Ciphertext size: {} bytes", 
                    response.getCiphertext().size());
            return response;
        } catch (StatusRuntimeException e) {
            log.error("Failed to encrypt: {}", e.getStatus());
            throw e;
        }
    }

    /**
     * Encrypt a single value (convenience method).
     */
    public EncryptResponse encrypt(String sessionId, long value) {
        return encrypt(sessionId, List.of(value));
    }

    // ==================== DECRYPTION ====================

    /**
     * Decrypt a ciphertext back to plaintext values.
     */
    public DecryptResponse decrypt(String sessionId, ByteString ciphertext) {
        log.info("Decrypting for session {}", sessionId);
        
        DecryptRequest request = DecryptRequest.newBuilder()
                .setSessionId(sessionId)
                .setCiphertext(ciphertext)
                .build();

        try {
            DecryptResponse response = blockingStub.decrypt(request);
            log.info("Decrypted values: {}", response.getValuesList());
            return response;
        } catch (StatusRuntimeException e) {
            log.error("Failed to decrypt: {}", e.getStatus());
            throw e;
        }
    }

    // ==================== HOMOMORPHIC OPERATIONS ====================

    /**
     * Homomorphic addition: result = ciphertext1 + ciphertext2
     */
    public BinaryOpResponse add(String sessionId, 
                                ByteString ciphertext1, 
                                ByteString ciphertext2) {
        log.info("Performing homomorphic addition for session {}", sessionId);
        
        BinaryOpRequest request = BinaryOpRequest.newBuilder()
                .setSessionId(sessionId)
                .setCiphertext1(ciphertext1)
                .setCiphertext2(ciphertext2)
                .build();

        try {
            return blockingStub.add(request);
        } catch (StatusRuntimeException e) {
            log.error("Failed to add: {}", e.getStatus());
            throw e;
        }
    }

    /**
     * Homomorphic multiplication: result = ciphertext1 * ciphertext2
     */
    public BinaryOpResponse multiply(String sessionId, 
                                     ByteString ciphertext1, 
                                     ByteString ciphertext2) {
        log.info("Performing homomorphic multiplication for session {}", sessionId);
        
        BinaryOpRequest request = BinaryOpRequest.newBuilder()
                .setSessionId(sessionId)
                .setCiphertext1(ciphertext1)
                .setCiphertext2(ciphertext2)
                .build();

        try {
            return blockingStub.multiply(request);
        } catch (StatusRuntimeException e) {
            log.error("Failed to multiply: {}", e.getStatus());
            throw e;
        }
    }

    // ==================== BENCHMARKING ====================

    /**
     * Run benchmark for a single library.
     */
    public BenchmarkResponse runBenchmark(String library, int numOperations) {
        log.info("Running benchmark for {} with {} operations", 
                 library, numOperations);
        
        BenchmarkRequest request = BenchmarkRequest.newBuilder()
                .setLibrary(library)
                .setNumOperations(numOperations)
                .build();

        try {
            return blockingStub.runBenchmark(request);
        } catch (StatusRuntimeException e) {
            log.error("Failed to run benchmark: {}", e.getStatus());
            throw e;
        }
    }

    /**
     * Run comparison benchmark for all three libraries.
     */
    public ComparisonBenchmarkResponse runComparisonBenchmark(int numOperations) {
        log.info("Running comparison benchmark with {} operations per library", 
                 numOperations);
        
        BenchmarkRequest request = BenchmarkRequest.newBuilder()
                .setNumOperations(numOperations)
                .build();

        try {
            return blockingStub.runComparisonBenchmark(request);
        } catch (StatusRuntimeException e) {
            log.error("Failed to run comparison benchmark: {}", e.getStatus());
            throw e;
        }
    }
}
