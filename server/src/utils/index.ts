import path from "path";


import fs from 'fs';
import { ClientContext, PushNotification } from "@/types";
import Logger from "./logger";


export function loadIcon(name: string): string | null {
    try {

        const iconPath = path.join(__dirname, 'src', 'icons', name);
        const file = fs.readFileSync(iconPath);
        const base64string = Buffer.from(file).toString('base64');

        return base64string;

    } catch (error) {
        Logger.error(`Error loading icon ${name}: ${error}`);
        return null;
    }
}

export function sendNotifications(clients: ClientContext[], notification: PushNotification): void {
    clients.forEach(({ req, res }) => {
        Logger.info(`Sending notification to ${req.socket?.remoteAddress}`);
        const data = `data: ${JSON.stringify(notification)}\n\n`;
        res.write(data);
    });
}
