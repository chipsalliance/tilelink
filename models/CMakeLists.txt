cmake_minimum_required(VERSION 3.25)

set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

project(tlmodel)
include(./cmake/sparta-config.cmake)

macro(sparta_application build_target)
endmacro(sparta_application)

find_package(nlohmann_json 3.2.0 REQUIRED)


file(GLOB_RECURSE SRCS ${CMAKE_SOURCE_DIR}/src/*.cpp ${CMAKE_SOURCE_DIR}/src/*.h ${CMAKE_SOURCE_DIR}/src/*.hpp)
add_executable(tlmodel ${SRCS})
set_property(TARGET tlmodel PROPERTY CXX_STANDARD 23)
target_include_directories(tlmodel PRIVATE ${CMAKE_SOURCE_DIR}/src)
target_link_libraries(tlmodel PRIVATE ${Sparta_LIBS})
target_link_libraries(tlmodel PRIVATE nlohmann_json::nlohmann_json)

#
# Testing
#
# include (../../test/TestingMacros.cmake)
# sparta_copy(sparta_skeleton "test_final_cfg.sh")
# sparta_copy(sparta_skeleton "test_arch_with_override.sh")
