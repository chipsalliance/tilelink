#include <fstream>
#include <iterator>
#include <vector>
#include <map>
#include <algorithm>
#include <functional>

#include "Arbiter.hpp"
#include "Crossbar.hpp"
#include "TLMsg.hpp"

const char *Crossbar::name = "crossbar";
struct IDMapper {
  // Start, length
  std::vector<std::pair<std::size_t, std::size_t>> starts;
  std::map<std::size_t, std::size_t> inv;
  size_t upper;

  IDMapper(const std::vector<std::optional<std::size_t>> &sizes) : starts(sizes.size()) {
    std::vector<std::size_t> indices(sizes.size());
    for(std::size_t i = 0; i < sizes.size(); ++i) indices[i] = i;
    stable_sort(indices.begin(), indices.end(), [&sizes](std::size_t a, std::size_t b) {
      if(sizes[a].has_value() && sizes[b].has_value()) return sizes[a] > sizes[b]; // Sort big to small
      if(sizes[a].has_value()) return true; // let empty ones go to the end
      return false;
    });

    // Accumelate
    std::size_t last = 0;
    for(const auto &i : indices) {
      size_t size = 0;
      if(const auto &s = sizes[i]) size = 1 << *s;
      starts[i] = std::make_pair(last, size);
      if(size != 0) inv.emplace(last, i);
      last += size;
    }

    upper = last;
  }

  // Returns id
  size_t project(size_t idx, size_t id) const {
    const auto &start = starts[idx];
    sparta_assert(id < (1 << start.second), "ID " << id << " is out of range for " << idx);
    return start.first + id;
  }

  // Returns (idx, id)
  std::pair<size_t, size_t> inv_project(size_t id) const {
    auto it = std::prev(inv.upper_bound(id)); // Last key <= idx, must exist because 0 in inv
    auto inv_id = id - it->first;
    sparta_assert(inv_id < (1 << starts[it->second].second), "During reverse projection, ID " << id << " -> " << inv_id << " is out of range for " << it->second);
    return std::make_pair(it->second, inv_id);
  }

  std::size_t get_upper() const { return upper; }
};

void AddressDecoder::add(size_t idx, std::pair<size_t, size_t> range) {
  mapping.emplace(range.first, std::make_pair(idx, range.second));
}

std::optional<std::size_t> AddressDecoder::decode(std::size_t input) const {
  auto it = mapping.upper_bound(input);
  if(it == mapping.begin()) return {};

  it = std::prev(it);
  if(input >= it->first + it->second.second) return {};
  return it->second.first;
}

struct SinkBundle {
  std::size_t idx;
  Crossbar *parent;
  std::unique_ptr<TLBundleSink<>> port;

  std::vector<std::size_t> reachable_downstream;
  std::vector<std::optional<std::size_t>> reachable_downstream_inv;

  IDMapper downstream_ids;

  LockingArbiter<TLABMsg<>> b_arb;
  LockingArbiter<TLDMsg<>> d_arb;

  sparta::UniqueEvent<> notify_b{&parent->unit_event_set_,
                                 "notify_b_" + std::to_string(idx),
                                 CREATE_SPARTA_HANDLER(SinkBundle, next_b)};
  sparta::UniqueEvent<> notify_d{&parent->unit_event_set_,
                                 "notify_d_" + std::to_string(idx),
                                 CREATE_SPARTA_HANDLER(SinkBundle, next_d)};

  SinkBundle(size_t idx, Crossbar *parent,
             std::vector<std::size_t> reachable_downstream,
             std::vector<std::optional<std::size_t>> reachable_downstream_inv,
             std::vector<std::optional<std::size_t>> source_sizes)
      : idx(idx), parent(parent), reachable_downstream(reachable_downstream),
        reachable_downstream_inv(reachable_downstream_inv),
        downstream_ids(source_sizes), b_arb(reachable_downstream.size()),
        d_arb(reachable_downstream.size()),
        port(std::make_unique<TLBundleSink<>>(
            parent->self, "sink_" + std::to_string(idx),
            "Sink at " + std::to_string(idx))) {
    port->a.data.registerConsumerHandler(CREATE_SPARTA_HANDLER_WITH_DATA(
      SinkBundle, data_a, TLABMsg<>
    ));
    port->c.data.registerConsumerHandler(CREATE_SPARTA_HANDLER_WITH_DATA(
      SinkBundle, data_c, TLCMsg<>
    ));
    port->e.data.registerConsumerHandler(CREATE_SPARTA_HANDLER_WITH_DATA(
      SinkBundle, data_e, TLEMsg<>
    ));
    port->b.accept.registerConsumerHandler(CREATE_SPARTA_HANDLER(
      SinkBundle, accept_b
    ));
    port->d.accept.registerConsumerHandler(CREATE_SPARTA_HANDLER(
      SinkBundle, accept_d
    ));
  }

  void data_a(const TLABMsg<> &msg);
  void data_c(const TLCMsg<> &msg);
  void data_e(const TLEMsg<> &msg);

  void accept_b();
  void accept_d();

  void next_b();
  void next_d();
};

struct SourceBundle {
  std::size_t idx;
  Crossbar *parent;
  std::unique_ptr<TLBundleSource<>> port;

  std::vector<std::size_t> reachable_upstream;
  std::vector<std::optional<std::size_t>> reachable_upstream_inv;

  IDMapper upstream_ids;

  LockingArbiter<TLABMsg<>> a_arb;
  LockingArbiter<TLCMsg<>> c_arb;
  LockingArbiter<TLEMsg<>> e_arb;

  sparta::UniqueEvent<> notify_a{&parent->unit_event_set_,
                                 "notify_a_" + std::to_string(idx),
                                 CREATE_SPARTA_HANDLER(SourceBundle, next_a)};
  sparta::UniqueEvent<> notify_c{&parent->unit_event_set_,
                                 "notify_c_" + std::to_string(idx),
                                 CREATE_SPARTA_HANDLER(SourceBundle, next_c)};
  sparta::UniqueEvent<> notify_e{&parent->unit_event_set_,
                                 "notify_e_" + std::to_string(idx),
                                 CREATE_SPARTA_HANDLER(SourceBundle, next_e)};

  SourceBundle(size_t idx, Crossbar *parent,
               std::vector<std::size_t> reachable_upstream,
               std::vector<std::optional<std::size_t>> reachable_upstream_inv,
               std::vector<std::optional<std::size_t>> sink_sizes)
      : idx(idx), parent(parent), reachable_upstream(reachable_upstream),
        reachable_upstream_inv(reachable_upstream_inv),
        upstream_ids(sink_sizes), a_arb(reachable_upstream.size()),
        c_arb(reachable_upstream.size()), e_arb(reachable_upstream.size()),
        port(std::make_unique<TLBundleSource<>>(
            parent->self, "source_" + std::to_string(idx),
            "Source at " + std::to_string(idx))) {
    port->b.data.registerConsumerHandler(CREATE_SPARTA_HANDLER_WITH_DATA(
      SourceBundle, data_b, TLABMsg<>
    ));
    port->d.data.registerConsumerHandler(CREATE_SPARTA_HANDLER_WITH_DATA(
      SourceBundle, data_d, TLDMsg<>
    ));
    port->a.accept.registerConsumerHandler(CREATE_SPARTA_HANDLER(
      SourceBundle, accept_a
    ));
    port->c.accept.registerConsumerHandler(CREATE_SPARTA_HANDLER(
      SourceBundle, accept_c
    ));
    port->e.accept.registerConsumerHandler(CREATE_SPARTA_HANDLER(
      SourceBundle, accept_e
    ));
  }

  void data_b(const TLABMsg<> &msg);
  void data_d(const TLDMsg<> &msg);

  void accept_a();
  void accept_c();
  void accept_e();

  void next_a();
  void next_c();
  void next_e();
};


void SinkBundle::data_a(const TLABMsg<> &msg) {
  auto downstream = parent->routing_.decode(msg.addr);
  sparta_assert(downstream, "Address " << msg.addr << " not in range");
  sparta_assert(reachable_downstream_inv[*downstream], "Address " << msg.addr << " is not reachable from sink " << idx);

  auto remapped_source_idx = parent->sources_[downstream.value()]->upstream_ids.project(idx, msg.source);

  auto updated_msg = msg;
  updated_msg.source = remapped_source_idx;

  auto &source = parent->sources_[downstream.value()];
  auto number = source->reachable_upstream_inv[idx].value();
  source->a_arb.propose(number, std::move(updated_msg));
  source->next_a();
}

void SinkBundle::data_c(const TLCMsg<> &msg) {
  auto downstream = parent->routing_.decode(msg.addr);
  sparta_assert(downstream, "Address " << msg.addr << " not in range");
  sparta_assert(reachable_downstream_inv[*downstream], "Address " << msg.addr << " is not reachable from sink " << idx);

  auto remapped_source_idx = parent->sources_[downstream.value()]->upstream_ids.project(idx, msg.source);

  auto updated_msg = msg;
  updated_msg.source = remapped_source_idx;

  auto &source = parent->sources_[downstream.value()];
  auto number = source->reachable_upstream_inv[idx].value();
  source->c_arb.propose(number, std::move(updated_msg));
  source->next_c();
}

void SinkBundle::data_e(const TLEMsg<> &msg) {
  auto [downstream, downstream_sink] = downstream_ids.inv_project(msg.sink);

  auto updated_msg = msg;
  updated_msg.sink = downstream_sink;

  auto &source = parent->sources_[downstream];
  auto number = source->reachable_upstream_inv[idx].value();
  source->e_arb.propose(number, std::move(updated_msg));
  source->next_e();
}

void SinkBundle::accept_b() {
  auto downstream = b_arb.accept();
  parent->sources_[downstream]->port->b.accept.send();

  next_b();
}

void SinkBundle::accept_d() {
  auto downstream = d_arb.accept();
  parent->sources_[downstream]->port->d.accept.send();

  next_d();
}

void SinkBundle::next_b() {
  if(b_arb.no_outstanding())
    if(auto next = b_arb.next())
      port->b.data.send(*next);
}

void SinkBundle::next_d() {
  if(d_arb.no_outstanding())
    if(auto next = d_arb.next())
      port->d.data.send(*next);
}

void SourceBundle::data_b(const TLABMsg<> &msg) {
  auto [upstream, upstream_source] = upstream_ids.inv_project(msg.source);

  auto updated_msg = msg;
  updated_msg.source = upstream_source;

  auto &sink = parent->sinks_[upstream];
  auto number = sink->reachable_downstream_inv[idx].value();
  sink->b_arb.propose(number, std::move(updated_msg));
  sink->next_b();
}

void SourceBundle::data_d(const TLDMsg<> &msg) {
  auto [upstream, upstream_source] = upstream_ids.inv_project(msg.source);

  auto remapped_sink_idx = parent->sinks_[upstream]->downstream_ids.project(idx, msg.sink);

  auto updated_msg = msg;
  updated_msg.source = upstream_source;
  updated_msg.sink = remapped_sink_idx;

  auto &sink = parent->sinks_[upstream];
  auto number = sink->reachable_downstream_inv[idx].value();
  sink->d_arb.propose(number, std::move(updated_msg));
  sink->next_d();
}

void SourceBundle::accept_a() {
  auto upstream = a_arb.accept();
  parent->sinks_[upstream]->port->a.accept.send();

  next_a();
}

void SourceBundle::accept_c() {
  auto upstream = c_arb.accept();
  parent->sinks_[upstream]->port->c.accept.send();

  next_c();
}

void SourceBundle::accept_e() {
  auto upstream = e_arb.accept();
  parent->sinks_[upstream]->port->e.accept.send();

  next_e();
}

void SourceBundle::next_a() {
  if(a_arb.no_outstanding())
    if(auto next = a_arb.next())
      port->a.data.send(*next);
}

void SourceBundle::next_c() {
  if(c_arb.no_outstanding())
    if(auto next = c_arb.next())
      port->c.data.send(*next);
}

void SourceBundle::next_e() {
  if(e_arb.no_outstanding())
    if(auto next = e_arb.next())
      port->e.data.send(*next);
}

Crossbar::Crossbar(sparta::TreeNode *node, const Parameters *params) :
  sparta::Unit(node, name),
  self(node),
  params_(params) {
    // TODO: validate parameter dimensions
    // TODO: validate ID widths

    sinks_.reserve(params_->sinks);
    sources_.reserve(params_->sources);

    // Populate router
    for(std::size_t i = 0; i < params_->sources; ++i) {
      auto &range = params_->ranges.getValue()[i];
      // TODO: validator
      routing_.add(i, std::make_pair(
        range[0],
        range[1]
      ));
    }

    // Construct bundles
    for(std::size_t i = 0; i < params_->sinks; ++i) {
      std::vector<std::optional<std::size_t>> source_sizes(params_->sources);
      std::vector<std::size_t> reachable_downstream;
      std::vector<std::optional<std::size_t>> reachable_downstream_inv(params_->sources);
      for(std::size_t j = 0; j < params_->sources; ++j) {
        if(params_->connectivity.getValue()[i][j]) {
          source_sizes[j] = params_->downstream_sizes.getValue()[j];
          reachable_downstream.push_back(j);
          reachable_downstream_inv[j] = reachable_downstream.size() - 1;
        }
      }
      sinks_.emplace_back(std::make_unique<SinkBundle>(
        i, this, reachable_downstream, reachable_downstream_inv, source_sizes
      ));
    }

    for(std::size_t i = 0; i < params_->sources; ++i) {
      std::vector<std::optional<std::size_t>> sink_sizes(params_->sinks);
      std::vector<std::size_t> reachable_upstream;
      std::vector<std::optional<std::size_t>> reachable_upstream_inv(params_->sinks);

      for(std::size_t j = 0; j < params_->sinks; ++j) {
        if(params_->connectivity.getValue()[j][i]) {
          sink_sizes[j] = params_->upstream_sizes.getValue()[j];
          reachable_upstream.push_back(j);
          reachable_upstream_inv[j] = reachable_upstream.size() - 1;
        } else {
          sink_sizes[j] = 0;
        }
      }

      sources_.emplace_back(std::make_unique<SourceBundle>(
        i, this, reachable_upstream, reachable_upstream_inv, sink_sizes
      ));
    }
  }

void Crossbar::bind_sink(std::size_t idx, TLBundleSource<> &src) {
  this->sinks_[idx]->port->bind(src);
}
void Crossbar::bind_src(std::size_t idx, TLBundleSink<> &sink) {
  this->sources_[idx]->port->bind(sink);
}