#include "Slave.hpp"
#include "TLMsg.hpp"
#include "GlobalLogger.hpp"
#include <iostream>
#include <limits>

const char *Slave::name = "slave";

Slave::Slave(sparta::TreeNode *node, const Parameters *params)
  : sparta::Unit(node, name)
  , port(std::make_unique<TLBundleSink<>>(node, "port", "Slave port"))
  , dist_d(std::numeric_limits<uint64_t>::min(), std::numeric_limits<uint64_t>::max())
  , params(params)
{
  std::mt19937 gen(params->seed);
  gen_d = RandGen(gen());

  port->d.accept.registerConsumerHandler(CREATE_SPARTA_HANDLER(Slave, accept_d));
  port->a.data.registerConsumerHandler(CREATE_SPARTA_HANDLER_WITH_DATA(Slave, data_a, TLABMsg<>));

  params->id.ignore();
}

void Slave::accept_d() {
  GlobalLogger::put(std::string("slave_") + std::to_string(params->id) +
                        ".d.accepted",
                    std::to_string(this->getClock()->currentCycle()));
  sparta_assert(inflight.has_value(), "Received accept when there is no outstanding request");
  inflight->remaining--;
  if(inflight->remaining == 0) {
    inflight.reset();
    if(pending_a.has_value()) this->sched_req();
  } else 
    next_d.schedule();
}

void Slave::send_d() {
  sparta_assert(inflight.has_value(), "No request to send data to");

  auto data = dist_d(gen_d);

  // TODO: write requests
  TLDMsg<> msg = {
    .source = inflight->source,
    .sink = 0,

    .op = {
      .code = TLOpCode::AccessAckData,
      .param = 0,
    },

    .size = inflight->size,
    .data = data,

    .denied = false, // TODO: check address
    .corrupt = false,
  };

  TimedEvent<TLDMsg<>> ev {
    .at = this->getClock()->currentCycle(),
    .event = msg,
  };
  GlobalLogger::put_json(
      std::string("slave_") + std::to_string(params->id) + ".d.propose", ev);
  port->d.data.send(msg);
}

void Slave::data_a(const TLABMsg<> &msg) {
  TimedEvent<TLABMsg<>> ev {
    .at = this->getClock()->currentCycle(),
    .event = msg,
  };
  GlobalLogger::put_json(
      std::string("slave_") + std::to_string(params->id) + ".a.proposed", ev);
  sparta_assert(!pending_a.has_value(), "Received A when there is already a pending A");
  pending_a = msg;
  if(!this->inflight.has_value()) this->sched_req();
}

void Slave::sched_req() {
  sparta_assert(pending_a.has_value(), "No pending A to schedule");
  sparta_assert(!inflight.has_value(), "Already have an inflight request");

  inflight = {
    .source = pending_a->source,
    .size = pending_a->size,
    .remaining = pending_a->get_response_beats(),
  };

  pending_a.reset();
  GlobalLogger::put(std::string("slave_") + std::to_string(params->id) +
                        ".a.accept",
                    std::to_string(this->getClock()->currentCycle()));
  port->a.accept.send();
  next_d.schedule();
}