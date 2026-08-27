const SandboxPackageDevInstallerState = {
  logs: []
};

const SandboxPackageDevInstaller = (function () {
  const ENVIRONMENT = "android";
  const SKILL_NAME = "SandboxPackage_DEV";
  const OperitPaths = Java.type("com.ai.assistance.operit.util.OperitPaths");
  const SKILLS_ROOT = String(OperitPaths.INSTANCE.skillsPathSdcard());
  const SKILL_ROOT = `${SKILLS_ROOT}/${SKILL_NAME}`;
  const REFERENCES_DIR = `${SKILL_ROOT}/references`;
  const TYPES_DIR = `${SKILL_ROOT}/types`;
  const SCRIPTS_DIR = `${SKILL_ROOT}/scripts`;
  const EXAMPLES_DIR = `${SKILL_ROOT}/examples`;
  const EXAMPLE_PACKAGES_DIR = `${EXAMPLES_DIR}/packages`;
  const BUILTIN_PACKAGES_ASSET_DIR = "packages";
  const BUNDLED_DEV_ASSET_DIR = "tools/sandboxpackage_dev";

  function logStep(message) {
    SandboxPackageDevInstallerState.logs.push(message);
    console.log(message);
  }

  async function makeDirectory(path) {
    return await Tools.Files.mkdir(path, true, ENVIRONMENT);
  }

  function collectRelativeFiles(directory, relativePrefix, collectedFiles) {
    const children = directory.listFiles();
    if (!children) {
      return;
    }

    for (let index = 0; index < children.length; index += 1) {
      const child = children[index];
      const relativePath = relativePrefix
        ? `${relativePrefix}/${String(child.getName())}`
        : String(child.getName());

      if (child.isDirectory()) {
        collectRelativeFiles(child, relativePath, collectedFiles);
        continue;
      }

      collectedFiles.push(relativePath);
    }
  }

  function syncBuiltInPackageExamples() {
    const File = Java.type("java.io.File");
    const AssetCopyUtils = Java.type("com.ai.assistance.operit.util.AssetCopyUtils");
    const context = Java.getApplicationContext();
    const outputDir = new File(EXAMPLE_PACKAGES_DIR);
    const copiedFiles = [];

    AssetCopyUtils.INSTANCE.copyAssetDirRecursive(
      context,
      BUILTIN_PACKAGES_ASSET_DIR,
      outputDir,
      true
    );

    collectRelativeFiles(outputDir, "", copiedFiles);
    copiedFiles.sort();
    return copiedFiles;
  }

  function syncBundledDevelopmentFiles() {
    const File = Java.type("java.io.File");
    const AssetCopyUtils = Java.type("com.ai.assistance.operit.util.AssetCopyUtils");
    const context = Java.getApplicationContext();
    const outputDir = new File(SKILL_ROOT);
    const copiedFiles = [];

    AssetCopyUtils.INSTANCE.copyAssetDirRecursive(
      context,
      BUNDLED_DEV_ASSET_DIR,
      outputDir,
      true
    );

    collectRelativeFiles(outputDir, "", copiedFiles);
    copiedFiles.sort();
    if (!copiedFiles.includes("SKILL.md")) {
      throw new Error("Bundled SandboxPackage_DEV is missing SKILL.md");
    }
    if (!copiedFiles.some((path) => path.startsWith("types/") && path.endsWith(".d.ts"))) {
      throw new Error("Bundled SandboxPackage_DEV is missing TypeScript declarations");
    }
    return copiedFiles;
  }

  async function run() {
    logStep(`Preparing skill root -> ${SKILL_ROOT}`);
    await makeDirectory(SKILLS_ROOT);
    await makeDirectory(SKILL_ROOT);
    await makeDirectory(REFERENCES_DIR);
    await makeDirectory(TYPES_DIR);
    await makeDirectory(SCRIPTS_DIR);
    await makeDirectory(EXAMPLES_DIR);

    logStep(`Syncing bundled development references -> ${SKILL_ROOT}`);
    const bundledDevelopmentFiles = syncBundledDevelopmentFiles();
    logStep(`Bundled development files synced -> ${bundledDevelopmentFiles.length} files`);

    logStep(`Syncing built-in package examples -> ${EXAMPLE_PACKAGES_DIR}`);
    const copiedExampleFiles = syncBuiltInPackageExamples();
    logStep(`Built-in package examples synced -> ${copiedExampleFiles.length} files`);

    return {
      success: true,
      message: `${SKILL_NAME} installed or updated successfully.`,
      data: {
        skill_name: SKILL_NAME,
        skill_root: SKILL_ROOT,
        references_dir: REFERENCES_DIR,
        types_dir: TYPES_DIR,
        scripts_dir: SCRIPTS_DIR,
        examples_dir: EXAMPLES_DIR,
        examples_packages_dir: EXAMPLE_PACKAGES_DIR,
        bundled_development_file_count: bundledDevelopmentFiles.length,
        builtin_example_count: copiedExampleFiles.length,
        builtin_example_files: copiedExampleFiles,
        logs: SandboxPackageDevInstallerState.logs
      }
    };
  }

  return {
    run
  };
})();

SandboxPackageDevInstaller.run()
  .then((result) => {
    complete(result);
  })
  .catch((error) => {
    complete({
      success: false,
      message: String(error && error.message ? error.message : error),
      data: {
        skill_name: "SandboxPackage_DEV",
        logs: SandboxPackageDevInstallerState.logs
      }
    });
  });
