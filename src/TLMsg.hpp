#ifndef __TLMSG__
#define __TLMSG__

#include <concepts>
#include <cstdint>

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
};

template<typename Ident = uint64_t>
struct TLEMsg {
  Ident sink;

  uint8_t get_size() const {
    return 1; // E channels have no locks
  }
};

#endif // __TLMSG__