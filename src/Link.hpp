#ifndef __LINK__
#define __LINK__

#include <limits>
#include <string_view>

#include "sparta/simulation/Unit.hpp"
#include "sparta/ports/PortSet.hpp"
#include "sparta/ports/DataPort.hpp"
#include "sparta/ports/SignalPort.hpp"
#include "sparta/simulation/ParameterSet.hpp"

#include "TLMsg.hpp"

template<HasSize Payload>
struct TLChannelSource;
template<typename Types = DefaultTypes>
struct TLBundleSource;

template<HasSize Payload>
struct TLChannelSink : sparta::PortSet {
  using sparta::PortSet::PortSet;
  sparta::DataInPort<Payload> data{ this, "data" };
  sparta::SignalOutPort accept{ this, "accept" };

  void bind(TLChannelSource<Payload> &ano) {
    ano.bind(*this);
  }
};

template<HasSize Payload>
struct TLChannelSource : sparta::PortSet {
  using sparta::PortSet::PortSet;
  sparta::DataOutPort<Payload> data{ this, "data" };
  sparta::SignalInPort accept{ this, "accept" };

  void bind(TLChannelSink<Payload> &ano) {
    sparta::bind(data, ano.data);
    sparta::bind(accept, ano.accept);
  }
};

template<typename Types = DefaultTypes>
struct TLBundleSink : sparta::PortSet {
  using sparta::PortSet::PortSet;
  TLChannelSink<TLABMsg<Types>> a{ this, "a" };
  TLChannelSource<TLABMsg<Types>> b{ this, "b" };
  TLChannelSink<TLCMsg<Types>> c{ this, "c" };
  TLChannelSource<TLDMsg<Types>> d{ this, "d" };
  TLChannelSink<TLEMsg<Types>> e{ this, "e" };

  void bind(TLBundleSource<Types> &ano) {
    ano.bind(*this);
  }
};

template<typename Types>
struct TLBundleSource : sparta::PortSet {
  using sparta::PortSet::PortSet;
  TLChannelSource<TLABMsg<Types>> a{ this, "a" };
  TLChannelSink<TLABMsg<Types>> b{ this, "b" };
  TLChannelSource<TLCMsg<Types>> c{ this, "c" };
  TLChannelSink<TLDMsg<Types>> d{ this, "d" };
  TLChannelSource<TLEMsg<Types>> e{ this, "e" };

  void bind(TLBundleSink<Types> &ano) {
    a.bind(ano.a);
    b.bind(ano.b);
    c.bind(ano.c);
    d.bind(ano.d);
    e.bind(ano.e);
  }
};

#endif // __LINK__