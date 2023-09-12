{
  inputs = { nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable"; };

  outputs = { self, nixpkgs }:
    let pkgs = nixpkgs.legacyPackages.x86_64-linux;
        sparta = pkgs.clangStdenv.mkDerivation rec {
          pname = "sparta";
          version = "v1.1.0";
          src = builtins.fetchGit {
            url = "https://github.com/sparcians/map.git";
            ref = "refs/tags/map_v1.1.0";
            rev = "f64923557b79629a1162a83d55f10fe94b0aad59";
            # sha256 = "sha256-OiN6+Bfaz0B6uPXi9pRGOUWKfUWDMhuT8lrjjhK1BH8=";
          };
          patches = [ ./patches/sparta.diff ];
          sourceRoot = "source/sparta";
          GIT_TAG = version;

          buildInputs = [
            pkgs.cmake
            pkgs.git
            pkgs.ninja
            pkgs.boost
            pkgs.yaml-cpp
            pkgs.sqlite
            pkgs.hdf5
            pkgs.rapidjson
          ];

          cmakeFlags = [
            "-DCMAKE_BUILD_TYPE=Release"
          ];
        };
    in {
      devShell.x86_64-linux = pkgs.mkShell { buildInputs = [
        pkgs.verilator
        pkgs.cmake
        pkgs.clang
        pkgs.mill
        pkgs.circt
        sparta
        pkgs.boost
        pkgs.yaml-cpp
        pkgs.sqlite
        pkgs.hdf5
        pkgs.rapidjson
      ]; };
   };
}
