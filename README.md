# EDI Service

This is a Java-based EDI (Electronic Data Interchange) service application.

## Project Structure

```
edi-service/
├── pom.xml                 # Maven project configuration
├── README.md              # Project documentation
├── run.ps1               # PowerShell script to run the application
└── src/
    ├── main/             # Application source code
    │   └── java/
    │       └── com/
    │           └── edi/
    │               └── service/
    │                   ├── App.java
    │                   ├── MrnInfo.java
    │                   └── PasswordCallback.java
    └── test/            # Test source code
        └── java/
            └── com/
                └── edi/
                    └── service/
                        └── AppTest.java
```

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher

## Building the Project

To build the project, run:

```bash
mvn clean install
```

## Running Tests

To run the tests:

```bash
mvn test
```

## Working with WSDL and Generated Code

The project uses Apache CXF to generate Java classes from WSDL files. The generated code is placed in `target/generated-sources/cxf`.

To generate the code:

```bash
mvn generate-sources
```

After generating the code, you'll need to update the imports in `MrnInfo.java` to match the actual generated package structure.

## Running the Application

You can run the application in two ways:

1. Using PowerShell script:

```powershell
# Basic run
.\run.ps1

# Force regeneration of WSDL code
.\run.ps1 -ForceRegenerate

# Show verbose output about generated code
.\run.ps1 -Verbose

# Both options
.\run.ps1 -ForceRegenerate -Verbose
```

2. Using Maven directly:

```bash
mvn exec:java
```
