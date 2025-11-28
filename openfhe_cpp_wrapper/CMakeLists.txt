cmake_minimum_required(VERSION 3.13)
project(openfhe_wrapper)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add OpenMP flags to compilation AND linking
add_compile_options(-fopenmp -pthread -fPIC)
add_link_options(-fopenmp -pthread)

# Find OpenFHE
set(OpenFHE_DIR /usr/local/lib/OpenFHE)
find_package(OpenFHE CONFIG REQUIRED)

if(OpenFHE_FOUND)
    message(STATUS "Found OpenFHE")
    message(STATUS "OpenFHE include: ${OpenFHE_INCLUDE}")
    message(STATUS "OpenFHE lib: ${OpenFHE_LIBDIR}")
else()
    message(FATAL_ERROR "OpenFHE not found!")
endif()

# Include directories - ADD CORE DIRECTORY for relative includes
include_directories(SYSTEM /usr/local/include)
include_directories(SYSTEM /usr/local/include/openfhe)
include_directories(SYSTEM /usr/local/include/openfhe/core)
include_directories(SYSTEM /usr/local/include/openfhe/pke)
include_directories(SYSTEM /usr/local/include/openfhe/binfhe)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

# Link directories
link_directories(/usr/local/lib)

# Create shared library
add_library(openfhe_wrapper SHARED
    src/openfhe_wrapper.cpp
)

# Set compile definitions to match OpenFHE build
target_compile_definitions(openfhe_wrapper PRIVATE
    MATHBACKEND=4
)

# Link OpenFHE libraries
target_link_libraries(openfhe_wrapper 
    OPENFHEpke 
    OPENFHEcore
    OPENFHEbinfhe
    gomp
    pthread
    dl
)

# Set library properties
set_target_properties(openfhe_wrapper PROPERTIES
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED ON
    VERSION 1.0
    SOVERSION 1
    POSITION_INDEPENDENT_CODE ON
)

# Install rules
install(TARGETS openfhe_wrapper
    LIBRARY DESTINATION lib
    ARCHIVE DESTINATION lib
)

install(FILES include/openfhe_wrapper.h
    DESTINATION include
)