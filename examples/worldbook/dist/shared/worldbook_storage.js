"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getWorldBookDir = getWorldBookDir;
exports.getWorldBookFile = getWorldBookFile;
exports.ensureWorldBookStorage = ensureWorldBookStorage;
exports.readWorldBookEntries = readWorldBookEntries;
exports.writeWorldBookEntries = writeWorldBookEntries;
exports.getWorldBookGroupsFile = getWorldBookGroupsFile;
exports.readWorldBookGroups = readWorldBookGroups;
exports.writeWorldBookGroups = writeWorldBookGroups;
function getWorldBookDir() {
    return ToolPkg.getConfigDir();
}
function getWorldBookFile() {
    return `${getWorldBookDir()}/entries.json`;
}
async function ensureWorldBookStorage() {
    const worldBookDir = getWorldBookDir();
    const worldBookFile = getWorldBookFile();
    const worldBookGroupsFile = getWorldBookGroupsFile();
    await Tools.Files.mkdir(worldBookDir, true);
    const currentFileExists = await Tools.Files.exists(worldBookFile);
    if (!currentFileExists?.exists) {
        await Tools.Files.write(worldBookFile, "[]", false);
    }
    const groupFileExists = await Tools.Files.exists(worldBookGroupsFile);
    if (!groupFileExists?.exists) {
        await Tools.Files.write(worldBookGroupsFile, "[]", false);
    }
}
async function readWorldBookEntries() {
    await ensureWorldBookStorage();
    try {
        const fileResult = await Tools.Files.read(getWorldBookFile());
        if (!fileResult?.content) {
            return [];
        }
        const parsed = JSON.parse(fileResult.content);
        return Array.isArray(parsed) ? parsed : [];
    }
    catch (_error) {
        return [];
    }
}
async function writeWorldBookEntries(entries) {
    await ensureWorldBookStorage();
    await Tools.Files.write(getWorldBookFile(), JSON.stringify(entries, null, 2));
}
function getWorldBookGroupsFile() {
    return `${getWorldBookDir()}/groups.json`;
}
async function readWorldBookGroups() {
    await ensureWorldBookStorage();
    const fileResult = await Tools.Files.read(getWorldBookGroupsFile());
    if (fileResult?.content == null) {
        throw new Error("世界书分组文件读取失败");
    }
    const parsed = JSON.parse(fileResult.content);
    if (!Array.isArray(parsed)) {
        throw new Error("世界书分组文件格式无效");
    }
    return parsed;
}
async function writeWorldBookGroups(groups) {
    await ensureWorldBookStorage();
    await Tools.Files.write(getWorldBookGroupsFile(), JSON.stringify(groups, null, 2));
}
