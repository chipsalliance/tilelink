#ifndef __ARBITER__
#define __ARBITER__

#include <limits>
#include <optional>
#include <sparta/events/Event.hpp>
#include <string_view>

#include "Link.hpp"
#include "sparta/simulation/Unit.hpp"
#include "sparta/ports/DataPort.hpp"
#include "sparta/ports/SignalPort.hpp"
#include "sparta/simulation/ParameterSet.hpp"

template<HasSize Payload>
class LockingArbiter {
public:
  LockingArbiter(size_t sink_cnt) {
    pendings.resize(sink_cnt);
  }

  void propose(size_t sink_id, Payload &&data) {
    sparta_assert(!pendings[sink_id].has_value(), "Sink " << sink_id << " has unhandled data");
    pendings[sink_id] = make_optional(data);
  }

  // Returns idx of accepted sink, and if there is new data
  size_t accept() {
    sparta_assert(!idle, "Can only accept when there is outstanding request");
    sparta_assert(remaining == 0, "Arbiter is idle");
    sparta_assert(pendings[locked].has_value(), "Sink " << locked << " has no data");

    pendings[locked].reset();
    --remaining;

    // Send accept to upstream
    return locked;
  }

  std::optional<Payload> next() {
    sparta_assert(idle, "Can only call next on idle");
    if(remaining != 0) {
      idle = !pendings[locked].has_value();
      return pendings[locked];
    } else for(size_t probe = (locked + 1) % pendings.size();; probe = (probe + 1) % pendings.size()) {
      if(pendings[probe].has_value()) {
        locked = probe;
        remaining = pendings[probe].value().get_size();
        idle = true;
        return pendings[probe];
      }

      if(probe == locked) break; // No data available
    }

    sparta_assert(idle, "Sanity check");
    return {};
  }

  bool no_outstanding() const {
    return idle;
  }

private:
  std::size_t locked = 0;
  std::size_t remaining = 0;

  std::vector<std::optional<Payload>> pendings;
  bool idle = true;
};

#endif // __ARBITER__