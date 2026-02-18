import path from "path";

import fs from "fs";
import { ClientContext, PushNotification } from "@/types";
import Logger from "./logger";

export function loadIcon(name: string): string | null {
  try {
    const iconsDir = path.join(__dirname, "../icons");
    
    if (!fs.existsSync(iconsDir)) {
      return null;
    }

    const files = fs.readdirSync(iconsDir);
    
    // Find a file where the name (without extension) matches the requested name
    const matchingFile = files.find(file => path.parse(file).name === name);

    if (matchingFile) {
      const iconPath = path.join(iconsDir, matchingFile);
      const file = fs.readFileSync(iconPath);
      return Buffer.from(file).toString("base64");
    }

    return null;
  } catch (error) {
    Logger.error(`Error loading icon ${name}: ${error}`);
    return null;
  }
}

export function sendNotifications(
  clients: ClientContext[],
  notification: PushNotification,
): void {
  clients.forEach((client) => {
    // If notification has a token, only send to that specific client
    // If notification.token is null (broadcast), send to all clients
    if (notification.token && client.token !== notification.token) {
      return;
    }

    Logger.info(`Sending notification to ${client.req.socket?.remoteAddress}`);
    const data = `data: ${JSON.stringify(notification)}\n\n`;
    client.res.write(data);
  });
}
