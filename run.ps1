# Parameters
param (
    [switch]$ForceRegenerate = $false,
    [switch]$Verbose = $false
)

# Check if Maven is installed
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Error "Maven is not installed or not in PATH. Please install Maven and try again."
    exit 1
}

# Check if Java is installed
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "Java is not installed or not in PATH. Please install Java and try again."
    exit 1
}

# Function to check if generated sources exist
function Test-GeneratedSources {
    # Check multiple possible locations
    $possiblePaths = @(
        "target/generated-sources/cxf/it/sogei/domest/infomrnfp",
        "target/generated-sources/cxf/it/sogei/domest/infomrnfp/services"
    )
    
    foreach ($path in $possiblePaths) {
        if (Test-Path $path) {
            return $true
        }
    }
    
    return $false
}

# Generate sources if they don't exist or if force regenerate is specified
if ((-not (Test-GeneratedSources)) -or $ForceRegenerate) {
    if ($ForceRegenerate) {
        Write-Host "Force regeneration enabled. Cleaning and regenerating sources..." -ForegroundColor Yellow
    } else {
        Write-Host "Generated sources not found. Running code generation..." -ForegroundColor Yellow
    }
    
    mvn clean generate-sources
    
    if (-not (Test-GeneratedSources)) {
        Write-Error "Failed to generate sources from WSDL. Please check if the WSDL file exists and is valid."
        
        # Show the actual generated structure for debugging
        Write-Host "`nListing generated directories:" -ForegroundColor Cyan
        if (Test-Path "target/generated-sources/cxf") {
            Get-ChildItem -Path "target/generated-sources/cxf" -Recurse -Directory | Select-Object FullName
        } else {
            Write-Host "No generated sources directory found." -ForegroundColor Red
        }
        
        exit 1
    }
    
    # Show the actual generated structure if verbose
    if ($Verbose) {
        Write-Host "`nGenerated sources structure:" -ForegroundColor Green
        Get-ChildItem -Path "target/generated-sources/cxf" -Recurse -Directory | Select-Object FullName
        
        # List Java files in the generated directories
        Write-Host "`nGenerated Java files:" -ForegroundColor Green
        Get-ChildItem -Path "target/generated-sources/cxf" -Recurse -Filter "*.java" | Select-Object FullName
    }
}

# Build and install the project
Write-Host "Building and installing the project..." -ForegroundColor Green
mvn clean install

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nStarting the EDI Service application..." -ForegroundColor Green
    mvn exec:java
} else {
    Write-Error "Build failed. Please fix the errors and try again."
    exit 1
} 