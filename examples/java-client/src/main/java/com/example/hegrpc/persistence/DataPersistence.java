package com.example.hegrpc.persistence;

import com.example.hegrpc.data.PatientDataEntry;
import com.example.hegrpc.manager.HospitalManager;
import com.example.hegrpc.model.Hospital;
import com.example.hegrpc.service.HEClientService;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Data Persistence - Save and Load Encrypted Hospital Data
 * 
 * Saves encrypted data to JSON files with Base64 encoding.
 * This demonstrates how encrypted data can be stored and
 * transmitted while remaining secure.
 */
public class DataPersistence {
    
    private final Scanner scanner;
    private final HEClientService heClient;
    private final HospitalManager hospitalManager;
    
    // Default save directory
    private static final String SAVE_DIR = "hospital_data";
    
    public DataPersistence(Scanner scanner, HEClientService heClient, HospitalManager hospitalManager) {
        this.scanner = scanner;
        this.heClient = heClient;
        this.hospitalManager = hospitalManager;
    }
    
    /**
     * Show the save/load data submenu
     */
    public void showMenu() {
        boolean inSubmenu = true;
        
        while (inSubmenu) {
            System.out.println();
            System.out.println("┌──────────────────────────────────────────────────────────────────────────────┐");
            System.out.println("│                          💾 SAVE / LOAD DATA                                │");
            System.out.println("│                    Persist Encrypted Data to Files                          │");
            System.out.println("├──────────────────────────────────────────────────────────────────────────────┤");
            System.out.println("│   [1] Save All Hospitals to File                                            │");
            System.out.println("│   [2] Save Single Hospital                                                   │");
            System.out.println("│   [3] Load Data from File                                                    │");
            System.out.println("│   [4] List Saved Files                                                       │");
            System.out.println("│   [5] View File Contents (Preview)                                          │");
            System.out.println("│   [0] Back to Main Menu                                                      │");
            System.out.println("└──────────────────────────────────────────────────────────────────────────────┘");
            System.out.print("   Enter your choice: ");
            
            int choice = readIntChoice(0, 5);
            
            switch (choice) {
                case 1 -> saveAllHospitals();
                case 2 -> saveSingleHospital();
                case 3 -> loadFromFile();
                case 4 -> listSavedFiles();
                case 5 -> viewFileContents();
                case 0 -> inSubmenu = false;
            }
        }
    }
    
    /**
     * Save all hospitals to a single JSON file
     */
    private void saveAllHospitals() {
        if (hospitalManager.getHospitalCount() == 0) {
            System.out.println("\n   📭 No hospitals to save!");
            pressEnterToContinue();
            return;
        }
        
        // Create save directory if needed
        ensureSaveDirectory();
        
        // Generate filename with timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "all_hospitals_" + timestamp + ".json";
        Path filePath = Paths.get(SAVE_DIR, filename);
        
        System.out.println("\n   💾 Saving all hospitals to: " + filePath);
        System.out.println("   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        try {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"exportDate\": \"").append(LocalDateTime.now().toString()).append("\",\n");
            json.append("  \"totalHospitals\": ").append(hospitalManager.getHospitalCount()).append(",\n");
            json.append("  \"hospitals\": [\n");
            
            Collection<Hospital> hospitals = hospitalManager.getAllHospitals();
            int count = 0;
            for (Hospital h : hospitals) {
                json.append(hospitalToJson(h, "    "));
                count++;
                if (count < hospitals.size()) {
                    json.append(",");
                }
                json.append("\n");
                System.out.printf("   ✓ Saved: %s - %s%n", h.getId(), h.getName());
            }
            
            json.append("  ]\n");
            json.append("}\n");
            
            // Write to file
            Files.writeString(filePath, json.toString());
            
            System.out.println("   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("   ✅ Successfully saved " + count + " hospital(s)!");
            System.out.println("   📁 File: " + filePath.toAbsolutePath());
            System.out.printf("   📊 Size: %,d bytes%n", Files.size(filePath));
            
        } catch (IOException e) {
            System.out.println("   ❌ Error saving file: " + e.getMessage());
        }
        
        pressEnterToContinue();
    }
    
    /**
     * Save a single hospital to JSON file
     */
    private void saveSingleHospital() {
        if (hospitalManager.getHospitalCount() == 0) {
            System.out.println("\n   📭 No hospitals to save!");
            pressEnterToContinue();
            return;
        }
        
        hospitalManager.displayAllHospitals();
        System.out.print("   Enter Hospital ID to save: ");
        String id = scanner.nextLine().trim().toUpperCase();
        
        Hospital hospital = hospitalManager.getHospital(id);
        if (hospital == null) {
            System.out.println("   ❌ Hospital not found: " + id);
            pressEnterToContinue();
            return;
        }
        
        // Create save directory if needed
        ensureSaveDirectory();
        
        // Generate filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String safeName = hospital.getName().replaceAll("[^a-zA-Z0-9]", "_");
        String filename = hospital.getId() + "_" + safeName + "_" + timestamp + ".json";
        Path filePath = Paths.get(SAVE_DIR, filename);
        
        System.out.println("\n   💾 Saving hospital to: " + filePath);
        
        try {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"exportDate\": \"").append(LocalDateTime.now().toString()).append("\",\n");
            json.append("  \"hospital\": ");
            json.append(hospitalToJson(hospital, "  "));
            json.append("\n}\n");
            
            Files.writeString(filePath, json.toString());
            
            System.out.println("   ✅ Successfully saved!");
            System.out.println("   📁 File: " + filePath.toAbsolutePath());
            System.out.printf("   📊 Size: %,d bytes%n", Files.size(filePath));
            
        } catch (IOException e) {
            System.out.println("   ❌ Error saving file: " + e.getMessage());
        }
        
        pressEnterToContinue();
    }
    
    /**
     * Load hospitals from a JSON file
     */
    private void loadFromFile() {
        ensureSaveDirectory();
        
        // List available files
        List<Path> jsonFiles = listJsonFiles();
        if (jsonFiles.isEmpty()) {
            System.out.println("\n   📭 No saved files found in " + SAVE_DIR + "/");
            System.out.println("   Save some data first!");
            pressEnterToContinue();
            return;
        }
        
        System.out.println("\n   📂 Available files:");
        for (int i = 0; i < jsonFiles.size(); i++) {
            Path file = jsonFiles.get(i);
            try {
                long size = Files.size(file);
                System.out.printf("   [%d] %s (%,d bytes)%n", i + 1, file.getFileName(), size);
            } catch (IOException e) {
                System.out.printf("   [%d] %s%n", i + 1, file.getFileName());
            }
        }
        System.out.print("\n   Enter file number to load: ");
        
        int choice = readIntChoice(1, jsonFiles.size());
        Path selectedFile = jsonFiles.get(choice - 1);
        
        System.out.println("\n   📂 Loading from: " + selectedFile.getFileName());
        System.out.println("   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        try {
            String content = Files.readString(selectedFile);
            int hospitalsLoaded = parseAndLoadJson(content);
            
            System.out.println("   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("   ✅ Loaded " + hospitalsLoaded + " hospital(s)!");
            
        } catch (IOException e) {
            System.out.println("   ❌ Error reading file: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("   ❌ Error parsing file: " + e.getMessage());
        }
        
        pressEnterToContinue();
    }
    
    /**
     * List all saved JSON files
     */
    private void listSavedFiles() {
        ensureSaveDirectory();
        List<Path> jsonFiles = listJsonFiles();
        
        if (jsonFiles.isEmpty()) {
            System.out.println("\n   📭 No saved files found in " + SAVE_DIR + "/");
            pressEnterToContinue();
            return;
        }
        
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                            📁 SAVED FILES                                    ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        
        for (Path file : jsonFiles) {
            try {
                long size = Files.size(file);
                String modified = Files.getLastModifiedTime(file).toString().substring(0, 19);
                System.out.printf("║  📄 %-50s %,10d bytes ║%n", 
                    truncate(file.getFileName().toString(), 50), size);
            } catch (IOException e) {
                System.out.printf("║  📄 %-68s ║%n", file.getFileName());
            }
        }
        
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Total: %d file(s) in %s/                                           ║%n", 
            jsonFiles.size(), SAVE_DIR);
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        
        pressEnterToContinue();
    }
    
    /**
     * View contents of a saved file
     */
    private void viewFileContents() {
        ensureSaveDirectory();
        List<Path> jsonFiles = listJsonFiles();
        
        if (jsonFiles.isEmpty()) {
            System.out.println("\n   📭 No saved files found!");
            pressEnterToContinue();
            return;
        }
        
        System.out.println("\n   📂 Available files:");
        for (int i = 0; i < jsonFiles.size(); i++) {
            System.out.printf("   [%d] %s%n", i + 1, jsonFiles.get(i).getFileName());
        }
        System.out.print("\n   Enter file number to preview: ");
        
        int choice = readIntChoice(1, jsonFiles.size());
        Path selectedFile = jsonFiles.get(choice - 1);
        
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                          📄 FILE PREVIEW                                     ║");
        System.out.printf("║  File: %-70s║%n", truncate(selectedFile.getFileName().toString(), 70));
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════╣");
        
        try {
            String content = Files.readString(selectedFile);
            
            // Show first 2000 characters with line wrapping
            String preview = content.length() > 2000 ? content.substring(0, 2000) : content;
            String[] lines = preview.split("\n");
            
            for (String line : lines) {
                if (line.length() > 76) {
                    // Truncate long lines (especially Base64 data)
                    if (line.contains("encryptedData")) {
                        System.out.printf("║  %s...║%n", line.substring(0, 70));
                    } else {
                        System.out.printf("║  %-76s║%n", line.substring(0, 76));
                    }
                } else {
                    System.out.printf("║  %-76s║%n", line);
                }
            }
            
            if (content.length() > 2000) {
                System.out.println("║  ...                                                                         ║");
                System.out.printf("║  [Showing first 2000 of %,d characters]                                    ║%n", 
                    content.length());
            }
            
        } catch (IOException e) {
            System.out.println("║  Error reading file: " + e.getMessage());
        }
        
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        
        pressEnterToContinue();
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Convert a hospital to JSON format
     */
    private String hospitalToJson(Hospital h, String indent) {
        StringBuilder json = new StringBuilder();
        json.append(indent).append("{\n");
        json.append(indent).append("  \"id\": \"").append(h.getId()).append("\",\n");
        json.append(indent).append("  \"name\": \"").append(escapeJson(h.getName())).append("\",\n");
        json.append(indent).append("  \"region\": \"").append(h.getRegion()).append("\",\n");
        json.append(indent).append("  \"sessionId\": \"").append(h.getSessionId() != null ? h.getSessionId() : "").append("\",\n");
        json.append(indent).append("  \"createdAt\": \"").append(h.getFormattedCreatedAt()).append("\",\n");
        json.append(indent).append("  \"departments\": [\n");
        
        Map<String, byte[]> encryptedData = h.getEncryptedDepartmentData();
        Map<String, long[]> originalData = h.getOriginalData();
        
        int deptCount = 0;
        for (String dept : encryptedData.keySet()) {
            byte[] encrypted = encryptedData.get(dept);
            long[] original = originalData.get(dept);
            
            json.append(indent).append("    {\n");
            json.append(indent).append("      \"name\": \"").append(dept).append("\",\n");
            json.append(indent).append("      \"originalValues\": [");
            for (int i = 0; i < original.length; i++) {
                json.append(original[i]);
                if (i < original.length - 1) json.append(", ");
            }
            json.append("],\n");
            json.append(indent).append("      \"encryptedData\": \"").append(Base64.getEncoder().encodeToString(encrypted)).append("\",\n");
            json.append(indent).append("      \"encryptedSize\": ").append(encrypted.length).append("\n");
            json.append(indent).append("    }");
            
            deptCount++;
            if (deptCount < encryptedData.size()) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append(indent).append("  ]\n");
        json.append(indent).append("}");
        
        return json.toString();
    }
    
    /**
     * Parse JSON and load hospitals
     */
    private int parseAndLoadJson(String content) {
        int loaded = 0;
        
        // Simple JSON parsing (for demo - in production use a JSON library)
        // Look for hospital blocks
        
        // Check if it's a single hospital or multiple
        if (content.contains("\"hospitals\":")) {
            // Multiple hospitals
            String[] hospitalBlocks = content.split("\"id\":");
            for (int i = 1; i < hospitalBlocks.length; i++) {
                if (loadHospitalFromBlock(hospitalBlocks[i])) {
                    loaded++;
                }
            }
        } else if (content.contains("\"hospital\":")) {
            // Single hospital
            String[] hospitalBlocks = content.split("\"id\":");
            if (hospitalBlocks.length > 1 && loadHospitalFromBlock(hospitalBlocks[1])) {
                loaded++;
            }
        }
        
        return loaded;
    }
    
    /**
     * Load a hospital from a JSON block
     */
    private boolean loadHospitalFromBlock(String block) {
        try {
            // Extract fields using simple string parsing
            String id = extractJsonString(block, "");  // id is at start
            if (id.isEmpty()) {
                id = extractFirstQuotedString(block);
            }
            String name = extractJsonString(block, "name");
            String region = extractJsonString(block, "region");
            String sessionId = extractJsonString(block, "sessionId");
            
            if (id.isEmpty() || name.isEmpty()) {
                return false;
            }
            
            // Check if hospital already exists
            if (hospitalManager.hospitalExists(id)) {
                System.out.printf("   ⚠️  Hospital %s already exists, skipping...%n", id);
                return false;
            }
            
            // Create hospital (note: session won't work after reload without key regeneration)
            Hospital hospital = hospitalManager.createHospitalWithId(id, name, region, sessionId);
            
            // Load department data
            String[] deptBlocks = block.split("\"name\":");
            for (int i = 2; i < deptBlocks.length; i++) { // Skip hospital name
                String deptBlock = deptBlocks[i];
                String deptName = extractFirstQuotedString(deptBlock);
                
                if (deptName.isEmpty() || deptName.equals(name)) continue;
                
                // Extract original values
                String valuesStr = extractJsonArray(deptBlock, "originalValues");
                long[] values = parseValuesArray(valuesStr);
                
                // Extract encrypted data
                String base64Data = extractJsonString(deptBlock, "encryptedData");
                if (!base64Data.isEmpty() && values.length > 0) {
                    byte[] encrypted = Base64.getDecoder().decode(base64Data);
                    hospital.addDepartmentData(deptName, encrypted, values);
                }
            }
            
            System.out.printf("   ✓ Loaded: %s - %s (%d departments)%n", 
                id, name, hospital.getEncryptedDepartmentData().size());
            return true;
            
        } catch (Exception e) {
            System.out.println("   ⚠️  Error parsing hospital block: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract a JSON string value
     */
    private String extractJsonString(String json, String key) {
        String searchFor = key.isEmpty() ? "\"" : "\"" + key + "\": \"";
        int start = json.indexOf(searchFor);
        if (start == -1) return "";
        
        start += searchFor.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        
        return json.substring(start, end);
    }
    
    /**
     * Extract first quoted string
     */
    private String extractFirstQuotedString(String json) {
        int start = json.indexOf("\"");
        if (start == -1) return "";
        start++;
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }
    
    /**
     * Extract a JSON array
     */
    private String extractJsonArray(String json, String key) {
        String searchFor = "\"" + key + "\": [";
        int start = json.indexOf(searchFor);
        if (start == -1) return "";
        
        start += searchFor.length();
        int end = json.indexOf("]", start);
        if (end == -1) return "";
        
        return json.substring(start, end);
    }
    
    /**
     * Parse a values array string
     */
    private long[] parseValuesArray(String arrayStr) {
        if (arrayStr.isEmpty()) return new long[0];
        
        String[] parts = arrayStr.split(",");
        long[] values = new long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                values[i] = Long.parseLong(parts[i].trim());
            } catch (NumberFormatException e) {
                values[i] = 0;
            }
        }
        return values;
    }
    
    /**
     * Escape special characters for JSON
     */
    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    /**
     * Truncate a string
     */
    private String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }
    
    /**
     * Ensure save directory exists
     */
    private void ensureSaveDirectory() {
        try {
            Files.createDirectories(Paths.get(SAVE_DIR));
        } catch (IOException e) {
            System.out.println("   ⚠️  Could not create save directory: " + e.getMessage());
        }
    }
    
    /**
     * List all JSON files in save directory
     */
    private List<Path> listJsonFiles() {
        List<Path> files = new ArrayList<>();
        try {
            Files.list(Paths.get(SAVE_DIR))
                .filter(p -> p.toString().endsWith(".json"))
                .sorted()
                .forEach(files::add);
        } catch (IOException e) {
            // Directory might not exist yet
        }
        return files;
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
                System.out.printf("   ❌ Please enter a number between %d and %d: ", min, max);
            } catch (NumberFormatException e) {
                System.out.print("   ❌ Invalid input. Please enter a number: ");
            }
        }
    }
    
    private void pressEnterToContinue() {
        System.out.print("\n   Press Enter to continue...");
        scanner.nextLine();
    }
}
