package com.example.hegrpc.analytics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.example.hegrpc.data.PatientDataEntry;
import com.example.hegrpc.manager.HospitalManager;
import com.example.hegrpc.model.Hospital;
import com.example.hegrpc.service.HEClientService;
import com.google.protobuf.ByteString;

import he_service.HeService.BinaryOpResponse;
import he_service.HeService.DecryptResponse;

/**
 * Regional Analytics - Privacy-Preserving Aggregation
 * 
 * This is the CORE feature of homomorphic encryption:
 * Sum encrypted data from multiple hospitals WITHOUT decrypting!
 * 
 * The regional health authority can compute totals while
 * each hospital's individual data remains private.
 */
public class RegionalAnalytics {
    
    private final Scanner scanner;
    private final HEClientService heClient;
    private final HospitalManager hospitalManager;
    
    public RegionalAnalytics(Scanner scanner, HEClientService heClient, HospitalManager hospitalManager) {
        this.scanner = scanner;
        this.heClient = heClient;
        this.hospitalManager = hospitalManager;
    }
    
    /**
     * Show the regional analytics submenu
     */
    public void showMenu() {
        boolean inSubmenu = true;
        
        while (inSubmenu) {
            System.out.println();
            System.out.println("┌──────────────────────────────────────────────────────────────────────────────┐");
            System.out.println("│                           REGIONAL ANALYTICS                                 │");
            System.out.println("│              Privacy-Preserving Computation on Encrypted Data                │");
            System.out.println("├──────────────────────────────────────────────────────────────────────────────┤");
            System.out.println("│   [1] Sum Department Data Across Region                                      │");
            System.out.println("│   [2] Sum All Hospitals (Same Department)                                    │");
            System.out.println("│   [3] Compare Two Hospitals (Encrypted)                                      │");
            System.out.println("│   [4] View Regional Summary                                                  │");
            System.out.println("│   [0] Back to Main Menu                                                      │");
            System.out.println("└──────────────────────────────────────────────────────────────────────────────┘");
            System.out.print("   Enter your choice: ");
            
            int choice = readIntChoice(0, 4);
            
            switch (choice) {
                case 1 -> sumDepartmentAcrossRegion();
                case 2 -> sumAllHospitalsSameDepartment();
                case 3 -> compareTwoHospitals();
                case 4 -> viewRegionalSummary();
                case 0 -> inSubmenu = false;
            }
        }
    }
    
    /**
     * Sum department data across all hospitals in a region
     */
    private void sumDepartmentAcrossRegion() {
        // Check prerequisites
        if (hospitalManager.getHospitalCount() == 0) {
            System.out.println("\n      No hospitals registered yet!");
            pressEnterToContinue();
            return;
        }
        
        // Select region
        System.out.println("\n   Select region to analyze:");
        for (int i = 0; i < HospitalManager.REGIONS.length; i++) {
            List<Hospital> regionHospitals = hospitalManager.getHospitalsByRegion(HospitalManager.REGIONS[i]);
            int withData = (int) regionHospitals.stream()
                .filter(h -> !h.getEncryptedDepartmentData().isEmpty())
                .count();
            System.out.printf("   [%d] %s (%d hospitals, %d with data)%n", 
                i + 1, HospitalManager.REGIONS[i], regionHospitals.size(), withData);
        }
        System.out.print("   Enter region number: ");
        
        int regionChoice = readIntChoice(1, HospitalManager.REGIONS.length);
        String region = HospitalManager.REGIONS[regionChoice - 1];
        
        // Get hospitals in region with data
        List<Hospital> regionHospitals = hospitalManager.getHospitalsByRegion(region);
        List<Hospital> hospitalsWithData = regionHospitals.stream()
            .filter(h -> !h.getEncryptedDepartmentData().isEmpty())
            .toList();
        
        if (hospitalsWithData.isEmpty()) {
            System.out.println("\n   📭 No hospitals in this region have encrypted data yet!");
            System.out.println("   Add department data first using Patient Data Entry.");
            pressEnterToContinue();
            return;
        }
        
        // Find common departments
        Set<String> commonDepts = findCommonDepartments(hospitalsWithData);
        if (commonDepts.isEmpty()) {
            System.out.println("\n      No common departments found across hospitals in this region.");
            System.out.println("   Make sure at least 2 hospitals have data for the same department.");
            pressEnterToContinue();
            return;
        }
        
        // Select department
        System.out.println("\n   Select department to aggregate:");
        List<String> deptList = new ArrayList<>(commonDepts);
        for (int i = 0; i < deptList.size(); i++) {
            final String deptName = deptList.get(i);
            long hospitalCount = hospitalsWithData.stream()
                .filter(h -> h.hasDepartment(deptName))
                .count();
            System.out.printf("   [%d] %s (%d hospitals have data)%n", i + 1, deptList.get(i), hospitalCount);
        }
        System.out.print("   Enter department number: ");
        
        int deptChoice = readIntChoice(1, deptList.size());
        String department = deptList.get(deptChoice - 1);
        
        // Get hospitals with this department
        List<Hospital> hospitalsForDept = hospitalsWithData.stream()
            .filter(h -> h.hasDepartment(department))
            .toList();
        
        if (hospitalsForDept.size() < 2) {
            System.out.println("\n       Need at least 2 hospitals with data for meaningful aggregation.");
            pressEnterToContinue();
            return;
        }
        
        // Perform homomorphic addition
        performHomomorphicSum(hospitalsForDept, department, region);
    }
    
    /**
     * Sum all hospitals for the same department (regardless of region)
     */
    private void sumAllHospitalsSameDepartment() {
        if (hospitalManager.getHospitalCount() == 0) {
            System.out.println("\n      No hospitals registered yet!");
            pressEnterToContinue();
            return;
        }
        
        // Find all hospitals with data
        List<Hospital> hospitalsWithData = hospitalManager.getAllHospitals().stream()
            .filter(h -> !h.getEncryptedDepartmentData().isEmpty())
            .toList();
        
        if (hospitalsWithData.size() < 2) {
            System.out.println("\n      Need at least 2 hospitals with encrypted data!");
            pressEnterToContinue();
            return;
        }
        
        // Find common departments
        Set<String> commonDepts = findCommonDepartments(hospitalsWithData);
        if (commonDepts.isEmpty()) {
            System.out.println("\n      No common departments found across hospitals.");
            pressEnterToContinue();
            return;
        }
        
        // Select department
        System.out.println("\n   Select department to aggregate across ALL hospitals:");
        List<String> deptList = new ArrayList<>(commonDepts);
        for (int i = 0; i < deptList.size(); i++) {
            final String deptName = deptList.get(i);
            long hospitalCount = hospitalsWithData.stream()
                .filter(h -> h.hasDepartment(deptName))
                .count();
            System.out.printf("   [%d] %s (%d hospitals)%n", i + 1, deptList.get(i), hospitalCount);
        }
        System.out.print("   Enter department number: ");
        
        int deptChoice = readIntChoice(1, deptList.size());
        String department = deptList.get(deptChoice - 1);
        
        List<Hospital> hospitalsForDept = hospitalsWithData.stream()
            .filter(h -> h.hasDepartment(department))
            .toList();
        
        performHomomorphicSum(hospitalsForDept, department, "All Regions");
    }
    
    /**
     * Compare data from two hospitals using encrypted addition
     */
    private void compareTwoHospitals() {
        if (hospitalManager.getHospitalCount() < 2) {
            System.out.println("\n      Need at least 2 hospitals to compare!");
            pressEnterToContinue();
            return;
        }
        
        // Display hospitals
        hospitalManager.displayAllHospitals();
        
        // Select first hospital
        System.out.print("\n   Enter first Hospital ID: ");
        String id1 = scanner.nextLine().trim().toUpperCase();
        Hospital hospital1 = hospitalManager.getHospital(id1);
        if (hospital1 == null || hospital1.getEncryptedDepartmentData().isEmpty()) {
            System.out.println("      Hospital not found or has no data: " + id1);
            pressEnterToContinue();
            return;
        }
        
        // Select second hospital
        System.out.print("   Enter second Hospital ID: ");
        String id2 = scanner.nextLine().trim().toUpperCase();
        Hospital hospital2 = hospitalManager.getHospital(id2);
        if (hospital2 == null || hospital2.getEncryptedDepartmentData().isEmpty()) {
            System.out.println("      Hospital not found or has no data: " + id2);
            pressEnterToContinue();
            return;
        }
        
        if (id1.equals(id2)) {
            System.out.println("      Please select two different hospitals!");
            pressEnterToContinue();
            return;
        }
        
        // Find common departments
        Set<String> common = new HashSet<>(hospital1.getEncryptedDepartmentData().keySet());
        common.retainAll(hospital2.getEncryptedDepartmentData().keySet());
        
        if (common.isEmpty()) {
            System.out.println("      These hospitals have no common departments with data.");
            pressEnterToContinue();
            return;
        }
        
        // Select department
        System.out.println("\n   Select department to compare:");
        List<String> deptList = new ArrayList<>(common);
        for (int i = 0; i < deptList.size(); i++) {
            System.out.printf("   [%d] %s%n", i + 1, deptList.get(i));
        }
        System.out.print("   Enter department number: ");
        
        int deptChoice = readIntChoice(1, deptList.size());
        String department = deptList.get(deptChoice - 1);
        
        // Perform comparison
        performHomomorphicSum(List.of(hospital1, hospital2), department, "Comparison");
    }
    
    /**
     * View summary of all regional data
     */
    private void viewRegionalSummary() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                            REGIONAL DATA SUMMARY                             ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        
        for (String region : HospitalManager.REGIONS) {
            List<Hospital> hospitals = hospitalManager.getHospitalsByRegion(region);
            int totalHospitals = hospitals.size();
            int withData = (int) hospitals.stream()
                .filter(h -> !h.getEncryptedDepartmentData().isEmpty())
                .count();
            int totalDepts = hospitals.stream()
                .mapToInt(h -> h.getEncryptedDepartmentData().size())
                .sum();
            
            System.out.printf("║     %-20s                                                      ║%n", region);
            System.out.printf("║     Hospitals: %d total, %d with data                                        ║%n", 
                totalHospitals, withData);
            System.out.printf("║     Total encrypted datasets: %d                                             ║%n", totalDepts);
            System.out.println("║                                                                              ║");
        }
        
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║     Use options 1-3 to perform privacy-preserving computations!              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        
        pressEnterToContinue();
    }
    
    /**
     * CORE FUNCTION: Perform homomorphic sum on encrypted data
     */
    private void performHomomorphicSum(List<Hospital> hospitals, String department, String scope) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 PRIVACY-PRESERVING REGIONAL AGGREGATION                      ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Scope: %-69s║%n", scope);
        System.out.printf("║  Department: %-64s║%n", department);
        System.out.printf("║  Hospitals: %-65s║%n", hospitals.size() + " participating");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        
        // Show individual hospital data (original values for verification)
        System.out.println("║                                                                              ║");
        System.out.println("║     INDIVIDUAL HOSPITAL DATA (private - only hospital knows):                ║");
        System.out.println("║  ┌─────────────────────────────────────────────────────────────────────────┐ ║");
        
        long[] expectedTotals = new long[PatientDataEntry.DATA_FIELDS.length];
        
        for (Hospital h : hospitals) {
            long[] original = h.getOriginalData().get(department);
            System.out.printf("║  │  %-20s: [%3d, %3d, %3d, %3d]                          │ ║%n",
                h.getId(), original[0], original[1], original[2], original[3]);
            
            for (int i = 0; i < original.length && i < expectedTotals.length; i++) {
                expectedTotals[i] += original[i];
            }
        }
        System.out.println("║  └─────────────────────────────────────────────────────────────────────────┘ ║");
        
        // Show expected totals (for verification)
        System.out.println("║                                                                              ║");
        System.out.printf("║     Expected totals (if we had access to raw data):                          ║%n");
        System.out.printf("║     [%d, %d, %d, %d]                                                         ║%n",
            expectedTotals[0], expectedTotals[1], expectedTotals[2], expectedTotals[3]);
        
        // Now perform homomorphic addition
        System.out.println("║                                                                              ║");
        System.out.println("║     PERFORMING HOMOMORPHIC ADDITION ON ENCRYPTED DATA...                     ║");
        System.out.println("║                                                                              ║");
        
        try {
            // Use first hospital's session for decryption
            Hospital firstHospital = hospitals.get(0);
            String sessionId = firstHospital.getSessionId();
            
            // Start with first hospital's ciphertext
            byte[] accumulatedCiphertext = firstHospital.getEncryptedDepartmentData().get(department);
            System.out.printf("║  Step 1: Start with %s encrypted data (%,d bytes)        ║%n",
                firstHospital.getId(), accumulatedCiphertext.length);
            
            // Add each subsequent hospital's data
            for (int i = 1; i < hospitals.size(); i++) {
                Hospital h = hospitals.get(i);
                byte[] hospitalCiphertext = h.getEncryptedDepartmentData().get(department);
                
                System.out.printf("║  Step %d: + Add %s encrypted data (%,d bytes)            ║%n",
                    i + 1, h.getId(), hospitalCiphertext.length);
                
                // Perform homomorphic addition
                BinaryOpResponse addResult = heClient.add(
                    sessionId,
                    ByteString.copyFrom(accumulatedCiphertext),
                    ByteString.copyFrom(hospitalCiphertext)
                );
                
                accumulatedCiphertext = addResult.getResultCiphertext().toByteArray();
            }
            
            System.out.println("║                                                                              ║");
            System.out.printf("║     Homomorphic addition complete! Result: %,d bytes              ║%n",
                accumulatedCiphertext.length);
            
            // Decrypt the result
            System.out.println("║                                                                              ║");
            System.out.println("║     DECRYPTING AGGREGATED RESULT...                                          ║");
            
            DecryptResponse decrypted = heClient.decrypt(sessionId, ByteString.copyFrom(accumulatedCiphertext));
            List<Long> result = decrypted.getValuesList();
            
            System.out.println("║                                                                              ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
            System.out.println("║                              REGIONAL TOTALS                                 ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
            
            for (int i = 0; i < PatientDataEntry.DATA_FIELDS.length && i < result.size(); i++) {
                String status = result.get(i).equals(expectedTotals[i]) ? "✓" : "?";
                System.out.printf("║   %s %-25s: %6d                                    ║%n",
                    status, PatientDataEntry.DATA_FIELDS[i], result.get(i));
            }
            
            System.out.println("║                                                                              ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
            System.out.println("║      PRIVACY PRESERVED!                                                      ║");
            System.out.println("║     • Individual hospital data was NEVER decrypted                          ║");
            System.out.println("║     • Computation happened entirely on encrypted data                       ║");
            System.out.println("║     • Only the aggregated result was decrypted                              ║");
            System.out.println("║     • No hospital revealed its actual patient counts!                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            System.out.println("║                                                                              ║");
            System.out.println("║    Error during homomorphic computation: " + e.getMessage());
            System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        }
        
        pressEnterToContinue();
    }
    
    /**
     * Find departments that exist in all hospitals
     */
    private Set<String> findCommonDepartments(List<Hospital> hospitals) {
        if (hospitals.isEmpty()) return new HashSet<>();
        
        // Start with all departments from first hospital
        Set<String> common = new HashSet<>(hospitals.get(0).getEncryptedDepartmentData().keySet());
        
        // Find departments that exist in at least 2 hospitals
        Set<String> result = new HashSet<>();
        for (String dept : common) {
            long count = hospitals.stream()
                .filter(h -> h.hasDepartment(dept))
                .count();
            if (count >= 2) {
                result.add(dept);
            }
        }
        
        // Also add departments from other hospitals that appear in 2+
        for (Hospital h : hospitals) {
            for (String dept : h.getEncryptedDepartmentData().keySet()) {
                long count = hospitals.stream()
                    .filter(hosp -> hosp.hasDepartment(dept))
                    .count();
                if (count >= 2) {
                    result.add(dept);
                }
            }
        }
        
        return result;
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
