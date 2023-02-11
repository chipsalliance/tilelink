#ifndef __ARBITER__
#define __ARBITER__

#include <limits>
#include <optional>
#include <string_view>

#include "Link.hpp"
#include "sparta/simulation/Unit.hpp"
#include "sparta/ports/DataPort.hpp"
#include "sparta/ports/SignalPort.hpp"
#include "sparta/simulation/ParameterSet.hpp"

template<HasSize Payload>
class LockingArbiter : sparta::Unit {
public:
  class Parameters : public sparta::ParameterSet {
  public:
    PARAMETER(uint32_t, sources, 1, "Number of sources (inputs)")
    Parameters(uint32_t src) {
      sources = src;
    }
  };

  LockingArbiter(sparta::TreeNode *node, const Parameters *params, const std::string &name) :
    sparta::Unit(node, name),
    params_(params),
    pendings(params_->sinks)
  {
    sink_notifiers_.reserve(params_->sinks);
    sinks_.reserve(params_->sinks);

    for(uint32_t i = 0; i < params_->sinks; ++i) sink_notifiers_.emplace_back(this, i);
    for(uint32_t i = 0; i < params_->sinks; ++i) {
      auto sink = make_unique<TLChannelSink<Payload>>(&unit_port_set_, "sink_" + std::to_string(i));
      sink.data.registerConsumerHandler(CREATE_SPARTA_HANDLER_WITH_DATA_WITH_OBJ(Notifier, &sink_notifiers_[i], on_data, Payload));
      sinks_.emplace_back(std::move(sink));
    }

    source_.accept.registerConsumerHandler(CREATE_SPARTA_HANDLER(LockingArbiter, on_accept));
  }
private:
  // Manually do some lambda stuff, because Sparta doesn't support that
  class Notifier {
    size_t idx;
    LockingArbiter<Payload> *arbiter;

    void on_data(const Payload &payload) {
      sparta_assert(!arbiter->pendings[idx].has_value(), "Sink " << idx << " has unhandled data");
      arbiter->pendings[idx] = { payload };
      arbiter->next.schedule();
    }
  };

  std::vector<Notifier> sink_notifiers_;

  TLChannelSource<Payload> source_{&unit_port_set_, "source"};
  std::vector<std::unique_ptr<TLChannelSink<Payload>>> sinks_;
  const Parameters *params_;

  std::size_t locked = 0;
  std::size_t remaining = 0;

  std::vector<std::optional<Payload>> pendings;
  sparta::Event<> next{&unit_event_set_, "next", CREATE_SPARTA_HANDLER(LockingArbiter, next)};

  void on_accept() {
    sparta_assert(remaining == 0, "Arbiter is idle");
    sparta_assert(pendings[locked].has_value(), "Sink " << locked << " has no data");

    pendings[locked].reset();
    --remaining;

    // Send accept to upstream
    sinks_[locked]->accept.send();

    // check if there is still pending data
    next.schedule();
  }

  void schedule() {
    if(remaining != 0 && pendings[locked].has_value())
      source_.data.send(pendings[locked].value());
    else {
      // Find next value
      for(size_t probe = (locked + 1) % params_->sinks;; probe = (probe + 1) % params_->sinks) {
        if(pendings[probe].has_value()) {
          locked = probe;
          remaining = pendings[probe].value().get_size();
          source_.data.send(pendings[probe].value());
          return;
        }

        if(probe == locked) break; // No data available
      }
    }
  }
};

#endif // __ARBITER__