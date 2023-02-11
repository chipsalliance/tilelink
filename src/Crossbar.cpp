#include <fstream>
#include <iterator>
#include <vector>
#include <map>
#include <algorithm>

#include "Arbiter.hpp"
#include "Crossbar.hpp"
#include "TLMsg.hpp"

struct IDMapper {
  // Start, length
  std::vector<std::pair<std::size_t, std::size_t>> starts;
  std::map<std::size_t, std::size_t> inv;
  size_t upper;

  // 0 size = skip
  IDMapper(const std::vector<std::size_t> &sizes) : starts(sizes.size()) {
    std::vector<std::size_t> indices(sizes.size());
    for(std::size_t i = 0; i < sizes.size(); ++i) indices[i] = i;
    sort(indices.begin(), indices.end(), [&sizes](std::size_t a, std::size_t b) {
      return sizes[a] > sizes[b]; // Sort big to small
    });

    // Accumelate
    std::size_t last = 0;
    for(const auto &i : indices) {
      starts[i] = std::make_pair(last, sizes[i]);
      if(sizes[i] == 0) continue;
      inv.emplace(last, i);
      last += sizes[i];
    }

    upper = last;
  }

  // Returns id
  size_t project(size_t idx, size_t id) const {
    const auto &start = starts[idx];
    sparta_assert(id < start.second, "ID " << id << " is out of range for " << idx);
    return start.first + id;
  }

  // Returns (idx, id)
  std::pair<size_t, size_t> inv_project(size_t id) const {
    auto it = std::prev(inv.upper_bound(id)); // Last key <= idx, must exist because 0 in inv
    auto inv_id = id - it->first;
    sparta_assert(inv_id < starts[it->second].second, "During reverse projection, ID " << id << " -> " << inv_id << " is out of range for " << it->second);
    return std::make_pair(it->second, inv_id);
  }

  std::size_t get_upper() const { return upper; }
};

struct AddressDecoder {
  // start -> (idx, len)
  std::map<size_t, std::pair<size_t, size_t>> mapping;

  // TODO: check mutual exclusiveness
  void add(size_t idx, std::pair<size_t, size_t> range) {
    mapping.emplace(range.first, std::make_pair(idx, range.second));
  }

  std::optional<std::size_t> decode(std::size_t input) const {
    auto it = mapping.upper_bound(input);
    if(it == mapping.begin()) return {};

    it = std::prev(it);
    if(input >= it->first + it->second.second) return {};
    return it->second.first;
  }
};

struct SinkBundle {
  size_t idx;
  std::unique_ptr<TLBundleSink<>> port;

  IDMapper downstream_ids;
  AddressDecoder downstream_addrs;

  std::vector<size_t> reachable_downstream;
  std::vector<std::optional<size_t>> reachable_downstream_inv;

  Crossbar *parent;

  void data_a(TLABMsg<> msg);
  void data_c(TLCMsg<> msg);
  void data_e(TLEMsg<> msg);
};

struct SourceBundle {
  size_t idx;
  std::unique_ptr<TLBundleSource<>> port;

  std::vector<size_t> reachable_upstream;
  std::vector<std::optional<size_t>> reachable_upstream_inv;

  IDMapper upstream_ids;
  Crossbar *parent;
};


void SinkBundle::data_a(TLABMsg<> msg) {
  auto downstream = downstream_addrs.decode(msg.addr);
  sparta_assert(downstream, "Address " << msg.addr << " is not reachable from sink " << idx);

  auto remapped_source_idx = parent->sources_[downstream.value()]->upstream_ids.project(idx, msg.source);

  // TODO: How do we connect to corresponding arbiter???????????????
}

Crossbar::Crossbar(sparta::TreeNode *node, const Parameters *params, const std::string &name) :
  sparta::Unit(node, name),
  params_(params) {
    // TODO: validate parameter dimensions
    // TODO: validate ID widths

    sinks_.reserve(params_->sinks);
    sources_.reserve(params_->sources);

    // Construct bundles
    std::vector<std::size_t> source_sizes(params_->sources);
    for(std::size_t i = 0; i < params_->sinks; ++i) {
      auto port = std::make_unique<TLBundleSink<>>(node, "sink_" + std::to_string(i));
      AddressDecoder dec;

      std::vector<std::size_t> reachable_downstream;
      std::vector<std::optional<std::size_t>> reachable_downstream_inv(params_->sources);

      for(std::size_t j = 0; j < params_->sources; ++j) {
        if(params_->connectivity.getValue()[i][j]) {
          source_sizes[j] = params_->downstream_sizes.getValue()[j];
          dec.add(j, params_->ranges.getValue()[j]);
          reachable_downstream.push_back(j);
          reachable_downstream_inv[j] = reachable_downstream.size() - 1;
        } else {
          source_sizes[j] = 0;
        }
      }

      IDMapper ids(source_sizes);
      auto b_arb_params = new LockingArbiter<TLABMsg<>>::Parameters(reachable_downstream.size());
      auto d_arb_params = new LockingArbiter<TLDMsg<>>::Parameters(reachable_downstream.size());

      auto bundle = std::unique_ptr<SinkBundle>(new SinkBundle {
        .idx = i,
        .port = std::move(port),
        .downstream_ids = { source_sizes },
        .downstream_addrs = dec,
        .reachable_downstream = reachable_downstream,
        .reachable_downstream_inv = reachable_downstream_inv,
        .b_arb = LockingArbiter<TLABMsg<>>(node, b_arb_params, "sink_" + std::to_string(i) + "_b_arb"),
        .d_arb = LockingArbiter<TLDMsg<>>(node, d_arb_params, "sink_" + std::to_string(i) + "_d_arb"),
        .parent = this
      });
    }
  }