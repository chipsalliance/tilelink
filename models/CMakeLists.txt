cmake_minimum_required(VERSION 3.25)
project(tlmodel)

set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

include(./cmake/sparta-config.cmake)

macro(sparta_application build_target)
endmacro(sparta_application)

find_package(nlohmann_json 3.2.0 REQUIRED)
find_package(verilator
  HINTS $ENV{VERILATOR_ROOT}
)

add_executable(tlmodel
  ${CMAKE_SOURCE_DIR}/src/Crossbar.cpp
  ${CMAKE_SOURCE_DIR}/src/GlobalLogger.cpp
  ${CMAKE_SOURCE_DIR}/src/main.cpp
  ${CMAKE_SOURCE_DIR}/src/Master.cpp
  ${CMAKE_SOURCE_DIR}/src/Simulator.cpp
  ${CMAKE_SOURCE_DIR}/src/Slave.cpp
)
set_property(TARGET tlmodel PROPERTY CXX_STANDARD 23)
target_include_directories(tlmodel PRIVATE ${CMAKE_SOURCE_DIR}/src)
target_include_directories(tlmodel SYSTEM PUBLIC /usr/local/include)
target_link_libraries(tlmodel PRIVATE ${Sparta_LIBS})
target_link_libraries(tlmodel PRIVATE nlohmann_json::nlohmann_json)

add_executable(rtlsim
  ${CMAKE_SOURCE_DIR}/src/rtl.cpp
)
set_property(TARGET rtlsim PROPERTY CXX_STANDARD 23)
target_include_directories(rtlsim PRIVATE ${CMAKE_SOURCE_DIR}/src)
target_include_directories(rtlsim SYSTEM PUBLIC /usr/local/include)
target_link_libraries(rtlsim PRIVATE nlohmann_json::nlohmann_json)

set(RTL_SRC ${CMAKE_CURRENT_SOURCE_DIR}/../out/tests/mfccompile/compile.dest/concat.sv)

verilate(
  rtlsim
  SOURCES ${RTL_SRC}
  PREFIX rtl
  TRACE_FST
  DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/verilated
  VERILATOR_ARGS --trace-params --trace-structs --top-module TLCrossBar
)
