#include "GlobalLogger.hpp"
#include <optional>

std::optional<GlobalLogger> GlobalLogger::logger = {};

GlobalLogger::GlobalLogger(std::string base) : base(base) {}

void GlobalLogger::put(const std::string &ident, std::string_view msg) {
  if(!logger) return;
  logger->put_impl(ident, msg);
}

void GlobalLogger::init(std::string base) {
  logger = GlobalLogger(base);
}

void GlobalLogger::put_impl(const std::string &ident, std::string_view msg) {
  if(!outputs.contains(ident)) {
    auto path = base / ident;
    outputs.emplace(ident, std::ofstream(path));
  }

  auto &out = outputs[ident];
  out << msg << std::endl;
}