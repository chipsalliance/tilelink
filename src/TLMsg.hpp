#ifndef __TLMSG__
#define __TLMSG__

#include <concepts>
#include <cstdint>
#include <ostream>

struct DefaultTypes {
  typedef uint64_t Data;
  typedef uint64_t Addr;
  typedef uint64_t Ident;
  typedef uint8_t Mask;
};

template<typename T>
concept HasSize = requires(const T a) {
  { a.get_size() } -> std::convertible_to<std::size_t>;
};

struct TLOp {
  uint8_t code;
  uint8_t param;
};

template<typename Types = DefaultTypes>
struct TLABMsg {
  typename Types::Ident source;
  TLOp op;

  typename Types::Addr addr;
  uint8_t size;
  typename Types::Data data;
  typename Types::Mask mask; // Ignored on C

  bool corrupt;

  uint8_t get_size() const {
    return size;
  }

  friend std::ostream &operator<<(std::ostream &os, const TLABMsg &msg) {
    os << "TLABMsg{source=" << msg.source << ", op=" << msg.op.code
       << ", addr=" << msg.addr << ", size=" << msg.size
       << ", data=" << msg.data << ", mask=" << msg.mask
       << ", corrupt=" << msg.corrupt << "}";
    return os;
  }
};

template<typename Types = DefaultTypes>
struct TLCMsg {
  typename Types::Ident source;
  TLOp op;

  typename Types::Addr addr;
  uint8_t size;
  typename Types::Data data;

  bool corrupt;

  uint8_t get_size() const {
    return size;
  }

  friend std::ostream &operator<<(std::ostream &os, const TLCMsg &msg) {
    os << "TLCMsg{source=" << msg.source << ", op=" << msg.op.code
       << ", addr=" << msg.addr << ", size=" << msg.size
       << ", data=" << msg.data << ", corrupt=" << msg.corrupt << "}";
    return os;
  }
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

  uint8_t get_size() const {
    return size;
  }

  friend std::ostream &operator<<(std::ostream &os, const TLDMsg &msg) {
    os << "TLDMsg{source=" << msg.source << ", sink=" << msg.sink
       << ", op=" << msg.op.code << ", size=" << msg.size
       << ", data=" << msg.data << ", denied=" << msg.denied
       << ", corrupt=" << msg.corrupt << "}";
    return os;
  }
};

template<typename Types = DefaultTypes>
struct TLEMsg {
  typename Types::Ident sink;

  uint8_t get_size() const {
    return 1; // E channels have no locks
  }

  friend std::ostream &operator<<(std::ostream &os, const TLEMsg &msg) {
    os << "TLEMsg{sink=" << msg.sink << "}";
    return os;
  }
};

#endif // __TLMSG__