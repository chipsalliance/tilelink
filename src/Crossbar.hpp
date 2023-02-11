#ifndef __CROSSBAR__
#define __CROSSBAR__

#include <limits>
#include <optional>
#include <string_view>

#include "Link.hpp"
#include "sparta/simulation/Unit.hpp"
#include "sparta/ports/DataPort.hpp"
#include "sparta/ports/SignalPort.hpp"
#include "sparta/simulation/ParameterSet.hpp"

struct SinkBundle;
struct SourceBundle;

class Crossbar : sparta::Unit {
  friend SinkBundle;
  friend SourceBundle;

  class Parameters : public sparta::ParameterSet {
  public:
    PARAMETER(uint32_t, sources, 1, "Number of sources (input slaves)")
    PARAMETER(uint32_t, sinks, 1, "Number of sinks (output masters)")
    // TODO: Arbitrary address width
    typedef std::vector<std::pair<uint64_t, uint64_t>> AddrRanges;
    typedef std::vector<std::vector<bool>> ConnectivityMatrix;
    typedef std::vector<std::size_t> IDSize;
    PARAMETER(AddrRanges, ranges, {}, "Ranges of addresses for each sink")
    PARAMETER(ConnectivityMatrix, connectivity, {}, "Connectivity matrix, sink -> source (input -> output)")
    PARAMETER(IDSize, downstream_sizes, {}, "ID sizes for each downstream links (source)")
    PARAMETER(IDSize, upstream_sizes, {}, "ID sizes for each upstream links (sink)")
  };

  Crossbar(sparta::TreeNode *node, const Parameters *params, const std::string &name);
private:
  const Parameters *params_;
  std::vector<std::unique_ptr<SinkBundle>> sinks_;
  std::vector<std::unique_ptr<SourceBundle>> sources_;
};

#endif // __CROSSBAR__
