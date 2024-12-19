#ifndef __GLOBAL_LOGGER__
#define __GLOBAL_LOGGER__

#include <optional>
#include <string>
#include <unordered_map>
#include <filesystem>
#include <fstream>

#include <nlohmann/json.hpp>

using json = nlohmann::json;

class GlobalLogger {
public:
  static void init(std::string base);

  static void put(const std::string &ident, std::string_view msg);

  template<typename T>
  static void put_json(const std::string &ident, T msg) {
    json j = msg;
    put(ident, j.dump());
  }

private:
  static std::optional<GlobalLogger> logger;

  std::filesystem::path base;
  std::unordered_map<std::string, std::ofstream> outputs;

  GlobalLogger(std::string base);
  void put_impl(const std::string &ident, std::string_view msg);
};

template <typename T>
struct TimedEvent {
  uint64_t at;
  T event;

  NLOHMANN_DEFINE_TYPE_INTRUSIVE(TimedEvent<T>, at, event)
};

#endif // __GLOBAL_LOGGER__