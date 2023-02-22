// <main.cpp> -*- C++ -*-


#include <iostream>

// TODO: upstream header fixes
#include <limits>
#include <string_view>

#include "Simulator.hpp" // Core model skeleton simulator

#include "sparta/parsers/ConfigEmitterYAML.hpp"
#include "sparta/app/CommandLineSimulator.hpp"
#include "sparta/app/MultiDetailOptions.hpp"
#include "sparta/sparta.hpp"

// User-friendly usage that correspond with
// sparta::app::CommandLineSimulator options
const char USAGE[] =
    "Usage:\n"
    "    [-d <file>]\n"
    "\n";

int main(int argc, char **argv)
{
    std::string db;

    sparta::app::DefaultValues DEFAULTS;
    DEFAULTS.auto_summary_default = "on";

    // try/catch block to ensure proper destruction of the cls/sim classes in
    // the event of an error
    try{
        // Helper class for parsing command line arguments, setting up
        // the simulator, and running the simulator. All of the things
        // done by this classs can be done manually if desired. Use
        // the source for the CommandLineSimulator class as a starting
        // point
        sparta::app::CommandLineSimulator cls(USAGE, DEFAULTS);
        auto & app_opts = cls.getApplicationOptions();
        app_opts.add_options()
            ("db,d",
             sparta::app::named_value<std::string>("DB", &db),
             "Specifies the output db") // example, not used
            ;

        // Parse command line options and configure simulator
        int err_code = 0;
        if(!cls.parse(argc, argv, err_code)){
            return err_code; // Any errors already printed to cerr
        }

        // Create the simulator object for population -- does not
        // instantiate nor run it.
        sparta::Scheduler scheduler;
        Simulator sim(scheduler);

        cls.populateSimulation(&sim);
        cls.runSimulator(&sim);
        cls.postProcess(&sim);

    }catch(...){
        // Could still handle or log the exception here
        throw;
    }

    return 0;
}
