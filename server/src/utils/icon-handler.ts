import path from "path";
import fs from "fs";

import Logger from "./logger";


export function loadIcon(name: string): string | null {
  try {
    const iconPath = path.join(__dirname, '..', 'icons', name);

    const file = fs.readFileSync(iconPath);

    const base64string = Buffer.from(file).toString('base64');

    return base64string;

  } catch (error) {
    Logger.error(`Error loading icon ${name}: ${error}`);
    return null;
  }
}
