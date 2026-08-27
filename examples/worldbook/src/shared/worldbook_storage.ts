export function getWorldBookDir(): string {
  return ToolPkg.getConfigDir();
}

export function getWorldBookFile(): string {
  return `${getWorldBookDir()}/entries.json`;
}

export async function ensureWorldBookStorage(): Promise<void> {
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

export async function readWorldBookEntries<T>(): Promise<T[]> {
  await ensureWorldBookStorage();

  try {
    const fileResult = await Tools.Files.read(getWorldBookFile());
    if (!fileResult?.content) {
      return [];
    }

    const parsed = JSON.parse(fileResult.content);
    return Array.isArray(parsed) ? (parsed as T[]) : [];
  } catch (_error) {
    return [];
  }
}

export async function writeWorldBookEntries(entries: unknown[]): Promise<void> {
  await ensureWorldBookStorage();
  await Tools.Files.write(getWorldBookFile(), JSON.stringify(entries, null, 2));
}

export function getWorldBookGroupsFile(): string {
  return `${getWorldBookDir()}/groups.json`;
}

export async function readWorldBookGroups<T>(): Promise<T[]> {
  await ensureWorldBookStorage();
  const fileResult = await Tools.Files.read(getWorldBookGroupsFile());
  if (fileResult?.content == null) {
    throw new Error("世界书分组文件读取失败");
  }
  const parsed = JSON.parse(fileResult.content);
  if (!Array.isArray(parsed)) {
    throw new Error("世界书分组文件格式无效");
  }
  return parsed as T[];
}

export async function writeWorldBookGroups(groups: unknown[]): Promise<void> {
  await ensureWorldBookStorage();
  await Tools.Files.write(getWorldBookGroupsFile(), JSON.stringify(groups, null, 2));
}
