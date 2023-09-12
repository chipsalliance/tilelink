#ifndef __CROSSBAR__
#define __CROSSBAR__

#include <initializer_list>
#include <limits>
#include <optional>
#include <sparta/simulation/TreeNode.hpp>
#include <string_view>

#include "Link.hpp"
#include "sparta/simulation/Unit.hpp"
#include "sparta/ports/DataPort.hpp"
#include "sparta/ports/SignalPort.hpp"
#include "sparta/simulation/ParameterSet.hpp"

struct SinkBundle;
struct SourceBundle;
class Simulator;

struct AddressDecoder {
  // start -> (idx, len)
  std::map<size_t, std::pair<size_t, size_t>> mapping;
  void add(size_t idx, std::pair<size_t, size_t> range);
  std::optional<std::size_t> decode(std::size_t input) const;
};

class Crossbar : public sparta::Unit {
  friend SinkBundle;
  friend SourceBundle;
  friend Simulator;

public:
  struct Parameters : public sparta::ParameterSet {
    using sparta::ParameterSet::ParameterSet;
    PARAMETER(uint32_t, sources, 1, "Number of sources (input slaves)")
    PARAMETER(uint32_t, sinks, 1, "Number of sinks (output masters)")
    // TODO: Arbitrary address width
    typedef std::vector<std::vector<uint64_t>> AddrRanges;
    typedef std::vector<std::vector<bool>> ConnectivityMatrix;
    typedef std::vector<std::size_t> IDSize;
    sparta::Parameter<AddrRanges> ranges {"ranges", {{0, 0x100000000}}, "Ranges of addresses for each sink", __this_ps};
    PARAMETER(ConnectivityMatrix, connectivity, {{true}}, "Connectivity matrix, sink -> source (input -> output)")
    PARAMETER(IDSize, downstream_sizes, {0}, "ID width for each downstream links (source)") // TODO: rename to _widths
    PARAMETER(IDSize, upstream_sizes, {0}, "ID width for each upstream links (sink)") // TODO: rename to _widths
  };

  Crossbar(sparta::TreeNode *node, const Parameters *params);
  static const char *name;

private:
  const Parameters *params_;
  AddressDecoder routing_;
  std::vector<std::unique_ptr<SinkBundle>> sinks_;
  std::vector<std::unique_ptr<SourceBundle>> sources_;
  sparta::TreeNode *self;

  void bind_sink(std::size_t idx, TLBundleSource<> &src);
  void bind_src(std::size_t idx, TLBundleSink<> &sink);
};

#endif // __CROSSBAR__
