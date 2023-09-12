#ifndef __LINK__
#define __LINK__

#include <limits>
#include <sparta/ports/PortSet.hpp>
#include <string_view>

#include "sparta/simulation/Unit.hpp"
#include "sparta/ports/DataPort.hpp"
#include "sparta/ports/SignalPort.hpp"
#include "sparta/simulation/ParameterSet.hpp"

#include "TLMsg.hpp"

template<HasSize Payload>
struct TLChannelSource;
template<typename Types = DefaultTypes>
struct TLBundleSource;

template<HasSize Payload>
struct TLChannelSink : sparta::TreeNode {
  using sparta::TreeNode::TreeNode;
  sparta::PortSet pset{ this, "Ports" };
  sparta::DataInPort<Payload> data{ &pset, "data" };
  sparta::SignalOutPort accept{ &pset, "accept" };

  void bind(TLChannelSource<Payload> &ano) {
    ano.bind(*this);
  }
};

template<HasSize Payload>
struct TLChannelSource : sparta::TreeNode {
  using sparta::TreeNode::TreeNode;
  sparta::PortSet pset{ this, "Ports" };
  sparta::DataOutPort<Payload> data{ &pset, "data" };
  sparta::SignalInPort accept{ &pset, "accept" };

  void bind(TLChannelSink<Payload> &ano) {
    sparta::bind(data, ano.data);
    sparta::bind(accept, ano.accept);
  }
};

template<typename Types = DefaultTypes>
struct TLBundleSink : sparta::TreeNode {
  using sparta::TreeNode::TreeNode;
  TLChannelSink<TLABMsg<Types>> a{ this, "a", "A channel" };
  TLChannelSource<TLABMsg<Types>> b{ this, "b", "B channel" };
  TLChannelSink<TLCMsg<Types>> c{ this, "c", "C channel" };
  TLChannelSource<TLDMsg<Types>> d{ this, "d", "D channel" };
  TLChannelSink<TLEMsg<Types>> e{ this, "e", "E channel" };

  void bind(TLBundleSource<Types> &ano) {
    ano.bind(*this);
  }
};

template<typename Types>
struct TLBundleSource : sparta::TreeNode {
  using sparta::TreeNode::TreeNode;
  TLChannelSource<TLABMsg<Types>> a{ this, "a", "A channel" };
  TLChannelSink<TLABMsg<Types>> b{ this, "b", "B channel" };
  TLChannelSource<TLCMsg<Types>> c{ this, "c", "C channel" };
  TLChannelSink<TLDMsg<Types>> d{ this, "d", "D channel" };
  TLChannelSource<TLEMsg<Types>> e{ this, "e", "E channel" };

  void bind(TLBundleSink<Types> &ano) {
    a.bind(ano.a);
    b.bind(ano.b);
    c.bind(ano.c);
    d.bind(ano.d);
    e.bind(ano.e);
  }
};

#endif // __LINK__