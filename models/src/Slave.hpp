#ifndef __SLAVE_H__
#define __SLAVE_H__

#include "sparta/simulation/Unit.hpp"
#include "sparta/ports/PortSet.hpp"
#include "sparta/ports/DataPort.hpp"
#include "sparta/ports/SignalPort.hpp"
#include "sparta/events/UniqueEvent.hpp"
#include "sparta/simulation/ParameterSet.hpp"

#include "Link.hpp"
#include "Rand.hpp"
#include <random>

class Simulator;

namespace {
  struct Inflight {
    uint64_t source;
    uint8_t size;
    size_t remaining;
  };
}

class Slave : public sparta::Unit {
  friend Simulator;
public:
  struct Parameters : sparta::ParameterSet {
    using sparta::ParameterSet::ParameterSet;
    PARAMETER(uint64_t, seed, 0x19260817, "Seed for generating data")
    PARAMETER(uint64_t, id, 0, "Slave id")
  };

  Slave(sparta::TreeNode *node, const Parameters *params);
  static const char *name;

private:
  std::unique_ptr<TLBundleSink<>> port;
  sparta::UniqueEvent<> next_d {
    &unit_event_set_, "next_d",
    CREATE_SPARTA_HANDLER(Slave, send_d),
    0 // Slaves immediately respond
  };

  const Parameters *params;

  RandGen gen_d;
  std::uniform_int_distribution<uint64_t> dist_d;

  void send_d();
  void accept_d();
  void data_a(const TLABMsg<> &msg);
  void sched_req();

  std::optional<Inflight> inflight;
  std::optional<TLABMsg<>> pending_a;
};

#endif // __SLAVE_H__