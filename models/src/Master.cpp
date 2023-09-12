#include <iostream>
#include <string>

#include "GlobalLogger.hpp"
#include "Master.hpp"
#include "TLMsg.hpp"

const char *Master::name = "master";

Master::Master(sparta::TreeNode *node, const Parameters *params)
  : sparta::Unit(node, name)
  , port(std::make_unique<TLBundleSource<>>(node, "port", "Master port"))
  , dist_a_downstream(0, params->downstreams.getValue().size() - 1)
  , params(params)
{
  std::mt19937 gen(params->seed);
  gen_a = RandGen(gen());

  dist_a_addrs.reserve(params->downstreams.getValue().size());
  for(const auto &downstream : params->downstreams.getValue())
    dist_a_addrs.emplace_back(downstream[0], downstream[1] - 1);

  port->a.accept.registerConsumerHandler(CREATE_SPARTA_HANDLER(Master, accept_a));
  port->d.data.registerConsumerHandler(CREATE_SPARTA_HANDLER_WITH_DATA(Master, data_d, TLDMsg<>));

  params->id.ignore();

  // Kickstart
  sparta::StartupEvent(node, CREATE_SPARTA_HANDLER(Master, send_a));
}

void Master::accept_a() {

  GlobalLogger::put(std::string("master_") + std::to_string(params->id) +
                        ".a.accepted",
                    std::to_string(this->getClock()->currentCycle()));
  next_a.schedule();
}

void Master::send_a() {
  auto downstream = dist_a_downstream(gen_a);
  auto addr = dist_a_addrs[downstream](gen_a);
  auto size = dist_a_size(gen_a);

  // TODO: write requests
  TLABMsg<> msg = {
    .source = 0, // Only 0 in this source

    .op = {
      .code = TLOpCode::Get,
      .param = 0,
    },

    .addr = addr,
    .size = size,

    .data = 0,
    .mask = 0,

    .corrupt = false,
  };

  TimedEvent<TLABMsg<>> ev {
    .at = this->getClock()->currentCycle(),
    .event = msg,
  };
  GlobalLogger::put_json(std::string("master_") + std::to_string(params->id) + ".a.propose", ev);
  port->a.data.send(msg);
}

void Master::data_d(const TLDMsg<> &msg) {
  TimedEvent<TLDMsg<>> ev {
    .at = this->getClock()->currentCycle(),
    .event = msg,
  };
  GlobalLogger::put_json(std::string("master_") + std::to_string(params->id) + ".d.proposed", ev);
  next_d.schedule();
}

void Master::grant_d() {
  GlobalLogger::put(std::string("master_") + std::to_string(params->id) +
                        ".d.accept",
                    std::to_string(this->getClock()->currentCycle()));
  port->d.accept.send();
}