//! Medical Data Encryption Comparison: SEAL vs HElib
//! 
//! This example encrypts the same medical record using both frameworks
//! and provides a detailed performance comparison.

use he_benchmark::{
    Context as SealContext, 
    Encryptor as SealEncryptor, 
    Decryptor as SealDecryptor,
    BatchEncoder as SealBatchEncoder,
    HEContext,
    HESecretKey,
    HEPublicKey,
    HEPlaintext,
};

use std::time::{Instant, Duration};
use std::thread::sleep;
use std::io::{self, Write};

// Performance Tracking Structures
// These structs store timing information for each phase
// of the SEAL and HElib encryption processes.
#[derive(Debug, Clone)]
// PhaseMetrics holds the duration (time taken) of each major step
// in the encryption pipeline for ONE framework (either SEAL or HElib).
struct PhaseMetrics {
    setup_time: Duration, // Time spent creating the encryption context and generating keys.
    encoding_time: Duration, // Time spent encoding the raw medical data into plaintext format.
    encryption_time: Duration, // Time taken to encrypt the encoded plaintext into ciphertext.
    operation_time: Duration, // Time taken to perform homomorphic operations (addition, etc.)
    decryption_time: Duration, // Time spent decrypting the resulting ciphertext.
    total_time: Duration, // Total accumulated time for the entire encryption workflow.
}

impl PhaseMetrics {
    // Creates a new PhaseMetrics object with all times initialized to zero.
    fn new() -> Self {
        Self {
            setup_time: Duration::ZERO,
            encoding_time: Duration::ZERO,
            encryption_time: Duration::ZERO,
            operation_time: Duration::ZERO,
            decryption_time: Duration::ZERO,
            total_time: Duration::ZERO,
        }
    }
}

#[derive(Debug)]
// ComparisonResult contains the performance metrics for BOTH
// encryption frameworks, SEAL and HElib, as well as a description
// of the test data used (e.g., "200-character medical record").
struct ComparisonResult {
    seal: PhaseMetrics, // Timing results for the SEAL encryption run.
    helib: PhaseMetrics, // Timing results for the HElib encryption run.
    data_description: String, // Human-readable description of the dataset (size, type, etc.).
}
