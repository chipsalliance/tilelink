#ifndef __SIMULATOR_H__
#define __SIMULATOR_H__

#include "sparta/app/Simulation.hpp"

class Simulator : public sparta::app::Simulation {
public:
  Simulator(sparta::Scheduler & scheduler);
  virtual ~Simulator();

private:
  void buildTree_() override;
  void configureTree_() override;
  void bindTree_() override;
};

#endif // __SIMULATOR_H__