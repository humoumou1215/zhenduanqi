import commandsData from '../data/arthas-commands.json';

export function getCommands() {
  return commandsData;
}

export function getCommandByName(name) {
  return commandsData.find((cmd) => cmd.name === name);
}

export function getCommandsByCategory(category) {
  return commandsData.filter((cmd) => cmd.category === category);
}

export function searchCommands(keyword) {
  if (!keyword) {
    return commandsData;
  }
  const lowerKeyword = keyword.toLowerCase();
  return commandsData.filter(
    (cmd) =>
      cmd.name.toLowerCase().includes(lowerKeyword) ||
      cmd.description.toLowerCase().includes(lowerKeyword)
  );
}
