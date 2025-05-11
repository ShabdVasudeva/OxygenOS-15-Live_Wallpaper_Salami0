# To learn more about how to use Nix to configure your environment
# see: https://firebase.google.com/docs/studio/customize-workspace
{ pkgs, ... }: {
  # Which nixpkgs channel to use.
  channel = "stable-24.05"; # or "unstable"

  # List the packages you would like to make available to your workspace.
  packages = [
    # Android SDK, platform-tools, and build-tools
    pkgs.androidsdk
    pkgs.androidsdk.platform-tools
    pkgs.androidsdk.build-tools."34.0.0" # Replace with your desired build tools version

    # Java Development Kit (JDK)
    pkgs.openjdk11 # Or your desired JDK version

    # Gradle build tool
    pkgs.gradle
  ];

  # Define the environment variables that will be available in your workspace.
  # This is where you should configure the Android SDK path.
  env = {
    ANDROID_HOME = "${pkgs.androidsdk}/libexec/android-sdk";
    # Ensure the platform-tools are in the PATH
    PATH = "$PATH:${pkgs.androidsdk.platform-tools}/bin";
  };

  # Sets environment variables in the workspace
  env = {};
  idx = {
    # Search for the extensions you want on https://open-vsx.org/ and use "publisher.id"
    extensions = [
      # "vscodevim.vim"
    ];

    # Enable previews
    previews = {
      enable = true;
      previews = {
        # web = {
        #   # Example: run "npm run dev" with PORT set to IDX's defined port for previews,
        #   # and show it in IDX's web preview panel
        #   command = ["npm" "run" "dev"];
        #   manager = "web";
        #   env = {
        #     # Environment variables to set for your server
        #     PORT = "$PORT";
        #   };
        # };
      };
    };

    # Workspace lifecycle hooks
    workspace = {
      # Runs when a workspace is first created
      onCreate = {
        # Example: install JS dependencies from NPM
        # npm-install = "npm install";
      };
      # Runs when the workspace is (re)started
      onStart = {
        # Example: start a background task to watch and re-build backend code
        # watch-backend = "npm run watch-backend";
      };
    };
  };
}
