#!/bin/bash

# Consistent Hashing Demo Runner
# This script compiles and runs the various demonstrations

echo "🚀 Consistent Hashing Demo Runner"
echo "================================="

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven first."
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 11 or higher."
    exit 1
fi

echo "✅ Dependencies check passed"
echo ""

# Compile the project
echo "🔧 Compiling project..."
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo "❌ Compilation failed"
    exit 1
fi
echo "✅ Compilation successful"
echo ""

# Show menu
echo "Select what to run:"
echo "1) 📝 Interactive Demo (Shows all consistent hashing concepts)"
echo "2) 📊 Performance Benchmarks (Compare with simple modulo hashing)"  
echo "3) 🧪 Run Unit Tests (Validate implementation)"
echo "4) 📈 All Above (Complete showcase)"
echo ""

read -p "Enter your choice (1-4): " choice

case $choice in
    1)
        echo "🎯 Running Interactive Demo..."
        echo "================================"
        mvn exec:java -Dexec.mainClass="com.consistenthashing.demo.ConsistentHashingDemo" -q
        ;;
    2)
        echo "⚡ Running Performance Benchmarks..."
        echo "===================================="
        mvn exec:java -Dexec.mainClass="com.consistenthashing.benchmark.ConsistentHashingBenchmark" -q
        ;;
    3)
        echo "🔬 Running Unit Tests..."
        echo "========================"
        mvn test
        ;;
    4)
        echo "🎪 Running Complete Showcase..."
        echo "==============================="
        echo ""
        echo "📝 PART 1: Interactive Demo"
        echo "============================="
        mvn exec:java -Dexec.mainClass="com.consistenthashing.demo.ConsistentHashingDemo" -q
        echo ""
        echo "📊 PART 2: Performance Benchmarks"  
        echo "=================================="
        mvn exec:java -Dexec.mainClass="com.consistenthashing.benchmark.ConsistentHashingBenchmark" -q
        echo ""
        echo "🧪 PART 3: Unit Tests"
        echo "====================="
        mvn test
        ;;
    *)
        echo "❌ Invalid choice. Please run the script again and select 1-4."
        exit 1
        ;;
esac

echo ""
echo "🎉 Done! Thanks for exploring Consistent Hashing!"
echo "💡 Check the README.md for more details and interview tips."