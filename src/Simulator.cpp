#include "Simulator.hpp"
#include "Master.hpp"
#include "Slave.hpp"
#include "Crossbar.hpp"

using MasterFactory = sparta::ResourceFactory<Master, Master::Parameters>;
using SlaveFactory = sparta::ResourceFactory<Slave, Slave::Parameters>;
using CrossbarFactory = sparta::ResourceFactory<Crossbar, Crossbar::Parameters>;

Simulator::Simulator(sparta::Scheduler &scheduler)
  : sparta::app::Simulation("tlmodel", &scheduler) {
  
  getResourceSet()->addResourceFactory<MasterFactory>();
  getResourceSet()->addResourceFactory<SlaveFactory>();
  getResourceSet()->addResourceFactory<CrossbarFactory>();
}

Simulator::~Simulator() {
  getRoot()->enterTeardown();
}

void Simulator::buildTree_() {
  // Create master
  sparta::ResourceTreeNode *master = new sparta::ResourceTreeNode(
      getRoot(), "master", sparta::TreeNode::GROUP_NAME_NONE,
      sparta::TreeNode::GROUP_IDX_NONE, "Master node",
      getResourceSet()->getResourceFactory(Master::name));
  to_delete_.emplace_back(master);

  sparta::ResourceTreeNode *slave = new sparta::ResourceTreeNode(
      getRoot(), "slave", sparta::TreeNode::GROUP_NAME_NONE,
      sparta::TreeNode::GROUP_IDX_NONE, "Slave node",
      getResourceSet()->getResourceFactory(Slave::name));
  to_delete_.emplace_back(slave);
}

void Simulator::configureTree_() {
  // Nothing to configure
}

void Simulator::bindTree_() {
  sparta::TreeNode* root = getRoot();
  auto master = root->getChild("master")->getResourceAs<Master>();
  auto slave = root->getChild("slave")->getResourceAs<Slave>();
  master->port->bind(*slave->port);
}