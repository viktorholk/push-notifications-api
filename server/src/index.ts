import dotenv from 'dotenv';
dotenv.config();

import express, { Request, Response } from 'express';
import cors from "cors";
import ip from "ip";
import { v4 as uuidv4 } from 'uuid';

import Logger from "@/utils/logger";
import LoggerMiddleware from "@/middleware/logger";
import { ClientContext, PushNotification } from '@/types';
import { loadIcon, sendNotifications } from '@/utils';


let notifications: PushNotification[] = [];
let clients: ClientContext[] = [];

function connectClient(req: Request, res: Response) {
    // Set headers to keep the connection open
    res.setHeader("Content-Type", "text/event-stream");
    res.setHeader("Cache-Control", "no-cache");
    res.setHeader("Connection", "keep-alive");

    Logger.info(`${req.socket?.remoteAddress} connected`);
    clients.push({ req, res });

    res.write("data: Connected\n\n");

    req.on("close", () => {
        Logger.info(`${req.socket?.remoteAddress} disconnected`);
        clients = clients.filter(client => client.req !== req);
    });

}

function createNotification(req: Request, res: Response) {
    const { title, message, url, icon, color } = req.body;

    if (!title || title.trim() === "") {
        return res.status(400).send("'title' field is required");
    }

    const notification: PushNotification = {
        id: uuidv4(),
        title,
        message,
        url,
        icon,
        color,
        createdAt: new Date().toLocaleString()
    };

    if (icon) {
        const base64Icon = loadIcon(icon);
        if (base64Icon) {
            notification.icon = base64Icon;
        }
    }

    notifications.push(notification);

    // Send the notification to all the connected clients
    sendNotifications(clients, notification);

    res.sendResponse(201);

}

function getAllNotifications(_req: Request, res: Response) {
    res.sendResponse(200, notifications);
}

function getLatestNotification(_req: Request, res: Response) {
    const latestNotification = notifications.length ? notifications[notifications.length - 1] : null;

    if (!latestNotification) {
        return res.sendResponse(404);
    }

    res.sendResponse(200, latestNotification);

}

async function main() {
    const app = express();

    const host = process.env.HOST || "0.0.0.0";
    const port = parseInt(process.env.PORT || "3000", 10);

    app.use(cors());
    app.use(express.json());
    app.use(LoggerMiddleware);

    app.post("/", createNotification);
    app.get("/", getAllNotifications);
    app.get("/latest", getLatestNotification);
    app.get("/events", connectClient);

    app.listen(port, host, () => {
        Logger.info(`Server is running on http://${host === "0.0.0.0" ? ip.address() : host}:${port}`);
    });
}

main();
