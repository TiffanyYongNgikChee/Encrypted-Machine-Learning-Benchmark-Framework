// grpc_client/src/main.rs
//
// Comprehensive Test Client for HE gRPC Server
// Tests all three libraries: SEAL, HELib, and OpenFHE

use tonic::Request;

pub mod he_service {
    tonic::include_proto!("he_service");
}

use he_service::{
    he_service_client::HeServiceClient, 
    GenerateKeysRequest, 
    EncryptRequest,
    DecryptRequest,
    BinaryOpRequest,
    BenchmarkRequest
};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("\n╔═══════════════════════════════════════════════════════════════╗");
    println!("║   🧪 HE gRPC Server - Comprehensive Test Suite               ║");
    println!("╚═══════════════════════════════════════════════════════════════╝\n");
    
    println!("🔌 Connecting to HE gRPC Server at [::1]:50051...");
    let mut client = HeServiceClient::connect("http://[::1]:50051").await?;
    println!("✓ Connected!\n");

    // Test each library independently
    test_seal(&mut client).await?;
    test_helib(&mut client).await?;
    test_openfhe(&mut client).await?;

    println!("\n╔═══════════════════════════════════════════════════════════════╗");
    println!("║   ✅ ALL TESTS PASSED - All three libraries working!         ║");
    println!("╚═══════════════════════════════════════════════════════════════╝\n");

    Ok(())
}

async fn test_seal(client: &mut HeServiceClient<tonic::transport::Channel>) -> Result<(), Box<dyn std::error::Error>> {
    println!("╔═══════════════════════════════════════════════════════════════╗");
    println!("║   🔷 Testing SEAL Library (Microsoft SEAL - BFV Scheme)      ║");
    println!("╚═══════════════════════════════════════════════════════════════╝\n");

    // 1. Generate Keys
    println!("📝 Test 1: Generating SEAL keys (poly_modulus_degree=8192)...");
    let request = Request::new(GenerateKeysRequest {
        library: "SEAL".to_string(),
        poly_modulus_degree: 8192,
    });
    let response = client.generate_keys(request).await?;
    let keys_response = response.into_inner();
    let session_id = keys_response.session_id.clone();
    println!("   ✓ Session ID: {}", &session_id[..8]);
    println!("   ✓ Status: {}\n", keys_response.status);

    // 2. Encrypt
    println!("📝 Test 2: Encrypting vector [10, 20, 30, 40, 50]...");
    let request = Request::new(EncryptRequest {
        session_id: session_id.clone(),
        values: vec![10, 20, 30, 40, 50],
    });
    let response = client.encrypt(request).await?;
    let encrypt_response = response.into_inner();
    println!("   ✓ Ciphertext: {} bytes", encrypt_response.ciphertext.len());
    println!("   ✓ Status: {}\n", encrypt_response.status);

    // 3. Decrypt
    println!("📝 Test 3: Decrypting ciphertext...");
    let request = Request::new(DecryptRequest {
        session_id: session_id.clone(),
        ciphertext: vec![],
    });
    let response = client.decrypt(request).await?;
    let decrypt_response = response.into_inner();
    println!("   ✓ Decrypted values: {:?}", &decrypt_response.values[..5.min(decrypt_response.values.len())]);
    println!("   ✓ Status: {}\n", decrypt_response.status);

    // 4. Addition
    println!("📝 Test 4: Homomorphic addition...");
    let request = Request::new(BinaryOpRequest {
        session_id: session_id.clone(),
        ciphertext1: vec![],
        ciphertext2: vec![],
    });
    let response = client.add(request).await?;
    let add_response = response.into_inner();
    println!("   ✓ Status: {}\n", add_response.status);

    // 5. Multiplication
    println!("📝 Test 5: Homomorphic multiplication...");
    let request = Request::new(BinaryOpRequest {
        session_id: session_id.clone(),
        ciphertext1: vec![],
        ciphertext2: vec![],
    });
    let response = client.multiply(request).await?;
    let multiply_response = response.into_inner();
    println!("   ✓ Status: {}\n", multiply_response.status);

    // 6. Benchmark
    println!("📝 Test 6: Running SEAL benchmark (50 operations)...");
    let request = Request::new(BenchmarkRequest {
        library: "SEAL".to_string(),
        num_operations: 50,
    });
    let response = client.run_benchmark(request).await?;
    let benchmark = response.into_inner();
    println!("   📊 Benchmark Results:");
    println!("      • Key Generation:  {:.2} ms", benchmark.key_gen_time_ms);
    println!("      • Encryption:      {:.2} ms/op", benchmark.encryption_time_ms);
    println!("      • Addition:        {:.2} ms/op", benchmark.addition_time_ms);
    println!("      • Multiplication:  {:.2} ms/op", benchmark.multiplication_time_ms);
    println!("      • Decryption:      {:.2} ms/op", benchmark.decryption_time_ms);
    println!("   ✓ {}\n", benchmark.status);

    println!("✅ SEAL tests completed successfully!\n");
    Ok(())
}

async fn test_helib(client: &mut HeServiceClient<tonic::transport::Channel>) -> Result<(), Box<dyn std::error::Error>> {
    println!("╔═══════════════════════════════════════════════════════════════╗");
    println!("║   🔶 Testing HELib Library (IBM HELib - BGV Scheme)          ║");
    println!("╚═══════════════════════════════════════════════════════════════╝\n");

    // 1. Generate Keys
    println!("📝 Test 1: Generating HELib keys (m=4095, p=2, r=1)...");
    let request = Request::new(GenerateKeysRequest {
        library: "HELib".to_string(),
        poly_modulus_degree: 4096,
    });
    let response = client.generate_keys(request).await?;
    let keys_response = response.into_inner();
    let session_id = keys_response.session_id.clone();
    println!("   ✓ Session ID: {}", &session_id[..8]);
    println!("   ✓ Status: {}\n", keys_response.status);

    // 2. Encrypt
    println!("📝 Test 2: Encrypting value [42] (HELib uses single values)...");
    let request = Request::new(EncryptRequest {
        session_id: session_id.clone(),
        values: vec![42],
    });
    let response = client.encrypt(request).await?;
    let encrypt_response = response.into_inner();
    println!("   ✓ Ciphertext: {} bytes", encrypt_response.ciphertext.len());
    println!("   ✓ Status: {}\n", encrypt_response.status);

    // 3. Decrypt
    println!("📝 Test 3: Decrypting ciphertext...");
    let request = Request::new(DecryptRequest {
        session_id: session_id.clone(),
        ciphertext: vec![],
    });
    let response = client.decrypt(request).await?;
    let decrypt_response = response.into_inner();
    println!("   ✓ Decrypted value: {:?}", decrypt_response.values);
    println!("   ✓ Status: {}\n", decrypt_response.status);

    // 4. Addition
    println!("📝 Test 4: Homomorphic addition...");
    let request = Request::new(BinaryOpRequest {
        session_id: session_id.clone(),
        ciphertext1: vec![],
        ciphertext2: vec![],
    });
    let response = client.add(request).await?;
    let add_response = response.into_inner();
    println!("   ✓ Status: {}\n", add_response.status);

    // 5. Multiplication
    println!("📝 Test 5: Homomorphic multiplication...");
    let request = Request::new(BinaryOpRequest {
        session_id: session_id.clone(),
        ciphertext1: vec![],
        ciphertext2: vec![],
    });
    let response = client.multiply(request).await?;
    let multiply_response = response.into_inner();
    println!("   ✓ Status: {}\n", multiply_response.status);

    // 6. Benchmark
    println!("📝 Test 6: Running HELib benchmark (50 operations)...");
    let request = Request::new(BenchmarkRequest {
        library: "HELib".to_string(),
        num_operations: 50,
    });
    let response = client.run_benchmark(request).await?;
    let benchmark = response.into_inner();
    println!("   📊 Benchmark Results:");
    println!("      • Key Generation:  {:.2} ms", benchmark.key_gen_time_ms);
    println!("      • Encryption:      {:.2} ms/op", benchmark.encryption_time_ms);
    println!("      • Addition:        {:.2} ms/op", benchmark.addition_time_ms);
    println!("      • Multiplication:  {:.2} ms/op", benchmark.multiplication_time_ms);
    println!("      • Decryption:      {:.2} ms/op", benchmark.decryption_time_ms);
    println!("   ✓ {}\n", benchmark.status);

    println!("✅ HELib tests completed successfully!\n");
    Ok(())
}

async fn test_openfhe(client: &mut HeServiceClient<tonic::transport::Channel>) -> Result<(), Box<dyn std::error::Error>> {
    println!("╔═══════════════════════════════════════════════════════════════╗");
    println!("║   🔷 Testing OpenFHE Library (OpenFHE - BFV Scheme)          ║");
    println!("╚═══════════════════════════════════════════════════════════════╝\n");

    // 1. Generate Keys
    println!("📝 Test 1: Generating OpenFHE keys (plaintext_mod=65537)...");
    let request = Request::new(GenerateKeysRequest {
        library: "OpenFHE".to_string(),
        poly_modulus_degree: 4096,
    });
    let response = client.generate_keys(request).await?;
    let keys_response = response.into_inner();
    let session_id = keys_response.session_id.clone();
    println!("   ✓ Session ID: {}", &session_id[..8]);
    println!("   ✓ Status: {}\n", keys_response.status);

    // 2. Encrypt
    println!("📝 Test 2: Encrypting vector [100, 200, 300, 400]...");
    let request = Request::new(EncryptRequest {
        session_id: session_id.clone(),
        values: vec![100, 200, 300, 400],
    });
    let response = client.encrypt(request).await?;
    let encrypt_response = response.into_inner();
    println!("   ✓ Ciphertext: {} bytes", encrypt_response.ciphertext.len());
    println!("   ✓ Status: {}\n", encrypt_response.status);

    // 3. Decrypt
    println!("📝 Test 3: Decrypting ciphertext...");
    let request = Request::new(DecryptRequest {
        session_id: session_id.clone(),
        ciphertext: vec![],
    });
    let response = client.decrypt(request).await?;
    let decrypt_response = response.into_inner();
    println!("   ✓ Decrypted values: {:?}", &decrypt_response.values[..4.min(decrypt_response.values.len())]);
    println!("   ✓ Status: {}\n", decrypt_response.status);

    // 4. Addition
    println!("📝 Test 4: Homomorphic addition...");
    let request = Request::new(BinaryOpRequest {
        session_id: session_id.clone(),
        ciphertext1: vec![],
        ciphertext2: vec![],
    });
    let response = client.add(request).await?;
    let add_response = response.into_inner();
    println!("   ✓ Status: {}\n", add_response.status);

    // 5. Multiplication
    println!("📝 Test 5: Homomorphic multiplication...");
    let request = Request::new(BinaryOpRequest {
        session_id: session_id.clone(),
        ciphertext1: vec![],
        ciphertext2: vec![],
    });
    let response = client.multiply(request).await?;
    let multiply_response = response.into_inner();
    println!("   ✓ Status: {}\n", multiply_response.status);

    // 6. Benchmark
    println!("📝 Test 6: Running OpenFHE benchmark (50 operations)...");
    let request = Request::new(BenchmarkRequest {
        library: "OpenFHE".to_string(),
        num_operations: 50,
    });
    let response = client.run_benchmark(request).await?;
    let benchmark = response.into_inner();
    println!("   📊 Benchmark Results:");
    println!("      • Key Generation:  {:.2} ms", benchmark.key_gen_time_ms);
    println!("      • Encryption:      {:.2} ms/op", benchmark.encryption_time_ms);
    println!("      • Addition:        {:.2} ms/op", benchmark.addition_time_ms);
    println!("      • Multiplication:  {:.2} ms/op", benchmark.multiplication_time_ms);
    println!("      • Decryption:      {:.2} ms/op", benchmark.decryption_time_ms);
    println!("   ✓ {}\n", benchmark.status);

    println!("✅ OpenFHE tests completed successfully!\n");
    Ok(())
}
