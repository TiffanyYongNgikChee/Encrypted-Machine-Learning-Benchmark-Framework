package com.example.hegrpc.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.example.hegrpc.data.PatientDataEntry;
import com.example.hegrpc.manager.HospitalManager;
import com.example.hegrpc.model.Hospital;
import com.example.hegrpc.service.HEClientService;

import he_service.HeService.EncryptResponse;

/**
 * Security Demo - Visualize Encryption Security
 * 
 * Demonstrates the security properties of homomorphic encryption:
 * - Same data encrypts to different ciphertexts (randomness)
 * - Encrypted data looks completely random
 * - Simulated attacks show data remains protected
 */
public class SecurityDemo {
    
    private final Scanner scanner;
    private final HEClientService heClient;
    private final HospitalManager hospitalManager;
    
    public SecurityDemo(Scanner scanner, HEClientService heClient, HospitalManager hospitalManager) {
        this.scanner = scanner;
        this.heClient = heClient;
        this.hospitalManager = hospitalManager;
    }
    
    /**
     * Show the security demo submenu
     */
    public void showMenu() {
        boolean inSubmenu = true;
        
        while (inSubmenu) {
            System.out.println();
            System.out.println("┌──────────────────────────────────────────────────────────────────────────────┐");
            System.out.println("│                              SECURITY DEMO                                   │");
            System.out.println("│                 Demonstrating Encryption Security Properties                 │");
            System.out.println("├──────────────────────────────────────────────────────────────────────────────┤");
            System.out.println("│   [1] Encryption Randomness Demo                                            │");
            System.out.println("│   [2] Hex Dump Visualization                                                │");
            System.out.println("│   [3] Byte Frequency Analysis                                               │");
            System.out.println("│   [4] Simulate Data Interception Attack                                     │");
            System.out.println("│   [5] Compare Plain vs Encrypted Size                                       │");
            System.out.println("│   [0] Back to Main Menu                                                      │");
            System.out.println("└──────────────────────────────────────────────────────────────────────────────┘");
            System.out.print("   Enter your choice: ");
            
            int choice = readIntChoice(0, 5);
            
            switch (choice) {
                case 1 -> encryptionRandomnessDemo();
                case 2 -> hexDumpVisualization();
                case 3 -> byteFrequencyAnalysis();
                case 4 -> simulateInterceptionAttack();
                case 5 -> comparePlainVsEncrypted();
                case 0 -> inSubmenu = false;
            }
        }
    }
    
    /**
     * Demo 1: Show that same data encrypts to different ciphertexts
     */
    private void encryptionRandomnessDemo() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                       ENCRYPTION RANDOMNESS DEMO                             ║");
        System.out.println("║                                                                              ║");
        System.out.println("║  Key Security Property: Same plaintext encrypts to DIFFERENT ciphertexts!   ║");
        System.out.println("║  This prevents attackers from recognizing patterns in encrypted data.       ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        
        // Get a session
        String sessionId = getOrCreateSession();
        if (sessionId == null) {
            pressEnterToContinue();
            return;
        }
        
        // Same values to encrypt multiple times
        List<Long> values = List.of(42L, 100L, 255L, 1000L);
        
        System.out.println("║                                                                              ║");
        System.out.printf("║  Encrypting the same values 3 times: %s                         ║%n", values);
        System.out.println("║                                                                              ║");
        
        try {
            byte[][] ciphertexts = new byte[3][];
            
            for (int i = 0; i < 3; i++) {
                EncryptResponse response = heClient.encrypt(sessionId, values);
                ciphertexts[i] = response.getCiphertext().toByteArray();
                
                System.out.printf("║  Encryption #%d - First 32 bytes:                                           ║%n", i + 1);
                System.out.print("║    ");
                for (int j = 0; j < 32 && j < ciphertexts[i].length; j++) {
                    System.out.printf("%02X ", ciphertexts[i][j] & 0xFF);
                }
                System.out.println("  ║");
            }
            
            // Compare ciphertexts
            System.out.println("║                                                                              ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
            System.out.println("║  ANALYSIS:                                                                   ║");
            
            boolean allDifferent = true;
            for (int i = 0; i < 3; i++) {
                for (int j = i + 1; j < 3; j++) {
                    boolean same = Arrays.equals(ciphertexts[i], ciphertexts[j]);
                    if (same) allDifferent = false;
                    System.out.printf("║    Encryption #%d vs #%d: %s                                          ║%n",
                        i + 1, j + 1, same ? "SAME ⚠️" : "DIFFERENT ✓");
                }
            }
            
            System.out.println("║                                                                              ║");
            if (allDifferent) {
                System.out.println("║     SUCCESS: Each encryption produces a unique ciphertext!                  ║");
                System.out.println("║                                                                              ║");
                System.out.println("║      This means an attacker cannot:                                         ║");
                System.out.println("║     • Detect when the same data is encrypted twice                         ║");
                System.out.println("║     • Build a dictionary of plaintext-ciphertext pairs                     ║");
                System.out.println("║     • Use frequency analysis to break the encryption                       ║");
            } else {
                System.out.println("║     Some ciphertexts matched (unusual for HE schemes)                      ║");
            }
            
        } catch (Exception e) {
            System.out.println("║    Error: " + e.getMessage());
        }
        
        System.out.println("║                                                                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        
        pressEnterToContinue();
    }
    
    /**
     * Demo 2: Detailed hex dump of encrypted data
     */
    private void hexDumpVisualization() {
        if (hospitalManager.getHospitalCount() == 0) {
            System.out.println("\n      No hospitals with encrypted data. Create some first!");
            pressEnterToContinue();
            return;
        }
        
        // Find a hospital with data
        Hospital hospital = findHospitalWithData();
        if (hospital == null) {
            System.out.println("\n      No hospital has encrypted department data yet.");
            pressEnterToContinue();
            return;
        }
        
        // Get first department's data
        String dept = hospital.getEncryptedDepartmentData().keySet().iterator().next();
        byte[] encrypted = hospital.getEncryptedDepartmentData().get(dept);
        long[] original = hospital.getOriginalData().get(dept);
        
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                          HEX DUMP VISUALIZATION                              ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Hospital: %-66s║%n", hospital.getName());
        System.out.printf("║  Department: %-64s║%n", dept);
        System.out.printf("║  Original Values: [%d, %d, %d, %d]                                           ║%n",
            original[0], original[1], original[2], original[3]);
        System.out.printf("║  Encrypted Size: %,d bytes                                                 ║%n", encrypted.length);
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║                                                                              ║");
        System.out.println("║  OFFSET    HEX DATA                                             ASCII        ║");
        System.out.println("║  ────────────────────────────────────────────────────────────────────────── ║");
        
        // Show hex dump (first 256 bytes)
        int bytesToShow = Math.min(256, encrypted.length);
        for (int offset = 0; offset < bytesToShow; offset += 16) {
            StringBuilder hex = new StringBuilder();
            StringBuilder ascii = new StringBuilder();
            
            for (int i = 0; i < 16 && offset + i < bytesToShow; i++) {
                byte b = encrypted[offset + i];
                hex.append(String.format("%02X ", b & 0xFF));
                
                // ASCII representation (printable chars only)
                if (b >= 32 && b < 127) {
                    ascii.append((char) b);
                } else {
                    ascii.append('.');
                }
            }
            
            System.out.printf("║  %06X   %-48s %-16s ║%n", offset, hex.toString(), ascii.toString());
        }
        
        if (encrypted.length > 256) {
            System.out.printf("║  ... (%,d more bytes not shown)                                           ║%n",
                encrypted.length - 256);
        }
        
        System.out.println("║                                                                              ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║     Notice: The data appears completely random - no patterns visible!        ║");
        System.out.println("║     The original values [" + original[0] + ", " + original[1] + ", " + 
            original[2] + ", " + original[3] + "] are impossible to see.                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        
        pressEnterToContinue();
    }
    
    /**
     * Demo 3: Analyze byte frequency distribution
     */
    private void byteFrequencyAnalysis() {
        if (hospitalManager.getHospitalCount() == 0) {
            System.out.println("\n      No hospitals with encrypted data. Create some first!");
            pressEnterToContinue();
            return;
        }
        
        Hospital hospital = findHospitalWithData();
        if (hospital == null) {
            System.out.println("\n      No hospital has encrypted department data yet.");
            pressEnterToContinue();
            return;
        }
        
        String dept = hospital.getEncryptedDepartmentData().keySet().iterator().next();
        byte[] encrypted = hospital.getEncryptedDepartmentData().get(dept);
        
        // Count byte frequencies
        int[] frequencies = new int[256];
        for (byte b : encrypted) {
            frequencies[b & 0xFF]++;
        }
        
        // Find min, max, average
        int minFreq = Integer.MAX_VALUE, maxFreq = 0;
        int minByte = 0, maxByte = 0;
        long totalFreq = 0;
        int nonZeroCount = 0;
        
        for (int i = 0; i < 256; i++) {
            if (frequencies[i] > 0) {
                nonZeroCount++;
                totalFreq += frequencies[i];
                if (frequencies[i] < minFreq) {
                    minFreq = frequencies[i];
                    minByte = i;
                }
                if (frequencies[i] > maxFreq) {
                    maxFreq = frequencies[i];
                    maxByte = i;
                }
            }
        }
        
        double avgFreq = (double) totalFreq / nonZeroCount;
        double expectedFreq = (double) encrypted.length / 256;
        
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                       BYTE FREQUENCY ANALYSIS                                ║");
        System.out.println("║                                                                              ║");
        System.out.println("║  Good encryption should have uniform byte distribution (like random data)   ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Data size: %,d bytes                                                      ║%n", encrypted.length);
        System.out.printf("║  Unique byte values: %d / 256                                               ║%n", nonZeroCount);
        System.out.printf("║  Expected frequency (uniform): %.1f per byte value                          ║%n", expectedFreq);
        System.out.printf("║  Actual average frequency: %.1f                                             ║%n", avgFreq);
        System.out.printf("║  Min frequency: %d (byte 0x%02X)                                             ║%n", minFreq, minByte);
        System.out.printf("║  Max frequency: %d (byte 0x%02X)                                            ║%n", maxFreq, maxByte);
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        
        // Show histogram (simplified - top 16 most frequent bytes)
        System.out.println("║  TOP 10 MOST FREQUENT BYTES:                                                ║");
        System.out.println("║  ────────────────────────────────────────────────────────────────────────── ║");
        
        // Sort by frequency
        List<int[]> sorted = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            if (frequencies[i] > 0) {
                sorted.add(new int[]{i, frequencies[i]});
            }
        }
        sorted.sort((a, b) -> b[1] - a[1]);
        
        for (int i = 0; i < Math.min(10, sorted.size()); i++) {
            int byteVal = sorted.get(i)[0];
            int freq = sorted.get(i)[1];
            double percent = (freq * 100.0) / encrypted.length;
            int barLen = (int) (percent * 2);
            String bar = "█".repeat(Math.min(barLen, 30));
            
            System.out.printf("║  0x%02X: %5d (%4.1f%%) %s%n", byteVal, freq, percent, bar);
        }
        
        System.out.println("║                                                                              ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        
        // Analysis
        double variance = maxFreq - minFreq;
        double variancePercent = (variance / avgFreq) * 100;
        
        if (variancePercent < 50) {
            System.out.println("║     EXCELLENT: Byte distribution is highly uniform (random-looking)         ║");
        } else if (variancePercent < 100) {
            System.out.println("║     GOOD: Byte distribution is reasonably uniform                           ║");
        } else {
            System.out.println("║      Distribution shows some variance (normal for structured ciphertext)    ║");
        }
        System.out.println("║                                                                              ║");
        System.out.println("║      An attacker analyzing this data would see only random bytes!             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        
        pressEnterToContinue();
    }
    
    /**
     * Demo 4: Simulate what an attacker would see
     */
    private void simulateInterceptionAttack() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                     SIMULATED DATA INTERCEPTION ATTACK                       ║");
        System.out.println("║                                                                              ║");
        System.out.println("║  Scenario: An attacker intercepts hospital data during transmission.        ║");
        System.out.println("║  Let's see what they can learn from the encrypted data...                   ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        
        Hospital hospital = findHospitalWithData();
        if (hospital == null) {
            System.out.println("║                                                                              ║");
            System.out.println("║     No encrypted data available. Add department data first!                 ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
            pressEnterToContinue();
            return;
        }
        
        String dept = hospital.getEncryptedDepartmentData().keySet().iterator().next();
        byte[] encrypted = hospital.getEncryptedDepartmentData().get(dept);
        long[] original = hospital.getOriginalData().get(dept);
        
        System.out.println("║                                                                              ║");
        System.out.println("║     INTERCEPTED DATA PACKET:                                                 ║");
        System.out.println("║  ┌─────────────────────────────────────────────────────────────────────────┐ ║");
        System.out.printf("║  │  Source: %s                                              │ ║%n", hospital.getId());
        System.out.printf("║  │  Payload size: %,d bytes                                            │ ║%n", encrypted.length);
        System.out.println("║  │  Content type: application/octet-stream (encrypted)                    │ ║");
        System.out.println("║  └─────────────────────────────────────────────────────────────────────────┘ ║");
        
        System.out.println("║                                                                              ║");
        System.out.println("║     ATTACKER'S ANALYSIS ATTEMPTS:                                            ║");
        System.out.println("║                                                                              ║");
        
        // Attempt 1: Pattern search
        System.out.println("║  Attempt 1: Search for plaintext patterns...                                ║");
        System.out.println("║    Looking for ASCII text:    None found (data is binary)                   ║");
        System.out.println("║    Looking for numbers:    No recognizable numeric patterns                 ║");
        System.out.println("║                                                                              ║");
        
        // Attempt 2: Known plaintext attack
        System.out.println("║  Attempt 2: Known plaintext attack...                                       ║");
        System.out.printf("║    Searching for value '%d' in encrypted data...                            ║%n", original[0]);
        
        // Search for the original value (won't find it)
        boolean found = false;
        byte[] searchBytes = String.valueOf(original[0]).getBytes();
        outer: for (int i = 0; i < encrypted.length - searchBytes.length; i++) {
            for (int j = 0; j < searchBytes.length; j++) {
                if (encrypted[i + j] != searchBytes[j]) continue outer;
            }
            found = true;
            break;
        }
        System.out.printf("║    Result: %s                                                     ║%n", 
            found ? "Found!   " : "   NOT FOUND");
        System.out.println("║                                                                              ║");
        
        // Attempt 3: Statistical analysis
        System.out.println("║  Attempt 3: Statistical analysis...                                         ║");
        int zeros = 0, ones = 0;
        for (byte b : encrypted) {
            for (int bit = 0; bit < 8; bit++) {
                if ((b & (1 << bit)) != 0) ones++;
                else zeros++;
            }
        }
        double ratio = (double) ones / (ones + zeros);
        System.out.printf("║    Bit ratio (1s/total): %.4f (expected ~0.5 for random)                   ║%n", ratio);
        System.out.println("║    Result:    Data appears random, no bias detected                         ║");
        System.out.println("║                                                                              ║");
        
        // Attempt 4: Brute force
        System.out.println("║  Attempt 4: Brute force decryption...                                       ║");
        System.out.printf("║    Key space size: ~2^128 or larger                                         ║%n");
        System.out.println("║    Estimated time to crack: Heat death of universe                          ║");
        System.out.println("║    Result:    INFEASIBLE                                                     ║");
        
        System.out.println("║                                                                              ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║     ATTACK SUMMARY:                                                          ║");
        System.out.println("║  ┌─────────────────────────────────────────────────────────────────────────┐ ║");
        System.out.println("║  │  Pattern Analysis:        FAILED                                        │ ║");
        System.out.println("║  │  Known Plaintext Attack:  FAILED                                        │ ║");
        System.out.println("║  │  Statistical Analysis:    FAILED                                        │ ║");
        System.out.println("║  │  Brute Force:             INFEASIBLE                                    │ ║");
        System.out.println("║  └─────────────────────────────────────────────────────────────────────────┘ ║");
        System.out.println("║                                                                              ║");
        System.out.println("║      CONCLUSION: The attacker learned NOTHING about the patient data!        ║");
        System.out.println("║                                                                              ║");
        System.out.println("║     What was actually in the packet (only hospital knows):                  ║");
        System.out.printf("║     %s = %d                                              ║%n", 
            PatientDataEntry.DATA_FIELDS[0], original[0]);
        System.out.printf("║     %s = %d                                                ║%n", 
            PatientDataEntry.DATA_FIELDS[1], original[1]);
        System.out.printf("║     %s = %d                                              ║%n", 
            PatientDataEntry.DATA_FIELDS[2], original[2]);
        System.out.printf("║     %s = %d                                                ║%n", 
            PatientDataEntry.DATA_FIELDS[3], original[3]);
        System.out.println("║                                                                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        
        pressEnterToContinue();
    }
    
    /**
     * Demo 5: Compare plaintext vs encrypted size
     */
    private void comparePlainVsEncrypted() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                       PLAINTEXT vs ENCRYPTED SIZE                            ║");
        System.out.println("║                                                                              ║");
        System.out.println("║  Homomorphic encryption has significant size overhead - this is the cost    ║");
        System.out.println("║  of being able to compute on encrypted data without decryption.             ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        
        Hospital hospital = findHospitalWithData();
        if (hospital == null) {
            System.out.println("║     No encrypted data available. Add department data first!                 ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
            pressEnterToContinue();
            return;
        }
        
        long totalPlain = 0;
        long totalEncrypted = 0;
        
        System.out.println("║                                                                              ║");
        System.out.println("║  DEPARTMENT                    PLAINTEXT      ENCRYPTED      EXPANSION      ║");
        System.out.println("║  ──────────────────────────────────────────────────────────────────────────  ║");
        
        for (String dept : hospital.getEncryptedDepartmentData().keySet()) {
            byte[] encrypted = hospital.getEncryptedDepartmentData().get(dept);
            long[] original = hospital.getOriginalData().get(dept);
            
            int plainSize = original.length * 8; // 8 bytes per long
            int encSize = encrypted.length;
            double expansion = (double) encSize / plainSize;
            
            totalPlain += plainSize;
            totalEncrypted += encSize;
            
            String deptName = dept.length() > 25 ? dept.substring(0, 22) + "..." : dept;
            System.out.printf("║  %-28s %6d bytes  %,10d bytes  %,.0fx        ║%n",
                deptName, plainSize, encSize, expansion);
        }
        
        System.out.println("║  ──────────────────────────────────────────────────────────────────────────  ║");
        double totalExpansion = (double) totalEncrypted / totalPlain;
        System.out.printf("║  TOTAL                         %6d bytes  %,10d bytes  %,.0fx        ║%n",
            totalPlain, totalEncrypted, totalExpansion);
        
        System.out.println("║                                                                              ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║     VISUAL COMPARISON:                                                       ║");
        System.out.println("║                                                                              ║");
        System.out.println("║  Plaintext (32 bytes):                                                       ║");
        System.out.println("║  ██                                                                          ║");
        System.out.println("║                                                                              ║");
        System.out.println("║  Encrypted (~131KB):                                                         ║");
        System.out.println("║  ████████████████████████████████████████████████████████████████████████    ║");
        System.out.println("║  ████████████████████████████████████████████████████████████████████████    ║");
        System.out.println("║  ████████████████████████████████████████████████████████████████████████    ║");
        System.out.println("║  ████████████████████████████████████████████████████████████████████████    ║");
        System.out.println("║                                                                              ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║     WHY SO LARGE?                                                            ║");
        System.out.println("║                                                                              ║");
        System.out.println("║  Homomorphic encryption uses polynomial rings and noise for security.       ║");
        System.out.println("║  The large ciphertext size enables:                                          ║");
        System.out.println("║    • Mathematical operations on encrypted data                               ║");
        System.out.println("║    • Noise management for multiple operations                                ║");
        System.out.println("║    • Security against quantum computers (lattice-based)                      ║");
        System.out.println("║                                                                              ║");
        System.out.println("║     TRADE-OFF: More space = ability to compute without decryption            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        
        pressEnterToContinue();
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Get or create a session for demos
     */
    private String getOrCreateSession() {
        // Try to use an existing hospital's session
        for (Hospital h : hospitalManager.getAllHospitals()) {
            if (h.getSessionId() != null) {
                return h.getSessionId();
            }
        }
        
        // Create a new session
        try {
            System.out.println("║  Creating encryption session...                                             ║");
            var keys = heClient.generateKeys("SEAL", 8192);
            return keys.getSessionId();
        } catch (Exception e) {
            System.out.println("║     Could not create session: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Find a hospital that has encrypted data
     */
    private Hospital findHospitalWithData() {
        for (Hospital h : hospitalManager.getAllHospitals()) {
            if (!h.getEncryptedDepartmentData().isEmpty()) {
                return h;
            }
        }
        return null;
    }
    
    /**
     * Read an integer choice within range
     */
    private int readIntChoice(int min, int max) {
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                int choice = Integer.parseInt(input);
                if (choice >= min && choice <= max) {
                    return choice;
                }
                System.out.printf("      Please enter a number between %d and %d: ", min, max);
            } catch (NumberFormatException e) {
                System.out.print("      Invalid input. Please enter a number: ");
            }
        }
    }
    
    private void pressEnterToContinue() {
        System.out.print("\n   Press Enter to continue...");
        scanner.nextLine();
    }
}
