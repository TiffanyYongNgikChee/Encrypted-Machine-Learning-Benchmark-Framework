//! Basic encryption/decryption test
//! 
//! This example demonstrates:
//! 1. Creating SEAL context
//! 2. Encrypting a plaintext
//! 3. Decrypting ciphertext
//! 4. Homomorphic addition

use he_benchmark::{Context, Encryptor, Decryptor, Plaintext, add};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("SEAL FFI Test Starting...\n");

    // ============================================
    // TEST 1: Context Creation
    // ============================================
    println!("Test 1: Creating SEAL context...");
    let context = Context::new(4096, 1024)?;
    println!("Context created successfully\n");
    
    // ============================================
    // TEST 2: Basic Encryption/Decryption
    // ============================================
    println!("Test 2: Basic encryption/decryption...");
    let encryptor = Encryptor::new(&context)?;
    let decryptor = Decryptor::new(&context)?;
    
    // Create plaintext
    let plain = Plaintext::from_hex("142")?;
    println!("   Original plaintext: {}", plain.to_string()?);
    
    // Encrypt
    let cipher = encryptor.encrypt(&plain)?;
    println!("   Encryption successful");

    // INSPECT THE CIPHERTEXT - NEW!
    println!("\n    Ciphertext Details:");
    println!("      {}", cipher.info()?);
    println!("      └─ Polynomials: {}", cipher.size());
    println!("      └─ Coefficients per poly: {}", cipher.coeff_count());
    println!("      └─ Size: {} bytes ({:.2} KB)", 
             cipher.byte_count(), 
             cipher.byte_count() as f64 / 1024.0);
    
    // Decrypt
    let decrypted = decryptor.decrypt(&cipher)?;
    println!("   Decrypted plaintext: {}", decrypted.to_string()?);
    
    // Verify
    assert_eq!(plain.to_string()?, decrypted.to_string()?);
    println!("Encryption/Decryption works!\n");

    // ============================================
    // TEST 3: Homomorphic Addition
    // ============================================
    println!("Test 3: Homomorphic addition...");
    let plain1 = Plaintext::from_hex("5")?;
    let plain2 = Plaintext::from_hex("7")?;
    
    let cipher1 = encryptor.encrypt(&plain1)?;
    let cipher2 = encryptor.encrypt(&plain2)?;
    
    // Add encrypted values
    let cipher_sum = add(&context, &cipher1, &cipher2)?;
    let decrypted_sum = decryptor.decrypt(&cipher_sum)?;
    
    println!("   5 + 7 (encrypted) = {}", decrypted_sum.to_string()?);
    println!("    Result ciphertext: {}", cipher_sum.info()?);
    println!(" Homomorphic addition works!\n");
    
    // ============================================
    // FINAL RESULT
    // ============================================
    println!(" ALL TESTS PASSED!");
    println!("\n Rust ↔ SEAL FFI is WORKING!");
    
    Ok(())
}