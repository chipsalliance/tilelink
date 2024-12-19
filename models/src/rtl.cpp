#include <cstdint>
#include <iostream>
#include <limits>
#include <random>

#include "verilated.h"
#include "rtl.h"
#include "verilated_fst_c.h"

#include "TLMsg.hpp"

#define GEN_IMPL_MASTER_A(IDX) \
void feedMasterA_## IDX (rtl &model, const TLABMsg<> &msg) { \
  model.masterLinksIO_ ## IDX ## _a_bits_source = msg.source; \
  model.masterLinksIO_ ## IDX ## _a_bits_address = msg.addr; \
  model.masterLinksIO_ ## IDX ## _a_bits_corrupt = msg.corrupt; \
  model.masterLinksIO_ ## IDX ## _a_bits_data = msg.data; \
  model.masterLinksIO_ ## IDX ## _a_bits_mask = msg.mask; \
  model.masterLinksIO_ ## IDX ## _a_bits_opcode = tl_opcode_to_int(msg.op.code); \
  model.masterLinksIO_ ## IDX ## _a_bits_size = msg.size; \
  model.masterLinksIO_ ## IDX ## _a_bits_param = msg.op.param; \
  model.masterLinksIO_ ## IDX ## _a_valid = true; \
} \
template<typename Gen> \
void idleMasterA_ ## IDX (rtl &model, Gen gen) { \
  std::uniform_int_distribution<uint64_t> dist(std::numeric_limits<uint64_t>::min(), std::numeric_limits<uint64_t>::max()); \
  model.masterLinksIO_ ## IDX ## _a_bits_source = dist(gen); \
  model.masterLinksIO_ ## IDX ## _a_bits_address = dist(gen); \
  model.masterLinksIO_ ## IDX ## _a_bits_corrupt = dist(gen); \
  model.masterLinksIO_ ## IDX ## _a_bits_data = dist(gen); \
  model.masterLinksIO_ ## IDX ## _a_bits_mask = dist(gen); \
  model.masterLinksIO_ ## IDX ## _a_bits_opcode = dist(gen); \
  model.masterLinksIO_ ## IDX ## _a_bits_size = dist(gen); \
  model.masterLinksIO_ ## IDX ## _a_bits_param = dist(gen); \
  model.masterLinksIO_ ## IDX ## _a_valid = false; \
}

#define GEN_IMPL_SLAVE_B(IDX) \
void feedSlaveB_## IDX (rtl &model, const TLABMsg<> &msg) { \
  model.slaveLinksIO_ ## IDX ## _b_bits_address = msg.addr; \
  model.slaveLinksIO_ ## IDX ## _b_bits_corrupt = msg.corrupt; \
  model.slaveLinksIO_ ## IDX ## _b_bits_data = msg.data; \
  model.slaveLinksIO_ ## IDX ## _b_bits_mask = msg.mask; \
  model.slaveLinksIO_ ## IDX ## _b_bits_opcode = tl_opcode_to_int(msg.op.code); \
  model.slaveLinksIO_ ## IDX ## _b_bits_size = msg.size; \
  model.slaveLinksIO_ ## IDX ## _b_bits_param = msg.op.param; \
  model.slaveLinksIO_ ## IDX ## _b_valid = true; \
} \
template<typename Gen> \
void idleSlaveB_ ## IDX (rtl &model, Gen gen) { \
  std::uniform_int_distribution<uint64_t> dist(std::numeric_limits<uint64_t>::min(), std::numeric_limits<uint64_t>::max()); \
  model.slaveLinksIO_ ## IDX ## _b_bits_address = dist(gen); \
  model.slaveLinksIO_ ## IDX ## _b_bits_corrupt = dist(gen); \
  model.slaveLinksIO_ ## IDX ## _b_bits_data = dist(gen); \
  model.slaveLinksIO_ ## IDX ## _b_bits_mask = dist(gen); \
  model.slaveLinksIO_ ## IDX ## _b_bits_opcode = dist(gen); \
  model.slaveLinksIO_ ## IDX ## _b_bits_size = dist(gen); \
  model.slaveLinksIO_ ## IDX ## _b_bits_param = dist(gen); \
  model.slaveLinksIO_ ## IDX ## _b_valid = false; \
}

#define GEN_IMPL_MASTER_C(IDX) \
void feedMasterC_## IDX (rtl &model, const TLCMsg<> &msg) { \
  model.masterLinksIO_ ## IDX ## _c_bits_address = msg.addr; \
  model.masterLinksIO_ ## IDX ## _c_bits_corrupt = msg.corrupt; \
  model.masterLinksIO_ ## IDX ## _c_bits_data = msg.data; \
  model.masterLinksIO_ ## IDX ## _c_bits_source = msg.source; \
  model.masterLinksIO_ ## IDX ## _c_bits_opcode = tl_opcode_to_int(msg.op.code); \
  model.masterLinksIO_ ## IDX ## _c_bits_size = msg.size; \
  model.masterLinksIO_ ## IDX ## _c_bits_param = msg.op.param; \
  model.masterLinksIO_ ## IDX ## _c_valid = true; \
} \
template<typename Gen> \
void idleMasterC_ ## IDX (rtl &model, Gen gen) { \
  std::uniform_int_distribution<uint64_t> dist(std::numeric_limits<uint64_t>::min(), std::numeric_limits<uint64_t>::max()); \
  model.masterLinksIO_ ## IDX ## _c_bits_address = dist(gen); \
  model.masterLinksIO_ ## IDX ## _c_bits_corrupt = dist(gen); \
  model.masterLinksIO_ ## IDX ## _c_bits_data = dist(gen); \
  model.masterLinksIO_ ## IDX ## _c_bits_source = dist(gen); \
  model.masterLinksIO_ ## IDX ## _c_bits_opcode = dist(gen); \
  model.masterLinksIO_ ## IDX ## _c_bits_param = dist(gen); \
  model.masterLinksIO_ ## IDX ## _c_valid = false; \
}

#define GEN_IMPL_SLAVE_D(IDX) \
void feedSlaveD_## IDX (rtl &model, const TLDMsg<> &msg) { \
  model.slaveLinksIO_ ## IDX ## _d_bits_sink = msg.sink; \
  model.slaveLinksIO_ ## IDX ## _d_bits_corrupt = msg.corrupt; \
  model.slaveLinksIO_ ## IDX ## _d_bits_data = msg.data; \
  model.slaveLinksIO_ ## IDX ## _d_bits_source = msg.source; \
  model.slaveLinksIO_ ## IDX ## _d_bits_opcode = tl_opcode_to_int(msg.op.code); \
  model.slaveLinksIO_ ## IDX ## _d_bits_size = msg.size; \
  model.slaveLinksIO_ ## IDX ## _d_bits_param = msg.op.param; \
  model.slaveLinksIO_ ## IDX ## _d_bits_denied = msg.denied; \
  model.slaveLinksIO_ ## IDX ## _d_valid = true; \
} \
template<typename Gen> \
void idleSlaveD_ ## IDX (rtl &model, Gen gen) { \
  std::uniform_int_distribution<uint64_t> dist(std::numeric_limits<uint64_t>::min(), std::numeric_limits<uint64_t>::max()); \
  model.slaveLinksIO_ ## IDX ## _d_bits_sink = dist(gen); \
  model.slaveLinksIO_ ## IDX ## _d_bits_corrupt = dist(gen); \
  model.slaveLinksIO_ ## IDX ## _d_bits_data = dist(gen); \
  model.slaveLinksIO_ ## IDX ## _d_bits_source = dist(gen); \
  model.slaveLinksIO_ ## IDX ## _d_bits_opcode = dist(gen); \
  model.slaveLinksIO_ ## IDX ## _d_bits_param = dist(gen); \
  model.slaveLinksIO_ ## IDX ## _d_bits_denied = dist(gen); \
  model.slaveLinksIO_ ## IDX ## _d_valid = false; \
}

#define GEN_IMPL_MASTER_E(IDX) \
void feedMasterE_## IDX (rtl &model, const TLEMsg<> &msg) { \
  model.masterLinksIO_ ## IDX ## _e_bits_sink = msg.sink; \
  model.masterLinksIO_ ## IDX ## _e_valid = true; \
} \
template<typename Gen> \
void idleMasterE_ ## IDX (rtl &model, Gen gen) { \
  std::uniform_int_distribution<uint64_t> dist(std::numeric_limits<uint64_t>::min(), std::numeric_limits<uint64_t>::max()); \
  model.masterLinksIO_ ## IDX ## _e_bits_sink = dist(gen); \
  model.masterLinksIO_ ## IDX ## _e_valid = false; \
}

GEN_IMPL_MASTER_A(0)
GEN_IMPL_MASTER_A(1)
GEN_IMPL_SLAVE_B(0)
GEN_IMPL_SLAVE_B(1)
GEN_IMPL_MASTER_C(0)
GEN_IMPL_MASTER_C(1)
GEN_IMPL_SLAVE_D(0)
GEN_IMPL_SLAVE_D(1)
GEN_IMPL_MASTER_E(0)
GEN_IMPL_MASTER_E(1)

#define GEN_IMPL_SLAVE_A(IDX) \
bool cmpSlaveA_## IDX (rtl &model, const TLABMsg<> &msg) { \
  return \
    model.slaveLinksIO_ ## IDX ## _a_bits_source == msg.source && \
    model.slaveLinksIO_ ## IDX ## _a_bits_address == msg.addr && \
    model.slaveLinksIO_ ## IDX ## _a_bits_corrupt == msg.corrupt && \
    model.slaveLinksIO_ ## IDX ## _a_bits_data == msg.data && \
    model.slaveLinksIO_ ## IDX ## _a_bits_mask == msg.mask && \
    model.slaveLinksIO_ ## IDX ## _a_bits_opcode == tl_opcode_to_int(msg.op.code) && \
    model.slaveLinksIO_ ## IDX ## _a_bits_size == msg.size && \
    model.slaveLinksIO_ ## IDX ## _a_bits_param == msg.op.param; \
}

#define GEN_IMPL_MASTER_B(IDX) \
bool cmpMasterB_## IDX (rtl &model, const TLABMsg<> &msg) { \
  return \
    model.masterLinksIO_ ## IDX ## _b_bits_address == msg.addr && \
    model.masterLinksIO_ ## IDX ## _b_bits_corrupt == msg.corrupt && \
    model.masterLinksIO_ ## IDX ## _b_bits_data == msg.data && \
    model.masterLinksIO_ ## IDX ## _b_bits_mask == msg.mask && \
    model.masterLinksIO_ ## IDX ## _b_bits_opcode == tl_opcode_to_int(msg.op.code) && \
    model.masterLinksIO_ ## IDX ## _b_bits_size == msg.size && \
    model.masterLinksIO_ ## IDX ## _b_bits_param == msg.op.param; \
}

#define GEN_IMPL_SLAVE_C(IDX) \
bool cmpSlaveC_## IDX (rtl &model, const TLCMsg<> &msg) { \
  return \
    model.slaveLinksIO_ ## IDX ## _c_bits_address == msg.addr && \
    model.slaveLinksIO_ ## IDX ## _c_bits_corrupt == msg.corrupt && \
    model.slaveLinksIO_ ## IDX ## _c_bits_data == msg.data && \
    model.slaveLinksIO_ ## IDX ## _c_bits_source == msg.source && \
    model.slaveLinksIO_ ## IDX ## _c_bits_opcode == tl_opcode_to_int(msg.op.code) && \
    model.slaveLinksIO_ ## IDX ## _c_bits_size == msg.size && \
    model.slaveLinksIO_ ## IDX ## _c_bits_param == msg.op.param; \
}

#define GEN_IMPL_MASTER_D(IDX) \
bool cmpMasterD_## IDX (rtl &model, const TLDMsg<> &msg) { \
  return \
    model.masterLinksIO_ ## IDX ## _d_bits_sink == msg.sink && \
    model.masterLinksIO_ ## IDX ## _d_bits_corrupt == msg.corrupt && \
    model.masterLinksIO_ ## IDX ## _d_bits_data == msg.data && \
    model.masterLinksIO_ ## IDX ## _d_bits_source == msg.source && \
    model.masterLinksIO_ ## IDX ## _d_bits_opcode == tl_opcode_to_int(msg.op.code) && \
    model.masterLinksIO_ ## IDX ## _d_bits_size == msg.size && \
    model.masterLinksIO_ ## IDX ## _d_bits_param == msg.op.param && \
    model.masterLinksIO_ ## IDX ## _d_bits_denied == msg.denied; \
}

#define GEN_IMPL_SLAVE_E(IDX) \
bool cmpSlaveE_## IDX (rtl &model, const TLEMsg<> &msg) { \
  return model.slaveLinksIO_ ## IDX ## _e_bits_sink == msg.sink; \
}

GEN_IMPL_SLAVE_A(0)
GEN_IMPL_SLAVE_A(1)
GEN_IMPL_MASTER_B(0)
GEN_IMPL_MASTER_B(1)
GEN_IMPL_SLAVE_C(0)
GEN_IMPL_SLAVE_C(1)
GEN_IMPL_MASTER_D(0)
GEN_IMPL_MASTER_D(1)
GEN_IMPL_SLAVE_E(0)
GEN_IMPL_SLAVE_E(1)

int main() {
  rtl model;

  Verilated::traceEverOn(true);
  VerilatedFstC tracer;
  model.trace(&tracer, 128);
  tracer.open("wave.fst");

  model.reset = true;
  uint64_t clk_double = 0;

  while(true) {
    model.reset = clk_double < 10;
    model.clock = clk_double % 2;

    if(clk_double >= 10) {
      uint64_t tick = (clk_double - 10) / 2;
      if(clk_double % 2 == 0) {
        // Is dropping edge, feed data
      } else {
        // Is raising edge, check data
      }
    }

    model.eval();
    tracer.dump(clk_double);
    ++clk_double;
  }

  tracer.close();
}