# Homomorphic Encryption gRPC API Reference

This document provides comprehensive documentation for all RPC methods available in the HE gRPC Service.

## Table of Contents

- [Overview](#overview)
- [Connection](#connection)
- [Supported Libraries](#supported-libraries)
- [RPC Methods](#rpc-methods)
  - [GenerateKeys](#1-generatekeys)
  - [Encrypt](#2-encrypt)
  - [Decrypt](#3-decrypt)
  - [Add](#4-add)
  - [Multiply](#5-multiply)
  - [RunBenchmark](#6-runbenchmark)
  - [RunComparisonBenchmark](#7-runcomparisonbenchmark)
- [Error Handling](#error-handling)
- [Complete Workflow Example](#complete-workflow-example)

---

## Overview

The HE gRPC Service provides a unified API for performing homomorphic encryption operations using three different libraries:

| Library | Scheme | Best For |
|---------|--------|----------|
| **Microsoft SEAL** | BFV | Batch operations, vector encryption |
| **HELib** | BGV | Single-value operations, bootstrapping |
| **OpenFHE** | BFV | Advanced FHE features, research |

All operations are performed on the server side, allowing clients in any language to use homomorphic encryption without installing the complex HE libraries locally.

---

## Connection

**Default Endpoint:** `localhost:50051`

```
grpc://localhost:50051
```

For Docker deployments, the server binds to `0.0.0.0:50051` to accept connections from any interface.

---

## Supported Libraries

When specifying a library in requests, use one of these values:

| Value | Library | Encryption Scheme |
|-------|---------|-------------------|
| `"SEAL"` | Microsoft SEAL v4.1 | BFV (Brakerski/Fan-Vercauteren) |
| `"HELib"` | IBM HELib v2.3 | BGV (Brakerski-Gentry-Vaikuntanathan) |
| `"OpenFHE"` | OpenFHE v1.2 | BFV |

---

## RPC Methods

### 1. GenerateKeys

Creates an encryption context and generates public/private key pairs for a specific HE library.

#### Request: `GenerateKeysRequest`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `library` | string | Yes | HE library to use: `"SEAL"`, `"HELib"`, or `"OpenFHE"` |
| `poly_modulus_degree` | int32 | No | Security parameter (default: 8192 for SEAL/OpenFHE) |

#### Response: `GenerateKeysResponse`

| Field | Type | Description |
|-------|------|-------------|
| `session_id` | string | Unique 8-character session identifier (save this!) |
| `public_key` | bytes | Serialized public key (for reference) |
| `status` | string | `"Keys generated for {library} (session: {id})"` or error |

#### Example

**Request:**
```json
{
  "library": "SEAL",
  "poly_modulus_degree": 8192
}
```

**Response:**
```json
{
  "session_id": "a1b2c3d4",
  "public_key": "<1024 bytes>",
  "status": "Keys generated for SEAL (session: a1b2c3d4)"
}
```

#### Notes
- The `session_id` must be used in all subsequent operations
- Sessions are stored in server memory; they persist until server restart
- SEAL uses `poly_modulus_degree` (recommended: 4096, 8192, 16384)
- HELib uses fixed parameters (m=4095, p=2, r=1)
- OpenFHE uses `plaintext_modulus=65537`, `multiplicative_depth=2`

---

### 2. Encrypt

Encrypts a vector of integer values using the keys from a previous session.

#### Request: `EncryptRequest`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `session_id` | string | Yes | Session ID from GenerateKeys |
| `values` | repeated int64 | Yes | Array of integers to encrypt |

#### Response: `EncryptResponse`

| Field | Type | Description |
|-------|------|-------------|
| `ciphertext` | bytes | Serialized encrypted data |
| `status` | string | `"Encrypted {n} values using {library}"` or error |

#### Example

**Request:**
```json
{
  "session_id": "a1b2c3d4",
  "values": [10, 20, 30, 40, 50]
}
```

**Response:**
```json
{
  "ciphertext": "<encrypted bytes>",
  "status": "Encrypted 5 values using SEAL"
}
```

#### Notes
- **SEAL**: Supports batch encryption of vectors (up to `poly_modulus_degree / 2` values)
- **HELib**: Only encrypts the first value; use single-value arrays `[42]`
- **OpenFHE**: Supports vector encryption similar to SEAL

---

### 3. Decrypt

Decrypts a ciphertext back to plaintext integer values.

#### Request: `DecryptRequest`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `session_id` | string | Yes | Session ID from GenerateKeys |
| `ciphertext` | bytes | Yes | Ciphertext from Encrypt or homomorphic operations |

#### Response: `DecryptResponse`

| Field | Type | Description |
|-------|------|-------------|
| `values` | repeated int64 | Decrypted integer values |
| `status` | string | `"Decrypted successfully using {library}"` or error |

#### Example

**Request:**
```json
{
  "session_id": "a1b2c3d4",
  "ciphertext": "<encrypted bytes from previous operation>"
}
```

**Response:**
```json
{
  "values": [10, 20, 30, 40, 50],
  "status": "Decrypted successfully using SEAL"
}
```

---

### 4. Add

Performs homomorphic addition on two ciphertexts. The result, when decrypted, equals the sum of the original plaintexts.

#### Request: `BinaryOpRequest`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `session_id` | string | Yes | Session ID from GenerateKeys |
| `ciphertext1` | bytes | Yes | First encrypted operand |
| `ciphertext2` | bytes | Yes | Second encrypted operand |

#### Response: `BinaryOpResponse`

| Field | Type | Description |
|-------|------|-------------|
| `result_ciphertext` | bytes | Encrypted result (decrypt to get sum) |
| `status` | string | `"Addition complete using {library}"` or error |

#### Example

**Scenario:** Add encrypted [10, 20] + encrypted [5, 10]

**Request:**
```json
{
  "session_id": "a1b2c3d4",
  "ciphertext1": "<encrypted [10, 20]>",
  "ciphertext2": "<encrypted [5, 10]>"
}
```

**Response:**
```json
{
  "result_ciphertext": "<encrypted result>",
  "status": "Addition complete using SEAL"
}
```

**After decrypting `result_ciphertext`:** `[15, 30]`

#### Notes
- Both ciphertexts must be from the same session
- Element-wise addition for vector encryption (SEAL, OpenFHE)
- Single value addition for HELib

---

### 5. Multiply

Performs homomorphic multiplication on two ciphertexts. The result, when decrypted, equals the product of the original plaintexts.

#### Request: `BinaryOpRequest`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `session_id` | string | Yes | Session ID from GenerateKeys |
| `ciphertext1` | bytes | Yes | First encrypted operand |
| `ciphertext2` | bytes | Yes | Second encrypted operand |

#### Response: `BinaryOpResponse`

| Field | Type | Description |
|-------|------|-------------|
| `result_ciphertext` | bytes | Encrypted result (decrypt to get product) |
| `status` | string | `"Multiplication complete using {library}"` or error |

#### Example

**Scenario:** Multiply encrypted [10, 20] × encrypted [2, 3]

**Request:**
```json
{
  "session_id": "a1b2c3d4",
  "ciphertext1": "<encrypted [10, 20]>",
  "ciphertext2": "<encrypted [2, 3]>"
}
```

**Response:**
```json
{
  "result_ciphertext": "<encrypted result>",
  "status": "Multiplication complete using SEAL"
}
```

**After decrypting `result_ciphertext`:** `[20, 60]`

#### Notes
- Multiplication increases ciphertext "noise" more than addition
- After many multiplications, decryption may fail (noise budget exhausted)
- SEAL includes relinearization to manage noise growth

---

### 6. RunBenchmark

Runs a performance benchmark for a single HE library, measuring timing for all operations.

#### Request: `BenchmarkRequest`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `library` | string | Yes | `"SEAL"`, `"HELib"`, or `"OpenFHE"` |
| `num_operations` | int32 | Yes | Number of operations to run (recommended: 20-100) |

#### Response: `BenchmarkResponse`

| Field | Type | Description |
|-------|------|-------------|
| `key_gen_time_ms` | double | Time to generate keys (milliseconds) |
| `encoding_time_ms` | double | Time per encoding operation (ms/op) |
| `encryption_time_ms` | double | Time per encryption operation (ms/op) |
| `addition_time_ms` | double | Time per addition operation (ms/op) |
| `multiplication_time_ms` | double | Time per multiplication operation (ms/op) |
| `decryption_time_ms` | double | Time per decryption operation (ms/op) |
| `total_time_ms` | double | Total benchmark execution time |
| `status` | string | `"Benchmark completed for {library}"` or error |

#### Example

**Request:**
```json
{
  "library": "SEAL",
  "num_operations": 50
}
```

**Response:**
```json
{
  "key_gen_time_ms": 5.55,
  "encoding_time_ms": 0.04,
  "encryption_time_ms": 1.12,
  "addition_time_ms": 0.02,
  "multiplication_time_ms": 2.64,
  "decryption_time_ms": 0.28,
  "total_time_ms": 82.55,
  "status": "Benchmark completed for SEAL"
}
```

---

### 7. RunComparisonBenchmark

Runs benchmarks for all three HE libraries and compares their performance.

#### Request: `BenchmarkRequest`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `library` | string | No | Ignored (runs all libraries) |
| `num_operations` | int32 | Yes | Number of operations per library |

#### Response: `ComparisonBenchmarkResponse`

| Field | Type | Description |
|-------|------|-------------|
| `seal` | BenchmarkResponse | SEAL benchmark results |
| `helib` | BenchmarkResponse | HELib benchmark results |
| `openfhe` | BenchmarkResponse | OpenFHE benchmark results |
| `fastest_library` | string | Library with lowest total time |
| `recommendation` | string | Usage recommendation based on results |

#### Example

**Request:**
```json
{
  "num_operations": 20
}
```

**Response:**
```json
{
  "seal": {
    "key_gen_time_ms": 5.09,
    "encryption_time_ms": 1.03,
    "addition_time_ms": 0.02,
    "multiplication_time_ms": 2.67,
    "decryption_time_ms": 0.24,
    "total_time_ms": 82.55
  },
  "helib": {
    "key_gen_time_ms": 96.58,
    "encryption_time_ms": 1.46,
    "addition_time_ms": 0.07,
    "multiplication_time_ms": 10.52,
    "decryption_time_ms": 2.17,
    "total_time_ms": 370.42
  },
  "openfhe": {
    "key_gen_time_ms": 25.01,
    "encryption_time_ms": 3.94,
    "addition_time_ms": 0.01,
    "multiplication_time_ms": 13.49,
    "decryption_time_ms": 1.89,
    "total_time_ms": 403.75
  },
  "fastest_library": "SEAL",
  "recommendation": "SEAL recommended for encryption-heavy workloads (batching support)"
}
```

#### Performance Summary (Typical Results)

| Library | Key Gen | Encryption | Addition | Multiplication | Total |
|---------|---------|------------|----------|----------------|-------|
| **SEAL** | ~5ms | ~1ms/op | ~0.02ms/op | ~2.6ms/op | **Fastest** |
| HELib | ~97ms | ~1.5ms/op | ~0.07ms/op | ~10ms/op | 4-5x slower |
| OpenFHE | ~25ms | ~4ms/op | ~0.01ms/op | ~13ms/op | 4-5x slower |

---

## Error Handling

### Common Error Responses

| Error | Cause | Solution |
|-------|-------|----------|
| `"Session not found: {id}"` | Invalid or expired session ID | Call GenerateKeys first |
| `"Unsupported library: {name}"` | Invalid library name | Use "SEAL", "HELib", or "OpenFHE" |
| `"Failed to decrypt"` | Corrupted ciphertext or wrong session | Ensure ciphertext matches session |
| `"Noise budget exhausted"` | Too many operations on ciphertext | Use fresh encryption or larger parameters |

### gRPC Status Codes

| Code | Meaning |
|------|---------|
| `OK` (0) | Success |
| `INVALID_ARGUMENT` (3) | Bad request parameters |
| `NOT_FOUND` (5) | Session not found |
| `INTERNAL` (13) | Server-side HE operation failed |
| `UNIMPLEMENTED` (12) | RPC method not available (server version mismatch) |

---

## Complete Workflow Example

Here's a complete example showing how to use the API for a medical data privacy scenario:

### Scenario: Two hospitals sharing encrypted patient counts

```
Hospital A                    HE gRPC Server                    Hospital B
    |                              |                                 |
    |-- GenerateKeys(SEAL) ------->|                                 |
    |<-- session_id: "abc123" -----|                                 |
    |                              |                                 |
    |-- Encrypt([100, 50, 75]) --->|  (patient counts by department) |
    |<-- ciphertext_A -------------|                                 |
    |                              |                                 |
    |                              |<-- Encrypt([80, 60, 90]) -------|
    |                              |--- ciphertext_B --------------->|
    |                              |                                 |
    |-- Add(ciphertext_A, B) ----->|  (combine encrypted totals)     |
    |<-- result_ciphertext --------|                                 |
    |                              |                                 |
    |-- Decrypt(result) ---------->|                                 |
    |<-- [180, 110, 165] ----------|  (combined totals, never exposed)|
```

### Code Example (Pseudocode)

```python
# 1. Generate encryption keys
response = client.GenerateKeys(library="SEAL")
session_id = response.session_id

# 2. Hospital A encrypts their data
hospital_a_data = [100, 50, 75]  # Patient counts
encrypted_a = client.Encrypt(session_id=session_id, values=hospital_a_data)

# 3. Hospital B encrypts their data  
hospital_b_data = [80, 60, 90]
encrypted_b = client.Encrypt(session_id=session_id, values=hospital_b_data)

# 4. Add encrypted values (data never exposed!)
result = client.Add(
    session_id=session_id,
    ciphertext1=encrypted_a.ciphertext,
    ciphertext2=encrypted_b.ciphertext
)

# 5. Decrypt to get combined totals
decrypted = client.Decrypt(session_id=session_id, ciphertext=result.result_ciphertext)
print(decrypted.values)  # [180, 110, 165]
```

---

## Protocol Buffer Definition

The complete `.proto` file is located at [`proto/he_service.proto`](../proto/he_service.proto).

To generate client code for your language:

```bash
# Python
python -m grpc_tools.protoc -I proto --python_out=. --grpc_python_out=. proto/he_service.proto

# Java
protoc -I proto --java_out=. --grpc-java_out=. proto/he_service.proto

# Go
protoc -I proto --go_out=. --go-grpc_out=. proto/he_service.proto
```

---

## See Also

- [Python Client Example](./python-client-example.md)
- [Java Client Example](./java-client-example.md)
- [Architecture Overview](./architecture.md)
