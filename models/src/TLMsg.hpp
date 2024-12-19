#ifndef __TLMSG__
#define __TLMSG__

#include <concepts>
#include <cstdint>
#include <ostream>
#include <string_view>
#include <optional>
#include <sparta/utils/SpartaAssert.hpp>
#include <nlohmann/json.hpp>

using json = nlohmann::json;

struct DefaultTypes {
  typedef uint64_t Data;
  typedef uint64_t Addr;
  typedef uint64_t Ident;
  typedef uint8_t Mask;
};

template<typename T>
concept HasSize = requires(const T a) {
  { a.get_beats() } -> std::convertible_to<std::size_t>;
};

enum class TLOpCode {
  // TL-UL
  Get,
  AccessAckData,
  PutFullData,
  PutPartialData,
  AccessAck,
};

static const char *tl_opcode_to_str(TLOpCode code) {
  switch (code) {
  case TLOpCode::Get:
    return "Get";
  case TLOpCode::AccessAckData:
    return "AccessAckData";
  case TLOpCode::PutFullData:
    return "PutFullData";
  case TLOpCode::PutPartialData:
    return "PutPartialData";
  case TLOpCode::AccessAck:
    return "AccessAck";
  }
  sparta_assert(false, "Unknown TLOpCode: " << (int) code);
}

static const std::optional<TLOpCode> tl_opcode_from_str(const std::string_view &str) {
  if(str == "Get") return TLOpCode::Get;
  if(str == "AccessAckData") return TLOpCode::AccessAckData;
  if(str == "PutFullData") return TLOpCode::PutFullData;
  if(str == "PutPartialData") return TLOpCode::PutPartialData;
  if(str == "AccessAck") return TLOpCode::AccessAck;
  return std::nullopt;
}

static const uint8_t tl_opcode_to_int(TLOpCode code) {
  switch (code) {
  case TLOpCode::Get:
    return 4;
  case TLOpCode::AccessAckData:
    return 1;
  case TLOpCode::PutFullData:
    return 0;
  case TLOpCode::PutPartialData:
    return 1;
  case TLOpCode::AccessAck:
    return 0;
  }
}

static void to_json(json& j, const TLOpCode& opcode) {
  j = tl_opcode_to_str(opcode);
}

static void from_json(json& j, TLOpCode& opcode) {
  std::string_view str = j.get<std::string_view>();
  opcode = tl_opcode_from_str(str).value();
}

struct TLOp {
  TLOpCode code;
  uint8_t param;

  friend std::ostream &operator<<(std::ostream &os, const TLOp &op) {
    os << "TLOp{code=" << tl_opcode_to_str(op.code) << ", param=" << (uint64_t) op.param << "}";
    return os;
  }

  NLOHMANN_DEFINE_TYPE_INTRUSIVE(TLOp, code, param)
};

// TODO: Split A and B
template<typename Types = DefaultTypes>
struct TLABMsg {
  typename Types::Ident source;
  TLOp op;

  typename Types::Addr addr;
  uint8_t size;
  typename Types::Data data;
  typename Types::Mask mask; // Ignored on C

  bool corrupt;

  size_t get_beats() const {
    if(op.code == TLOpCode::Get) return 1;

    size_t full_size = 1 << size;
    if(full_size < sizeof(typename Types::Data)) return 1;
    return full_size / sizeof(typename Types::Data);
  }

  size_t get_response_beats() const {
    size_t full_size = 1 << size;
    if(full_size < sizeof(typename Types::Data)) return 1;
    return full_size / sizeof(typename Types::Data);
  }

  friend std::ostream &operator<<(std::ostream &os, const TLABMsg &msg) {
    os << "TLABMsg{source=" << msg.source << ", op=" << msg.op
       << ", addr=" << msg.addr << ", size=" << (uint64_t) msg.size
       << ", data=" << msg.data << ", mask=" << (uint64_t) msg.mask
       << ", corrupt=" << msg.corrupt << "}";
    return os;
  }

  NLOHMANN_DEFINE_TYPE_INTRUSIVE(TLABMsg, source, op, addr, size, data, mask, corrupt)
};

template<typename Types = DefaultTypes>
struct TLCMsg {
  typename Types::Ident source;
  TLOp op;

  typename Types::Addr addr;
  uint8_t size;
  typename Types::Data data;

  bool corrupt;

  size_t get_beats() const {
    size_t full_size = 1 << size;
    if(full_size < sizeof(typename Types::Data)) return 1;
    return full_size / sizeof(typename Types::Data);
  }

  friend std::ostream &operator<<(std::ostream &os, const TLCMsg &msg) {
    os << "TLCMsg{source=" << msg.source << ", op=" << msg.op
       << ", addr=" << msg.addr << ", size=" << (uint64_t) msg.size
       << ", data=" << msg.data << ", corrupt=" << msg.corrupt << "}";
    return os;
  }

  NLOHMANN_DEFINE_TYPE_INTRUSIVE(TLCMsg, source, op, addr, size, data, corrupt)
};

template<typename Types = DefaultTypes>
struct TLDMsg {
  typename Types::Ident source;
  typename Types::Ident sink;
  TLOp op;

  uint8_t size;
  typename Types::Data data;

  bool denied;
  bool corrupt;

  size_t get_beats() const {
    if(op.code == TLOpCode::AccessAck) return 1;

    size_t full_size = 1 << size;
    if(full_size < sizeof(typename Types::Data)) return 1;
    return full_size / sizeof(typename Types::Data);
  }

  friend std::ostream &operator<<(std::ostream &os, const TLDMsg &msg) {
    os << "TLDMsg{source=" << msg.source << ", sink=" << msg.sink
       << ", op=" << msg.op << ", size=" << (uint64_t) msg.size
       << ", data=" << msg.data << ", denied=" << msg.denied
       << ", corrupt=" << msg.corrupt << "}";
    return os;
  }

  NLOHMANN_DEFINE_TYPE_INTRUSIVE(TLDMsg, source, sink, op, size, data, denied, corrupt)
};

template<typename Types = DefaultTypes>
struct TLEMsg {
  typename Types::Ident sink;

  size_t get_beats() const {
    return 1; // E channels have no locks
  }

  friend std::ostream &operator<<(std::ostream &os, const TLEMsg &msg) {
    os << "TLEMsg{sink=" << msg.sink << "}";
    return os;
  }

  NLOHMANN_DEFINE_TYPE_INTRUSIVE(TLEMsg, sink)
};

#endif // __TLMSG__