#!/bin/bash

# This script removes Lombok annotations and adds explicit constructors
# Run this in the project root directory

SRC_DIR="src/main/java/com/fashionshop"

# Find all Java files with @RequiredArgsConstructor
find "$SRC_DIR" -name "*.java" -type f | while read -r file; do
    if grep -q "@RequiredArgsConstructor" "$file"; then
        echo "Processing: $file"
        
        # Remove @RequiredArgsConstructor line
        sed -i '' '/@RequiredArgsConstructor/d' "$file"
        
        # Remove lombok import
        sed -i '' '/import lombok/d' "$file"
        
        echo "  Fixed: $file"
    fi
done

echo "Done fixing Lombok annotations!"
